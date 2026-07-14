# Nextcloud Inbound Connector

A Camunda 8 inbound (webhook) connector that receives Nextcloud file events (created, updated,
deleted, renamed, copied, touched) and starts/correlates a BPMN process for each event.

See the [root README](../README.md) for compatibility requirements, how Camunda secrets are
resolved, and an overview of the other connector in this repo.

This connector is built against Nextcloud's **"Webhook Listeners" app** (`webhook_listeners`,
bundled with Nextcloud server since v30, enabled by default in recent versions). It does **not**
work with the classic Flow "Request a URL" action (Administration → Flow) — that action sends a
different, non-configurable payload with no custom-header support. It also doesn't rely on the
third-party `webhooks` app (kffl/nextcloud-webhooks) some older setups use.

> [!IMPORTANT]
> `webhook_listeners` has no admin UI for registering webhooks — every webhook is created via its
> OCS API (or `occ` for read-only listing). This is normal, not a workaround: the Flow UI you see
> under Administration → Flow only lets you pick *trigger conditions* for a webhook that already
> exists — URL, HTTP method, headers, and auth are configured once, up front, via the steps below.

**Nextcloud documentation:**
* [Webhook Listeners admin manual](https://docs.nextcloud.com/server/latest/admin_manual/webhook_listeners/index.html) — app overview, event classes, `eventFilter` syntax.
* [Webhook Listeners OCS API reference](https://docs.nextcloud.com/server/latest/developer_manual/_static/openapi.html#/operations/webhook_listeners-webhooks-index) — full request/response schema for the `webhooks` endpoint used below.

---

## Requirements

* Nextcloud with the `webhook_listeners` app enabled:
  ```bash
  php occ app:list | grep webhook_listeners
  php occ app:enable webhook_listeners   # if not already enabled
  ```
  `occ` is a PHP script in the Nextcloud webroot, not a standalone binary — run it as `php occ ...`
  (or `sudo -u www-data php occ ...` on a typical package install where the webserver user owns the
  install). Against this repo's local dev docker-compose setup, run it inside the `app` container
  instead: `docker compose exec --user www-data app php occ app:enable webhook_listeners`.
* A Nextcloud account with admin (or delegated admin) rights, to call the OCS API.
* The connector's inbound URL for your deployment — see the outbound connector's
  [`README.md`](../nextcloud-connector-outbound/README.md#deployment-options) "Deployment Options"
  for how the standalone service vs. a shared Connector Runtime expose that URL (the same two
  deployment modes apply here). The `Webhook ID` you choose below becomes part of it, e.g.
  `https://runtime-host/inbound/<webhook-id>`.

---

## 1. Register the webhook in Nextcloud

There's no `occ` command to create a webhook — use the OCS API. Requests need
`OCS-APIRequest: true` and admin Basic Auth (an [app password](https://docs.nextcloud.com/server/stable/user_manual/en/session_management.html#managing-devices)
is recommended over the account password).

```bash
curl -u admin:app-password -X POST \
  -H "OCS-APIRequest: true" -H "Content-Type: application/json" \
  https://nextcloud.example.com/ocs/v2.php/apps/webhook_listeners/api/v1/webhooks \
  -d '{
    "httpMethod": "POST",
    "uri": "https://runtime-host/inbound/my-webhook-id",
    "event": "OCP\\Files\\Events\\Node\\NodeCreatedEvent",
    "headers": { "X-Webhook-Secret": "a-long-random-value" }
  }'
```

Or use the wrapper script under [`scripts/register-webhook.sh`](../scripts/register-webhook.sh)
(env vars `NEXTCLOUD_URL`/`NEXTCLOUD_ADMIN_USER`/`NEXTCLOUD_ADMIN_PASSWORD` default to this repo's
local dev docker-compose credentials, see [`DEVELOPMENT.md`](../DEVELOPMENT.md)):

```bash
./scripts/register-webhook.sh create \
  https://runtime-host/inbound/my-webhook-id \
  'OCP\Files\Events\Node\NodeCreatedEvent' \
  a-long-random-value
```

| Field | Required | Description |
| :--- | :--- | :--- |
| `httpMethod` | Yes | Must be `POST` — the connector rejects any other method. |
| `uri` | Yes | The connector's inbound URL, ending in the `Webhook ID` you'll set on the BPMN element (step 3). |
| `event` | Yes | Fully-qualified PHP event class to listen for — see the table below. |
| `eventFilter` | No | Optional [Mongo-style filter](https://docs.nextcloud.com/server/stable/admin_manual/webhook_listeners/index.html) evaluated against the payload envelope, e.g. `{"event.node.path": "/regex/"}` to only fire for a path pattern. This is the "filter criteria" also settable from the Flow UI once the webhook exists. |
| `headers` | No, but effectively required | Custom headers sent with every call. Put your shared secret here, e.g. `{"X-Webhook-Secret": "..."}` — the connector checks this against its `Expected header value` field (step 3). |
| `userIdFilter` | No | Restrict firing to events caused by a specific user. |

Nextcloud also exposes `authMethod`/`authData` for a built-in auth mechanism, but its exact
`authData` shape isn't documented publicly at the time of writing — sending your secret as a plain
custom header via `headers` (as above) is simpler and is what this connector checks against.

### Supported event classes

| `event` value | Fires on | Connector tag | Payload shape |
| :--- | :--- | :--- | :--- |
| `OCP\Files\Events\Node\NodeCreatedEvent` | File/folder created | `created` | single `node` |
| `OCP\Files\Events\Node\NodeWrittenEvent` | File content changed | `updated` | single `node` |
| `OCP\Files\Events\Node\NodeTouchedEvent` | File touched (metadata-only change) | `touched` | single `node` |
| `OCP\Files\Events\Node\NodeDeletedEvent` | File/folder deleted | `deleted` | single `node` |
| `OCP\Files\Events\Node\NodeRenamedEvent` | File/folder renamed/moved | `renamed` | `source` + `target` |
| `OCP\Files\Events\Node\NodeCopiedEvent` | File/folder copied | `copied` | `source` + `target` |

Any other `event.class` (e.g. a `Before*` variant, or `NodeRestoredEvent`) is still accepted and
delivered to the BPMN process, just logged with an `unknown (...)` tag since the connector doesn't
model its fields yet.

> [!NOTE]
> `NodeCreatedEvent`'s payload shape (`event.node.id` / `event.node.path`) is confirmed against
> Nextcloud's own documentation. The single-node vs. `source`/`target` split for the other event
> classes is corroborated by Nextcloud server's own test suite
> (`tests/lib/Files/Node/HookConnectorTest.php`, which asserts `getNode()` for single-node events and
> `getSource()`/`getTarget()` for copy/rename) — but no raw webhook JSON example beyond
> `NodeCreatedEvent` has been directly observed, so still verify against a real payload for your
> Nextcloud version before relying on additional fields in a FEEL expression.

---

## 2. Verify the registration

```bash
php occ webhook_listeners:list
# or, to see full details of one webhook:
curl -u admin:app-password -H "OCS-APIRequest: true" \
  https://nextcloud.example.com/ocs/v2.php/apps/webhook_listeners/api/v1/webhooks/<id>
```

---

## 3. Configure the BPMN element (Camunda Modeler)

Add a "Nextcloud Webhook Connector" start/message-start/boundary/intermediate-catch event and set:

| Field | Value |
| :--- | :--- |
| **Webhook ID** | The last path segment of the `uri` you registered above, e.g. `my-webhook-id`. |
| **Auth header name** | The header key you used in `headers`, e.g. `X-Webhook-Secret` (default if left blank). |
| **Expected header value** | The header value you used in `headers`. Supports FEEL, so reference a secret instead of hardcoding it, e.g. `secrets.NEXTCLOUD_WEBHOOK_SECRET` — see the root README's [Authentication & Secrets](../README.md#authentication--secrets) section for how `secrets.<NAME>` resolves at runtime. Leave empty to disable the header check (not recommended). |

Leaving **Expected header value** empty accepts any request to that URL, so only skip it for local/
trusted-network testing.

---

## 4. Update or remove a webhook later

```bash
# update (send the full new definition, same shape as create)
curl -u admin:app-password -X POST \
  -H "OCS-APIRequest: true" -H "Content-Type: application/json" \
  https://nextcloud.example.com/ocs/v2.php/apps/webhook_listeners/api/v1/webhooks/<id> \
  -d '{ "httpMethod": "POST", "uri": "...", "event": "...", "headers": {...} }'

# delete
curl -u admin:app-password -X DELETE -H "OCS-APIRequest: true" \
  https://nextcloud.example.com/ocs/v2.php/apps/webhook_listeners/api/v1/webhooks/<id>
```

If you rotate the secret, update both sides together: the `headers` value in Nextcloud and the
**Expected header value** on the BPMN element (or the secret it references).

---

## Payload received by the connector

```json
{
  "event": {
    "class": "OCP\\Files\\Events\\Node\\NodeCreatedEvent",
    "node": { "id": 437, "path": "/admin/files/test-webhook.txt" }
  },
  "user": { "uid": "admin", "displayName": "Admin" },
  "time": 1700100000
}
```

The default result expression exposes `event`, `path`, `fileId`, and `user` — for `Renamed`/
`Copied` events use `request.body.event.source`/`.target` instead of `request.body.event.node` in
your own correlation/result expression, since those two event types don't have a `node` field.

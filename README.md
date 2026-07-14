# Nextcloud Connector for Camunda 8

Custom Camunda 8 Connectors for **Nextcloud**, built on the Nextcloud WebDAV and OCS Share APIs.

---

## Connectors

| Connector | Type | Description |
| :--- | :--- | :--- |
| [Outbound](nextcloud-connector-outbound/README.md) | Function | File/folder operations (create, list, download, upload, copy, move, delete) and share creation, callable from BPMN service tasks. |
| [Inbound](nextcloud-connector-inbound/README.md) | Webhook | Receives Nextcloud file events (created, updated, deleted, renamed, copied, touched) via the `webhook_listeners` app and starts/correlates a BPMN process. |

See each connector's README for its supported operations, configuration fields, and payload
examples.

---

## Compatibility

| Component | Minimum Version | Notes |
| :--- | :--- | :--- |
| **Camunda Platform** | 8.5+ | Required for the unified `Document` API |
| **Connector SDK** | 8.9.5 | Defined in `pom.xml` |
| **Java Runtime (JRE)** | 21 | Required for compilation and runtime |
| **Nextcloud (outbound)** | 23+ | WebDAV and OCS Share API compatibility |
| **Nextcloud (inbound)** | 30+ | Requires the `webhook_listeners` app, bundled with Nextcloud server since v30 |

---

## Authentication & Secrets

Both connectors resolve credentials via Camunda secrets rather than hardcoded values — reference a
secret of your choice using a FEEL expression on the connector task, e.g. `secrets.MY_NEXTCLOUD_URL`.
`examples/` uses `NCA_NEXTCLOUD_APP_URL`/`_USER`/`_PASSWORD` as its naming convention, but that's
just this repo's choice — name your secrets however your organization's convention requires.

How `secrets.<NAME>` gets resolved at runtime depends on which secret provider the Connector
Runtime (or standalone service) is configured with, e.g.:

* **Camunda Console** secret provider (Camunda SaaS) — secrets managed in the Console UI.
* **Environment variables** — the built-in provider used by this repo's local dev profiles resolves
  `secrets.NAME` to the env var `SECRETS_NAME`.
* **A custom secret provider**, e.g. ilume's
  [`hashicorp-secret-provider`](https://github.com/ilume-Informatik-AG/hashicorp-secret-provider)
  for HashiCorp Vault (private repo).

> [!TIP]
> It is highly recommended to use a Nextcloud **App Password** (generated under *Personal Settings -> Security*) instead of your main account password.

---

## Local Development & Building

```bash
mvn clean package
```

For a local Nextcloud + Camunda dev setup, IntelliJ run/debug configurations, and deploying the
example processes under `examples/`, see [`DEVELOPMENT.md`](DEVELOPMENT.md).

---

## License

This project is licensed under the Apache License 2.0.

---

## Maintainers

* Fabian Stach (fabian.stach@ilume.de)
* Developed and Maintained by **Ilume Informatik AG**

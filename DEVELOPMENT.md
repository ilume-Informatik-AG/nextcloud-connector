# Local Development

Spin up a local Nextcloud instance to develop/test against:

```bash
cp .env.example .env
docker compose up -d
```

Then run the outbound connector standalone against it (targets Camunda's lightweight local
runtime, [`c8run`](https://github.com/camunda/camunda/tree/main/c8run), no auth, and embeds demo
Nextcloud secrets matching the compose defaults):

```bash
mvn -pl nextcloud-connector-outbound spring-boot:run -Dspring-boot.run.profiles=c8run
```

For a full local Camunda 8 Platform stack (Zeebe + Identity/Keycloak, OIDC auth) instead, use the
`local` profile and export `SECRETS_NCA_NEXTCLOUD_APP_URL`/`_USER`/`_PASSWORD` yourself first.

## Developing & Debugging in IntelliJ IDEA

1. **Open the project**: `File > Open...` and select the repo root `pom.xml` (or the root folder).
   IntelliJ imports both modules from the reactor `pom.xml`; no separate module import is needed.
2. **Start the dependencies** the connector talks to at runtime — these run outside IntelliJ:
   - Local Nextcloud: `cp .env.example .env && docker compose up -d` (see above).
   - A local Camunda runtime: download/start
     [`c8run`](https://github.com/camunda/camunda/tree/main/c8run) (`./start.sh` /
     `start.bat` from the c8run distribution). It exposes the gRPC gateway on `26500` and the REST
     API on `8080`, matching `application-c8run.yml`, with no auth.
3. **Create a Run/Debug Configuration** — two options, depending on what you're doing:

   **Option 1 (recommended for day-to-day dev): `LocalConnectorRuntime`**
   `nextcloud-connector-outbound/src/test/java/.../LocalConnectorRuntime.java` is a dev-only entry
   point (test scope, never packaged into the shipped jars) that hardcodes the `c8run` profile and
   carries a `@Deployment(resources = {"file:../examples/**/*.bpmn", "file:../examples/**/*.form"})`
   annotation, so it auto-deploys every example process/form on startup.
   - Type: *Application*
   - Main class: `de.ilume.nextcloud.outbound.LocalConnectorRuntime`
   - Use classpath of module: `nextcloud-connector-outbound`
   - Working directory: the module root (`nextcloud-connector-outbound`) — IntelliJ defaults to
     this already for a test-scope main class, but double-check it under *Modify options*, since
     the `@Deployment` resource paths are relative to it (`../examples/...`).
   - No VM options needed — just hit Run/Debug.

   **Option 2: `RuntimeApplication`** (mirrors the actual `spring-boot:run`/production entry point)
   - Type: *Application*
   - Main class: `de.ilume.nextcloud.outbound.RuntimeApplication`
   - Use classpath of module: `nextcloud-connector-outbound`
   - VM options: `-Dspring.profiles.active=c8run` (the `local` profile works the same way if
     you're targeting a full Camunda 8 Platform stack instead — remember to also export the
     `SECRETS_NCA_NEXTCLOUD_APP_*` env vars in the run configuration in that case, since
     `application-local.yml` doesn't embed demo secrets). This entry point doesn't auto-deploy
     anything — use [Deploying the Example Processes](#deploying-the-example-processes) below.
4. **Debug**: set breakpoints anywhere in `nextcloud-connector-outbound` (e.g. in
   `NextcloudExecutor` or a specific action handler) and launch the configuration with *Debug*
   instead of *Run*. Once the Spring context is up, the connector polls Zeebe for jobs against the
   c8run gateway; triggering a BPMN process that reaches a Nextcloud connector task will hit your
   breakpoints like any other Spring Boot app.
5. **Code style**: install the *google-java-format* IntelliJ plugin — this repo's `.idea` config
   (`.idea/google-java-format.xml`) enables it project-wide, so reformatting (`Ctrl/Cmd+Alt+L`)
   matches CI expectations.

> [!NOTE]
> Only `nextcloud-connector-outbound` has real source to run/debug; `nextcloud-connector-inbound`
> has no connector class yet (see Known Issues in `CLAUDE.md`), so there's nothing to launch there.

## Deploying the Example Processes

Each folder under `examples/` is a self-contained Camunda process application (BPMN + any forms)
demonstrating one connector action. If you're running `LocalConnectorRuntime` (see above), they're
deployed automatically on startup — nothing further to do. Otherwise (Option 2 above, `docker
compose`/standalone/shared-runtime deployments, or CI-adjacent testing), deploy them all in one
shot via the Camunda 8 REST API instead of manually importing each file in Camunda Modeler /
Operate:

```bash
./scripts/deploy-examples.sh
```

This POSTs every `.bpmn`/`.form` file under `examples/` to `http://localhost:8080/v2/deployments`
(c8run's REST API address, unauthenticated). Point it at a different runtime with an argument or
env var: `./scripts/deploy-examples.sh http://localhost:8080` or
`CAMUNDA_REST_ADDRESS=http://localhost:8080 ./scripts/deploy-examples.sh`. Re-run it any time after
editing an example — Camunda versions deployments automatically, so redeploying is safe. Once
deployed, start a process instance from Operate/Tasklist (or `zbctl`) to exercise the connector.

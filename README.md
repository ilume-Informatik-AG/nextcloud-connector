# Nextcloud Connector for Camunda 8

A custom Camunda 8 Connector enabling seamless file and share management in **Nextcloud** directly from your BPMN processes. The Connector uses the Nextcloud WebDAV and OCS APIs for secure, authenticated file and share operations.

---

## Features & Supported Operations

* 📁 **Create Folder**: Creates a new directory at a specified target path.
* 🔍 **List Folder Contents**: Lists resources in a folder with configurable depth and metadata selection (default, all, or custom WebDAV properties).
* 📥 **Download File**: Downloads a file from Nextcloud and registers it directly into the **Camunda Document Storage**.
* 📤 **Upload File**: Streams a Camunda Document from **Camunda Document Storage** into a specified path in Nextcloud.
* 📋 **Copy File**: Copies a file within Nextcloud.
* 📦 **Move File**: Moves a file within Nextcloud.
* ❌ **Delete File**: Deletes a file or folder in Nextcloud.
* 🔗 **Create Share**: Generates public links or shares files/folders with specific Users, Groups, Circles, or Talk Conversations using the Nextcloud OCS Share API (supports passwords and expiration dates).

---

## Compatibility

| Component | Minimum Version | Notes |
| :--- | :--- | :--- |
| **Camunda Platform** | 8.5+ | Required for the unified `Document` API |
| **Connector SDK** | 8.9.5 | Defined in `pom.xml` |
| **Java Runtime (JRE)** | 21 | Required for compilation and runtime |
| **Nextcloud** | 23+ | WebDAV and OCS API compatibility |

---

## Configuration

The Connector supports **Basic Authentication** (Username and Password / App Password) against
Nextcloud. The three authentication fields (**Server URL**, **Username**, **Password**) are plain
input mappings on the connector task — there are no fixed/required secret names. To comply with
security best practices, don't hardcode credentials into these fields; instead reference a secret
of your choice using a FEEL expression, e.g. in Camunda Web/Desktop Modeler:

| Field | Example value |
| :--- | :--- |
| Server URL | `secrets.MY_NEXTCLOUD_URL` |
| Username | `secrets.MY_NEXTCLOUD_USER` |
| Password | `secrets.MY_NEXTCLOUD_PASSWORD` |

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

### Tunable Parameters

These apply in both deployment modes (standalone service or mounted into a shared Connector
Runtime) and are read once at startup via `System.getProperty(key)` (falling back to
`System.getenv()`, then the default). To set as an environment variable, upper-case the key and
replace `.`/`-` with `_` (e.g. `camunda.server.files.max-size` → `CAMUNDA_SERVER_FILES_MAX_SIZE`).

| Parameter | Default | Required | Description |
| :--- | :--- | :--- | :--- |
| `camunda.server.files.max-size` | `10485760` (10 MB) | No | Max allowed size in bytes for file download/upload operations |
| `nextcloud.upload.buffer-stream` | `true` | No | Whether an upload's `InputStream` is fully buffered into memory before being sent to Nextcloud (needed because non-repeatable streams can't be retried if the connection drops mid-stream) |

For local development setup, IntelliJ run/debug configurations, and deploying the example
processes, see [`DEVELOPMENT.md`](DEVELOPMENT.md).

---

## Deployment Options

The outbound connector can be deployed in two ways from the same `mvn package` build.

### Option A: Standalone Service (Docker / Kubernetes Pod)
Run the connector as a self-contained microservice that registers with Camunda 8 and pulls jobs
automatically.

#### 1. Build the Executable JAR
```bash
mvn -pl nextcloud-connector-outbound -am clean package
```
This produces a standalone executable Spring Boot JAR under
`nextcloud-connector-outbound/target/*-exec.jar`.

#### 2. Run with Docker
`nextcloud-connector-outbound/Dockerfile` (Alpine Java 21) is for local image builds/iteration:
```bash
docker build -t camunda-nextcloud-connector nextcloud-connector-outbound

docker run -d \
  --name nextcloud-connector \
  -e CAMUNDA_CLIENT_GRPC_ADDRESS=http://localhost:26500 \
  -e SECRETS_NCA_NEXTCLOUD_APP_URL=https://nextcloud.example.com \
  -e SECRETS_NCA_NEXTCLOUD_APP_USER=demo \
  -e SECRETS_NCA_NEXTCLOUD_APP_PASSWORD=my-app-password \
  camunda-nextcloud-connector
```
The released image is instead built/pushed by CI via Cloud Native Buildpacks
(`mvn -pl nextcloud-connector-outbound -am package -Pbuild-image`), triggered by pushing a version
tag — see `CLAUDE.md` for the release process.

---

### Option B: Shared Connector Runtime (SaaS / Self-Managed)
If you already run a shared Camunda Connector Runtime (a single Java process/container that hosts
multiple connectors):

1. Build `nextcloud-connector-outbound/target/*-with-dependencies.jar` (shaded by
   `maven-shade-plugin`; `mvn -pl nextcloud-connector-outbound -am clean package`). It only bundles
   what a bare Connector Runtime doesn't already provide (`sardine` and its transitive deps) — no
   Spring Boot, no Camunda SDK classes, to avoid clashing with the host runtime's own versions.
2. Add that JAR to the `/opt/app` directory (or the classpath) of your shared Connector Runtime.
3. The runtime automatically discovers the connector via
   `de.ilume.nextcloud.outbound.NextcloudConnectorFunction`
   (`META-INF/services/io.camunda.connector.api.outbound.OutboundConnectorFunction`).

---

## Element Templates & BPMN Modeling

This project automatically generates the **Element Templates** required for modeling the Connector in Camunda Web Modeler or Desktop Modeler.

* The template generator runs during the Maven `package` phase.
* Generated templates are saved to the `element-templates/` directory.
* Import the generated JSON file into your Camunda Modeler project to enable the custom Nextcloud Connector task UI.

---

## Operations & Payload Examples

All file-based operations return a standardized **relative path** (starting with `/`) which allows you to easily chain outputs into inputs of subsequent steps in your BPMN diagram.

### 1. Create Folder
Creates a folder at the specified target path.

* **Inputs**:
    * **Target Path** (`action.path`): The parent folder (e.g. `/documents`).
    * **Folder Name** (`action.folderName`): The folder name to create (e.g. `Invoices`).
* **Output Example (`NextcloudFileOperationResponse`)**:
  ```json
  {
    "actionType": "CREATE_FOLDER",
    "source": null,
    "target": "/documents/Invoices",
    "fileName": "Invoices"
  }
  ```

### 2. List Folder Contents
Retrieves metadata of files and folders at a path.

* **Inputs**:
    * **Folder Path** (`action.path`): Path to list (e.g. `/documents`).
    * **Search Depth** (`action.depth`): `1` (shows immediate children), `0` (shows folder metadata only).
    * **Metadata Selection** (`action.metadataSelection`): `DEFAULT`, `ALL`, or `CUSTOM`.
    * **Additional Properties** (`action.additionalProperties`): A map of custom WebDAV namespaces and names (required only if `metadataSelection` is `custom`).
* **Output Example (`NextcloudListResponse`)**:
  ```json
  {
    "actionType": "LISTING_FOLDERS",
    "target": "/documents",
    "resources": [
      {
        "name": "invoice_1.pdf",
        "path": "/documents/invoice_1.pdf",
        "isDirectory": false,
        "size": 145020,
        "contentType": "application/pdf",
        "etag": "\"a87fd89...\"",
        "modified": "2026-06-11T08:30:00.000+00:00",
        "customProperties": {
          "favorite": "1",
          "tags": "Invoices, Finance"
        }
      },
      {
        "name": "Archive",
        "path": "/documents/Archive",
        "isDirectory": true,
        "size": -1,
        "contentType": "httpd/unix-directory",
        "etag": "\"6a292eee21078\"",
        "modified": "2026-06-11T09:15:22.000+00:00",
        "customProperties": {}
      }
    ]
  }
  ```

### 3. Download File
Downloads a file from Nextcloud and stores it in the Camunda 8 Document Storage.

* **Inputs**:
    * **Target Path** (`action.path`): Path of the file's parent folder (e.g. `/invoices`).
    * **File Name** (`action.fileName`): Name of the file to download (e.g. `invoice.pdf`).
* **Output Example (`NextcloudDownloadResponse`)**:
  ```json
  {
    "actionType": "DOWNLOAD_FILE",
    "target": "/invoices/invoice.pdf",
    "document": {
      "camunda.document.type": "camunda",
      "storeId": "in-memory",
      "documentId": "6ecef1e7-3d84-4421-89af-2ba72f96d34f",
      "contentHash": "af42f12f03eac596310b...",
      "metadata": {
        "contentType": "application/pdf",
        "fileName": "invoice.pdf",
        "size": 145020
      }
    }
  }
  ```

### 4. Upload File
Streams a file from Camunda 8 Document Storage to Nextcloud.

* **Inputs**:
    * **Target Path** (`action.path`): Path in Nextcloud where the file should be saved (e.g. `/uploads`).
    * **Document** (`action.document`): The Camunda Document reference object (mapped dynamically using a FEEL expression or variable, e.g. `=myDocument`).
* **Output Example (`NextcloudFileOperationResponse`)**:
  ```json
  {
    "actionType": "UPLOAD_FILE",
    "source": null,
    "target": "/uploads/my-report.pdf",
    "fileName": "my-report.pdf"
  }
  ```

### 5. Create Share
Uses the Nextcloud OCS Share API to share a file or folder.

For a file request use
- **Share Type** `PUBLIC_LINK`
- **Permissions** `CREATE`

* **Inputs**:
    * **Path** (`action.path`): Path of the file/folder to share.
    * **Share Type** (`action.shareType`): `USER`, `GROUP`, `PUBLIC_LINK`, `EMAIL`, `FEDERATED_CLOUD_SHARE`, `CIRCLE`, or `TALK_CONVERSATION`.
    * **Share With** (`action.shareWith`): Target user, group, circle ID, or conversation name (not required for `PUBLIC_LINK`).
    * **Allow Public Upload** (`action.publicUpload`): (**legacy attribute**. Dont use for File upload request!) Boolean, allows uploads to public shared folders (only for folders with `PUBLIC_LINK`).
    * **Password** (`action.sharePassword`): Optional password for public links (minimum 10 characters, at least 1 uppercase, 1 lowercase, and 1 number).
    * **Permissions** (`action.permissions`): `READ`, `UPDATE`, `CREATE`, `DELETE`, `SHARE`, `READ_UPDATE`, `READ_CREATE_UPDATE`, or `ALL`.
    * **Expiration Date** (`action.expireDate`): Optional date in `YYYY-MM-DD` format (only for `PUBLIC_LINK`).
* **Output Example (`NextcloudShareResponse`)**:
  ```json
  {
    "actionType": "CREATE_NEW_SHARE",
    "target": "/documents/shared-folder",
    "shareId": "42",
    "url": "https://nextcloud.example.com/s/abcdefgh",
    "token": "abcdefgh",
    "shareWith": null,
    "permissions": "1",
    "expiration": null
  }
  ```

---

## Error Handling

All standard WebDAV errors (e.g., `404 Not Found`, `401 Unauthorized`, `409 Conflict`) and network issues are intercepted by the Connector and wrapped into a Camunda `ConnectorException` with the error code `"FAIL"`. These errors are propagated back to the process instance as a job failure, allowing you to capture them using **BPMN Boundary Error Events** or handle them in the Camunda Operate UI.

---

## License

This project is licensed under the Apache License 2.0.

---

## Maintainers

* Fabian Stach (fabian.stach@ilume.de)
* Developed and Maintained by **Ilume Informatik AG**

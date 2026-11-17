# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Phorm — standalone XML document validation service built on the [phive](https://github.com/phax/phive) validation engine and [phive-rules](https://github.com/phax/phive-rules). Supports 30+ international e-invoicing/document standards (Peppol, XRechnung, ZUGFeRD, FatturaPA, Facturae, ZATCA, etc.). Java WAR application using the Helger Photon web framework, deployed to Tomcat.

## Build & Run Commands

```bash
# Build (requires Java 17+, Java 21 recommended)
mvn clean install

# Build via Docker Maven image (no local Java needed)
./build-with-docker.sh clean install

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=AppVersionTest

# Run a single test method
mvn test -Dtest=AppValidatorTest#testBasic

# Build Docker image (also tags as phelger/valsvc for backwards compat)
docker build --pull -t phelger/phorm .

# Run Docker container
docker run -d --name phorm -p 8080:8080 phelger/phorm
```

**Local dev server**: Run `com.helger.phorm.jetty.RunInJettyPhorm` as a main class (port 8080). Stop with `JettyStopPhorm`.

## Architecture

### API Layer (`com.helger.phorm.api`)
- `AbstractAPIInvoker` / `CommonAPIInvoker` — base classes handling X-Token auth and request parsing
- `ApiPostValidate` — POST `/api/validate/{vesid}` — validate XML against a specific VESID
- `ApiGetAllVESIDs` — GET `/api/get/vesids` — list all registered validation executor set IDs
- `ApiPostDetermineDocDetails` — POST `/api/determinedoctype` — auto-detect document format
- `ApiPostDetermineDocTypeAndValidate` — POST `/api/dd_and_validate` — detect + validate in one call
- `ApiGetAdminRepositories` — GET `/api/admin/repositories` — status of the external repositories (admin token)
- `ApiPostAdminReloadRepositories` — POST `/api/admin/repositories/reload` — atomically reload the external repositories; on any error the previous state is kept (admin token)
- The admin APIs are only registered if `phorm.api.admin.token` is set; `AbstractAPIInvoker.verifyAdminAuthOrSetForbidden` checks `X-Token` against it
- `ApiPostHybridValidate` — POST `/api/hybrid_validate` — validate a ZUGFeRD / Factur-X hybrid PDF: PDF carrier via kaltblut, then the embedded XML (optional `?country=DE|FR|OTHER`); the BT-24 of the embedded XML is checked against the XMP profile (`EHybridSpecificationID`), and any problem with the embedded XML is reported in a `hybrid-embedded-xml` layer

### Core Components
- `AppValidator` (`validation/`) — static registry of all `ValidationExecutorSet` instances; loads all phive-rules and phive-rules-legacy in a static initialiser on first use of the class. The external repositories (`phorm.repositories.file`) are loaded explicitly via `AppValidator.initExternalRepositories ()` from `AppWebAppListener.initGlobalSettings ()`; any error throws and aborts the startup. `loadExternalRepositories` swaps the external VES atomically under a read-write lock that also guards all registry lookups
- `repo/` — `ExternalVESLoader` reads the ph-diver repository configuration (`ph-diver-repo-config`, namespace `urn:fdc:com:helger:diver:repoconfig:v1.0`) and loads all VES eagerly with phive's `VESLoader` (one per repository, so `requires` only resolve within one repository); `RepositoryVESEnumerator` finds the VES via the top-level ToC (`toptoc="diver-xml"` is required). Only `localfs` repositories, only XSD/Schematron VES; duplicate VESIDs (between repositories or against built-ins) are errors
- `PhormDDD` (`ddd/`) — document details determination using the `ddd` library
- `AppConfig` — typed access to `application.properties` settings
- `AppWebAppListener` (`servlet/`) — application lifecycle initialization (extends Photon's `WebAppListener`)

### Servlet Mappings (web.xml)
- `/api/*` → PhotonAPIServlet (REST endpoints)
- `/ping/*` → PingPongServlet (health check)
- `/status/*` → StatusServlet
- `/` → RootServlet

### Configuration
- `src/main/resources/application.properties` — main config (debug mode, data path, API token, etc.)
- `src/main/resources/private-application.properties` — local overrides (gitignored, higher priority)
- `application.docker.properties` — Docker-specific overrides
- Config lookup rules: https://github.com/phax/ph-commons/wiki/ph-config

Key settings: `phorm.api.requiredtoken` (X-Token auth value), `phorm.api.admin.token` (admin API X-Token value; admin API disabled if empty), `phorm.repositories.file` (external repository XML; none if empty), `phorm.api.response.onfailure.http400`, `webapp.datapath` (default: `conf/`).

## Code Conventions

- Package root: `com.helger.phorm`
- Constants classes prefixed with `C` (e.g., `CApp`)
- Nullness annotations from JSpecify (`@NonNull`, `@Nullable`)
- `@Immutable` on stateless utility classes
- API responses support JSON (default), XML (`Accept: application/xml`), and HTML (`Accept: text/html`) based on Accept header

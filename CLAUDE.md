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
- `ApiPostHybridValidate` — POST `/api/hybrid_validate` — validate a ZUGFeRD / Factur-X hybrid PDF: PDF carrier via kaltblut, then the embedded XML (optional `?country=DE|FR|OTHER`)

### Core Components
- `AppValidator` (`validation/`) — static registry of all `ValidationExecutorSet` instances; loads all phive-rules and phive-rules-legacy in a static initialiser on first use of the class, not in `AppWebAppListener`
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

Key settings: `phorm.api.requiredtoken` (X-Token auth value), `phorm.api.response.onfailure.http400`, `webapp.datapath` (default: `conf/`).

## Code Conventions

- Package root: `com.helger.phorm`
- Constants classes prefixed with `C` (e.g., `CApp`)
- Nullness annotations from JSpecify (`@NonNull`, `@Nullable`)
- `@Immutable` on stateless utility classes
- API responses support JSON (default), XML (`Accept: application/xml`), and HTML (`Accept: text/html`) based on Accept header

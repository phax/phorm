# Standalone Validation Service

This repository contains a simple, standalone Validation Service accessible by API only.

# API

The services offers the following APIs.

* POST **`/api/validate/{vesid}`**
  * Validate the provided payload in the body against the validation rules, identified by `{vesid}`
  * Requires the HTTP header `X-Token` to have the configured value (see below for `valsvc.api.requiredtoken`)
  * The result is a JSON structure
* GET **`/api/get/vesids`**
  * Get a list of all registered VESIDs
  * The optional URL parameter `include-deprecated` can be used to also return registered, but deprecated VES IDs. No parameter value is needed
  * The result is a JSON structure

# Configuration

The Validation Service comes with one configuration file called `application.properties`.
The lookup rules for the file is defined in https://github.com/phax/ph-commons/wiki/ph-config

It supports the following settings:
* **`global.debug`**: overall debug mode. This enables additional checks that should not be executed every time (e.g. because they are slow or because they are spamming the logfile etc.). This flag has no impact on the logging level! This flag should be set to `true` in development mode, but to `false` in production mode. The value of this field is internally maintained in class `com.helger.commons.debug.GlobalDebug`.
* **`global.production`**: overall production mode. If this flag is set to `false` certain functionality not applicable in development environment (like mass mail sending) is disabled. This flag should be set to `true` in production mode.
* **`webapp.datapath`**: the path where all relevant data and settings are stored. This can e.g. be a relative path (like `conf` - relative to the web application directory) for development purposes but should be an absolute path (e.g. `/config/valsvc`) in production. Make sure the user running the Validation Service has write access to this folder.
* **`webapp.checkfileaccess`**: a flag that determines whether the directory of the web application should be checked for read and write access. This is only required if the data path inside the web application and should therefore always be `false`.
* **`webapp.testversion`**: a special indicator for the web application whether the version should be highlighted as a "test" version. Set to `true` in debug mode and `false` in production mode.
* **`valsvc.statusapi.enabled`**: a flag that indicates, if the status API (`/status`) should deliver data or not.
* **`valsvc.api.requiredtoken`**: the specific value of the `X-Token` header that must be provided to access the API. Customize this once and don't share it.

# Building

## From Source 

* Requires Java 11 or higher
* Build with Apache Maven 3.x - via `mvn clean install`

* Alternatively build with a Docker Maven image:

```
build-with-docker.cmd clean install
```
or
```
./build-with-docker.sh clean install
```

## Docker image

Building:

```shell
docker build --pull -t phelger/valsvc .
```

Running:

```shell
docker run -d --name valsvc -p 8080:8080 phelger/valsvc
```

Example curl command (use the right token and the right address):
```
curl -d "@base-example.xml" -H "Content-Type: application/xml" -H "X-Token: XXX" -X POST http://localhost:8080/api/validate/eu.peppol.bis3:invoice:latest
```

# News and noteworthy

2024-09-17 - updated to phive v10 and ph-diver v3
2024-05-23 - added UBL.BE rules as well 
2024-01-10 - added XRechnung rules as well

# Standalone Validation Service

This repository contains a simple, standalone Validation Service accessible by API only.

The implementation of the validation is based on the open source validation engine [phive](https://github.com/phax/phive) and the collection of rules [phive-rules](https://github.com/phax/phive-rules).

# Development environment

* Requires Java 11 or newer
* [Apache Maven](https://maven.apache.org) is used as the build tool. May be abstracted by a Docker image.
* Coding language: English

# API

The services offers the following APIs.

* POST **`/api/validate/{vesid}`**
  * Validate the provided payload in the body against the validation rules, identified by `{vesid}`
  * Requires the HTTP header `X-Token` to have the configured value (see below for `valsvc.api.requiredtoken`)
  * The result is a JSON structure
  * Test invocation:
    * `curl -X POST -H "Content-Type: application/xml" -H "X-Token: XXX" -d @src/test/resources/testfiles/peppol-bis3/base-example.xml http://localhost:8080/api/validate/eu.peppol.bis3:invoice:latest`
* GET **`/api/get/vesids`**
  * Get a list of all registered VESIDs
  * The optional URL parameter `include-deprecated` can be used to also return registered, but deprecated VES IDs. No parameter value is needed
  * The result is a JSON structure
    * `curl -X GET http://localhost:8080/api/get/vesids`

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
* **`valsvc.api.response.onfailure.http400`**: a flag to indicate, whether the API should return HTTP 400 (Bad Request) on failed validations or not. The default is `true` for backwards compatibility reasons.

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

Example curl command (use the correct "X-Token" and the right address):
```
curl -d "@base-example.xml" -H "Content-Type: application/xml" -H "X-Token: XXX" -X POST http://localhost:8080/api/validate/eu.peppol.bis3:invoice:latest
```

# Validation Service Updates

If an update is made to the validation, you have to do a `git pull` and recompile.

To make sure your own configuration is kept unchanged, my suggestion is to create a file `private-application.properties` 
  in the `src/main/resources` folder of your checked-out copy (same folder as `application.properties`), 
  where you can adjust or change all the configuration entries that are important to you.

The file with this specific name and within this folder has a higher priority than the default 
  `application.properties` file and is also marked as "ignored" in git, i.e. changes to this file
  are not overwritten during updates from the repository.
If the file is in the correct folder, it will also be included in the compilation process and 
  is therefore available out of the box for the validation service.

As an alternative to using `private-application.properties` you may also consider using
   environment variables or Java system properties for the configuration - 
   see https://github.com/phax/ph-commons/wiki/ph-config for details.

# News and noteworthy

2024-12-03 - the new configuration property "valsvc.api.response.onfailure.http400" can be used to disable returning HTTP 400 on validation failure
2024-09-17 - updated to phive v10 and ph-diver v3
2024-05-23 - added UBL.BE rules as well 
2024-01-10 - added XRechnung rules as well

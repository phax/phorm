# Example external repository

A small external repository for testing the support for external repositories (see *External repositories* in the main [README](../README.md)).
It is also used by the unit test `ExampleRepositoryTest`.

## Content

* `repositories.xml` - the repository configuration file for `phorm.repositories.file`. It contains the single repository `example-rules`
* `example-rules/` - the ph-diver repository with top-level ToC, ToCs per artifact and SHA-256 hashes. It is generated - don't edit it manually
* `source/` - the source files of the repository:
    * `order.xsd` - XML Schema for a simple order (namespace URI `urn:example:order:1.0`)
    * `order.sch` - Schematron with the business rules `ORD-01` to `ORD-04` (`ORD-02` is a warning, the others are errors)
    * `order.ves` - the VES `com.example:order:1.0.0` - XML Schema only
    * `order-rules.ves` - the VES `com.example:order-rules:1.0.0` - requires `com.example:order:1.0.0` and adds the Schematron
* `testfiles/` - documents to validate:
    * `order-valid.xml` - valid against both VES
    * `order-invalid-xsd.xml` - invalid currency code, so the XML Schema validation fails and the Schematron is skipped
    * `order-invalid-rules.xml` - valid against the XML Schema, but violates all business rules

ph-diver normalises the version, so the VESIDs are listed as `com.example:order:1` and `com.example:order-rules:1`.
Both spellings (`1` and `1.0.0`) can be used in the API.

## Usage

Add the following lines to `src/main/resources/private-application.properties` and start `RunInJettyPhorm` from the project root folder:

```properties
phorm.repositories.file=example-repository/repositories.xml
phorm.api.admin.token=admin-dev-token
```

A relative path in `phorm.repositories.file` is resolved against the current working directory.

Test invocations (replace `XXX` with the configured `phorm.api.requiredtoken`):

```bash
# The VES of the example repository carry "repository":"example-rules"
curl -H "X-Token: XXX" http://localhost:8080/api/get/vesids

# Valid
curl -X POST -H "Content-Type: application/xml" -H "X-Token: XXX" --data-binary @example-repository/testfiles/order-valid.xml http://localhost:8080/api/validate/com.example:order-rules:1.0.0

# Errors ORD-01, ORD-03, ORD-04 and warning ORD-02
curl -X POST -H "Content-Type: application/xml" -H "X-Token: XXX" --data-binary @example-repository/testfiles/order-invalid-rules.xml http://localhost:8080/api/validate/com.example:order-rules:1.0.0

# Status and reload
curl -H "X-Token: admin-dev-token" http://localhost:8080/api/admin/repositories
curl -X POST -H "X-Token: admin-dev-token" http://localhost:8080/api/admin/repositories/reload
```

## Changing the repository

1. Change the files in `source/`. A new VES needs a new call in `MainCreateExampleRepository`
2. Run `com.helger.phorm.repo.MainCreateExampleRepository` (in `src/test/java`) from the project root folder. It recreates `example-rules/`
3. Call `/api/admin/repositories/reload` - the response lists the `added` and `removed` VESIDs

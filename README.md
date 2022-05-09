# Standalone Validation Service

This repository contains a simple, standalone Validation Service accessible by API only.

## API

The services offers the following APIs.

* POST **`/api/validate/{vesid}`**
  * Validate the provided payload in the body against the validation rules, identified by `{vesid}`
  * Requires the HTTP header `X-Token` to have the value `E5AQdmi1efE126CzdGxs` (see `AbstractAPIInvoker`) (don't share this)
  * The result is a JSON structure
* GET **`/api/get/vesids`**
  * Get a list of all registered VESIDs
  * The optional URL parameter `include-deprecated` can be used to also return registered, but deprecated VES IDs. No parameter value is needed.
  * The result is a JSON structure

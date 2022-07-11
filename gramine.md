# What should change in SMS to add Gramine feature?

## Architecture

- ✅ Move `com.iexec.sms.tee.session.[attestation|cas|palaemon]` package to `com.iexec.sms.tee.session.scone`.
- ✅ Add a `gramineSessionTemplate.json.vm` file
- ✅ Rename `PalaemonSessionRequest` into `TeeSecretsSessionRequest`

## Code
### `TeeController` class

#### `generateTeeSession` method

- Signature should get a new `OrderTag` parameter (in `WorkerpoolAuthorization`?)

### `TeeSessionService` class

#### `generateTeeSession` method

- Signature should get a new `OrderTag` parameter

### `PalaemonSessionService` class

- ✅ Should be generified to provide a single secret filler service

### `TeeWorkflowConfiguration` class

- Rename all `preCompute[...]` and `postCompute[...]` fields to `sconePreCompute[...]` and `sconePostCompute[...]`.
- Add `graminePreCompute[...]` and `graminePostCompute[...]` fields.
- These changes should be made in `application.yml` too.
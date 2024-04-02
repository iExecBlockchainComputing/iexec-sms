# Changelog

All notable changes to this project will be documented in this file.

## [[8.5.1]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.5.1) 2024-04-02

### New Features

- Add `Authorization` header on `/tee/challenges/{chainTaskId}` endpoint. (#255 #256)

### Quality

- Use only two SQL statements to read `TeeTaskComputeSecret` and `Web2Secret` during TEE session creation. (#254)

## [[8.5.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.5.0) 2024-02-29

### New Features

- Export metrics on TEE challenges and Ethereum Credentials counts. (#230)
- Add a cache mechanism for secret existence. (#231)
- Expose cache metrics and refactor cache implementation. (#238)
- Set permissions to read-only on AES Key File. (#242)
- Backup AES Key File with database backup. (#243)
- Copy AES Key File on database copy. (#244)
- Delete AES Key File on database delete. (#245)
- Restore AES Key File on database restoration. (#246)
- Create TEE session with worker IPFS storage token and fallback on requester token. (#248)

### Bug Fixes

- Validate authorization first when working on compute secrets. (#229)
- Remove outdated empty log assertions in session tests. (#233)
- Do not use `@Data` lombok annotations on entities. (#235)
- Forbid access to sensitive APIs when no API key has been configured. (#249 #252)

### Quality

- Optimize chain calls in `AuthorizationService`. (#228)
- Fix licence headers in some source files. (#232)
- Move `EthereumCredentials` class to `com.iexec.sms.tee.challenge` package. (#234)
- Use `@DataJpaTest` to run secrets and TEE challenges tests on H2 database. (#236 #237)
- Insert secrets in a single SQL statement. (#238)
- Remove unused method in `AuthorizationService`, update `AuthorizationServiceTests`. (#240)
- Move `ApiKeyRequestFilter` class to `com.iexec.sms.admin` package. (#241)
- Clean App compute secret endpoints on `AppComputeSecretController`. (#247)

### Dependency Upgrades

- Upgrade to `iexec-common` 8.4.0. (#250)

## [[8.4.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.4.0) 2024-01-10

### New Features

- Add a security filter to activate an API Key mechanism on endpoints. (#207)
- Create admin endpoints foundation. (#208 #209)
- Add H2 database connection informations and storage ID decoding method. (#210)
- Add the ability to trigger a backup via a dedicated endpoint. (#211, #215)
- Add the ability to trigger a database restore via a dedicated endpoint. (#212)
- Add the ability to trigger a delete via a dedicated endpoint. (#213)
- Add the ability to trigger a backup replication via a dedicated endpoint. (#214)
- Add the ability to trigger a backup copy via a dedicated endpoint. (#217)
- Expose version through prometheus endpoint and through VersionController. (#220 #221)

### Bug Fixes

- Remove MockTeeConfiguration and set scone instead in `TeeTaskComputeSecretIntegrationTests`. (#222)
- Remove `/up` endpoint. (#224)
- Fix `README.md` and remove some code smells. (#225)

### Dependency Upgrades

- Upgrade to `eclipse-temurin:11.0.21_9-jre-focal`. (#219)
- Upgrade to Spring Boot 2.7.17. (#218)
- Upgrade to Spring Dependency Management Plugin 1.1.4. (#218)
- Upgrade to Spring Doc OpenAPI 1.7.0. (#220)
- Upgrade to `jenkins-library` 2.7.4. (#216)
- Upgrade to `iexec-commons-poco` 3.2.0. (#223)
- Upgrade to `iexec-common` 8.3.1. (#223)

## [[8.3.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.3.0) 2023-09-28

### Bug Fixes

- Fix and harmonize `Dockerfile entrypoint` in all Spring Boot applications. (#194)
- Check authorization before working with web2 or web3 secrets. (#200)

### Quality

- Upgrade to Gradle 8.2.1 with up-to-date plugins. (#193)
- Use `JpaRepository` in all repository classes for improved features. (#195)
- Remove session display option to prevent information leaks. (#197)
- Immutable classes for TEE enclaves and sessions manipulations. (#198)
- Immutable `TeeAppProperties` class with `@Builder` pattern. (#201)
- Fix Scone generated sessions permissions. (#202)
- Remove `VersionService#isSnapshot`. (#204)

### Dependency Upgrades

- Upgrade to `eclipse-temurin` 11.0.20. (#191)
- Upgrade to Spring Boot 2.7.14. (#192)
- Upgrade to Spring Dependency Management Plugin 1.1.3. (#192)
- Upgrade to `H2` 2.2.222. (#196)
- Upgrade to `jenkins-library` 2.7.3. (#199)
- Upgrade to `iexec-common` 8.3.0. (#203)
- Upgrade to `iexec-common-poco` 3.1.0. (#203)

## [[8.2.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.2.0) 2023-08-11

### New Features

- Export metrics on secrets counts. (#181)

### Quality

- Remove `nexus.intra.iex.ec` repository. (#180)
- Parameterize build of TEE applications while PR is not started. This allows faster builds. (#182 #184)
- Refactor secrets measures. (#185)
- Update `sconify.sh` script and use latest `5.7.2-wal` sconifier. (#186 #187 #188)
- Add `/metrics` endpoint. (#183)

### Dependency Upgrades

- Upgrade to `jenkins-library` 2.6.0. (#182)

## [[8.1.2]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.1.2) 2023-06-27

### Dependency Upgrades

- Upgrade to `iexec-commons-poco` 3.0.5. (#178)

## [[8.1.1]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.1.1) 2023-06-23

### Dependency Upgrades

- Upgrade to `iexec-common` 8.2.1. (#176)
- Upgrade to `iexec-commons-poco` 3.0.4. (#176)

## [[8.1.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.1.0) 2023-06-07

### New Features

- Enable Prometheus actuator. (#166)

### Bug Fixes

- Remove unused dependencies. (#168)
- Use DatasetAddress in `IEXEC_DATASET_FILENAME` environment variable. (#172)

### Dependency Upgrades

- Upgrade to `feign` 11.10. (#167)
- Upgrade to `iexec-common` 8.2.0. (#169 #170 #171 #173)
- Add new `iexec-commons-poco` 3.0.2 dependency. (#169 #170 #171 #173)

## [[8.0.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v8.0.0) 2023-03-06

### New Features

* Support SMS in enclave for Scone TEE tasks.
* Support Gramine framework for TEE tasks.
* Add `GET /up` client method in iexec-sms-library.
* Return a same `SmsClient` from the `SmsClientProvider` of iexec-sms-library when calling a same SMS URL.
* Add iExec banner at startup.
* Show application version on banner.

### Bug Fixes

* Remove TLS context on server.
* Remove `GET /secrets` endpoints.
* Remove non-TEE workflow.
* Remove enclave entrypoints from Gramine sessions since already present in manifests of applications.
* Update Scone transformation parameters to enable health checks in SMS in enclave.

### Quality

* Refactor secret model.
* Improve code quality.

### Dependency Upgrades

* Upgrade to Spring Boot 2.6.14.
* Upgrade to Gradle 7.6.
* Upgrade OkHttp to 4.9.0.
* Upgrade to Java 11.0.16 patch.
* Upgrade to `iexec-common` 7.0.0.
* Upgrade to `jenkins-library` 2.4.0.

## [[7.3.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v7.3.0) 2023-01-18

* Add endpoint to allow health checks.

## [[7.2.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v7.2.0) 2023-01-09

* Increments jenkins-library up to version 2.2.3. Enable SonarCloud analyses on branches and pull requests.
* Add `ReservedSecretKeyName` class to iexec-sms-library.

## [[7.1.1]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v7.1.1) 2022-11-29

* Update build workflow to 2.1.4, update documentation in README and add CHANGELOG.

## [[7.1.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v7.1.0) 2022-07-01

* An application developer can define a secret associated to its dapp address.
* Allow a requester to define multiple secrets in the SMS.
  They can then be used when buying an order.
* Add OpenFeign client library in dedicated iexec-sms-library jar.
* Define possible errors during TEE session creation for task feedback.
* Use Spring Boot 2.6.2.
* Use Java 11.0.15.

## [[7.0.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v7.0.0) 2021-12-14

* No evolution in SMS.

## [[6.1.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v6.1.0) 2021-11-30

* Generate enclave challenge private key with fixed length.

## [[6.0.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/v6.0.0) 2021-06-16

* Add TEE pre-compute stage for iExec Workers (confidential tasks inputs).
* Enable confidential task on iExec Workers with production enclave mode.
  (pre-compute, compute and post-compute stages).
* Expose trusted TEE configuration for iExec Workers.
* Add custom options for security policies.
* Disable requester post-compute.

## [[1.0.0]](https://github.com/iExecBlockchainComputing/iexec-sms/releases/tag/1.0.0) 2020-07-15

* First version.

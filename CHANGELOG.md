# Changelog

All notable changes to this project will be documented in this file.

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

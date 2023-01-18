# iExec Secret Management Service (iExec SMS)

## Overview

The iExec Secret Management Service (SMS) stores user secrets and provisions them to authorized Trusted Execution Environment (TEE) applications running on the iExec network.

### Details

* Confidential assets you have (password, token, API key, AES key, ..) should be securely transferred from your machine to the SMS over a TLS channel (iExec SDK is recommended). This operation is only done once.
* Internally, secrets are encrypted with standard AES encryption before being written to disk. 
* The iExec SMS secret provisioning policy is based on on-chain ACL (PoCo). PoCo smart contracts define simple ACL rules where individuals have ownership of on-chain objects they have deployed (workerpool, application, secret-dataset & requester).
* Each individual who is the owner of an object could define a policy on it. For example, "As a Requester (0xAlice), I only authorize my confidential Secret-Dataset (0xSecretOfAlice) to be used by the application of Bob (0xAppOfBob) I trust which will run on the Workerpool of Carl (0xWorkerpoolOfCarl)".
* When the secure application of Bob starts, the secret of Alice is written into a temporary session and sent over TLS to a dedicated  Configuration & Attestation Service (CAS) enclave responsible for communicating with the final application enclave.
* If the application enclave is legit (measurable with its mrenclave with Scone), it will receive the secrets.
* To sum up, if all checks are correct, the secret of Alice will cross the following environments: Alice-Host -> iExec-SMS -> Scone-CAS -> Bob-Scone-Application

## Configuration

The iExec Secret Management Service is available as an OCI image on [Docker Hub](https://hub.docker.com/r/iexechub/iexec-sms/tags).

To run properly, the iExec Secret Management Service requires:
* A blockchain node. iExec smart contracts must be deployed on the blockchain network.
* A Scontain Configuration and Attestation Service (CAS) to provision secrets to remote enclaves.
* Valid OCI images configurations for pre-compute and post-compute stages of TEE tasks executions.
* A valid OCI image configuration of a Scontain Local Attestation Service (LAS).
  This service will be deployed by an iExec Worker to compute TEE tasks.

The iExec SMS can be started locally for development purpose.
It is not advised to use an instance with such configuration in production.

You can configure the SMS with the following properties:

| Environment variable | Description | Type | Default value |
| --- | --- | --- | --- |
| IEXEC_SMS_PORT | Server HTTPS port. | Positive integer | `15443` |
| IEXEC_SMS_HTTP_ENABLED | Whether to start an http context when starting the SMS. | Boolean | `true` |
| IEXEC_SMS_HTTP_PORT | Server HTTP port. | Positive integer | `13300` |
| IEXEC_SMS_SSL_KEYSTORE | Path to the key store that holds the SSL certificate. | String | `src/main/resources/ssl-keystore-dev.p12` |
| IEXEC_SMS_SSL_KEYSTORE_PASSWORD | Password used to access the key store. | String | `whatever` |
| IEXEC_SMS_SSL_KEYSTORE_TYPE | Type of the key store. | Positive integer | `PKCS12` |
| IEXEC_SMS_SSL_KEYSTORE_ALIAS | Alias that identifies the key in the key store. | String | `iexec-core` |
| IEXEC_SMS_H2_URL | JDBC URL of the database. | URL | `jdbc:h2:file:/tmp/h2/sms-h2` |
| IEXEC_SMS_H2_CONSOLE | Whether to enable the H2 console. | Boolean | `false` |
| IEXEC_SMS_STORAGE_ENCRYPTION_AES_KEY_PATH  | Path to the key created and used to encrypt secrets. | String | `src/main/resources/iexec-sms-aes.key` |
| IEXEC_CHAIN_ID | Chain ID of the blockchain network to connect. | Positive integer | `17` |
| IEXEC_SMS_BLOCKCHAIN_NODE_ADDRESS | URL to connect to the blockchain node. | URL | `http://localhost:8545` |
| IEXEC_HUB_ADDRESS | Proxy contract address to interact with the iExec on-chain protocol. | String | `0xBF6B2B07e47326B7c8bfCb4A5460bef9f0Fd2002` |
| IEXEC_GAS_PRICE_MULTIPLIER | Transactions will be sent with `networkGasPrice * IEXEC_GAS_PRICE_MULTIPLIER`. | Float | `1.0` |
| IEXEC_GAS_PRICE_CAP | In Wei, will be used for transactions if `networkGasPrice * IEXEC_GAS_PRICE_MULTIPLIER > IEXEC_GAS_PRICE_CAP`. | Integer | `22000000000` |
| IEXEC_IS_SIDECHAIN | Define if iExec on-chain protocol is built on top of token (`false`) or native currency (`true`). | Boolean | `false` |
| IEXEC_SCONE_CAS_HOST | CAS service host. | String | `localhost` |
| IEXEC_SCONE_CAS_PORT | Server port of the CAS client API (session management). | Positive integer | `8081` |
| IEXEC_SCONE_CAS_PUBLIC_HOST | Server port of the CAS enclave API (remote attestation). Typically used by workers to execute TEE tasks. | Positive integer | `localhost` |
| IEXEC_SCONE_CAS_ENCLAVE_PORT | Scontain CAS service enclave port, used from worker host to attest applications running within enclaves. | Positive integer | `18765` |
| IEXEC_PALAEMON_TEMPLATE | Path to the template file used to generate configurations of TEE enclave sessions. | String | `src/main/resources/palaemonTemplate.vm` |
| IEXEC_SCONE_TOLERATED_INSECURE_OPTIONS | List of hardware or software Scone vulnerabilities to ignore. | String | |
| IEXEC_IGNORED_SGX_ADVISORIES | List of hardware or software Intel vulnerabilities to ignore. | String | |
| IEXEC_SMS_IMAGE_LAS_IMAGE | Scontain LAS OCI image to be used by workers to execute TEE tasks. LAS performs local attestation which creates a quote that CAS can verify. | String | |
| IEXEC_TEE_WORKER_PRE_COMPUTE_IMAGE | TEE enabled OCI image name for worker pre-compute stage of TEE tasks. | String | |
| IEXEC_TEE_WORKER_PRE_COMPUTE_FINGERPRINT | Fingerprint (aka mrenclave) of the TEE enabled worker pre-compute image. | String | |
| IEXEC_TEE_WORKER_PRE_COMPUTE_HEAP_SIZE_GB | Required heap size for a worker pre-compute enclave (in Giga Bytes). | Positive integer | `4` |
| IEXEC_TEE_WORKER_PRE_COMPUTE_ENTRYPOINT | Command executed when starting a container from the TEE enabled worker pre-compute image. | String | `java -jar /app/app.jar` |
| IEXEC_TEE_WORKER_POST_COMPUTE_IMAGE | TEE enabled OCI image name for worker post-compute stage of TEE tasks. | String | |
| IEXEC_TEE_WORKER_POST_COMPUTE_FINGERPRINT | Fingerprint (aka mrenclave) of the TEE enabled worker post-compute image. | String | |
| IEXEC_TEE_WORKER_POST_COMPUTE_HEAP_SIZE_GB | Required heap size for a worker post-compute enclave (in Giga Bytes). | Positive integer | `4` |
| IEXEC_TEE_WORKER_POST_COMPUTE_ENTRYPOINT | Command executed when starting a container from the TEE enabled worker post-compute image. | String | `java -jar /app/app.jar` |
| IEXEC_SMS_DISPLAY_DEBUG_SESSION | Whether to display TEE enclaves sessions configuration in SMS logs. | Boolean | `false` |

## Health checks

A health endpoint (`/actuator/health`) is enabled by default and can be accessed on the **IEXEC_SMS_HTTP_PORT**.
This endpoint allows to define health checks in an orchestrator or a [compose file](https://github.com/compose-spec/compose-spec/blob/master/spec.md#healthcheck).
No default strategy has been implemented in the [Dockerfile](Dockerfile) at the moment.

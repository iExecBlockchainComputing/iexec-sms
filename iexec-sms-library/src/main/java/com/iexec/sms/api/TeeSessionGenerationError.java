/*
 * Copyright 2022-2025 IEXEC BLOCKCHAIN TECH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iexec.sms.api;

public enum TeeSessionGenerationError {
    // region Authorization
    INVALID_AUTHORIZATION,
    EXECUTION_NOT_AUTHORIZED_EMPTY_PARAMS_UNAUTHORIZED,
    EXECUTION_NOT_AUTHORIZED_NO_MATCH_ONCHAIN_TYPE,
    EXECUTION_NOT_AUTHORIZED_GET_CHAIN_TASK_FAILED,
    EXECUTION_NOT_AUTHORIZED_TASK_NOT_ACTIVE,
    EXECUTION_NOT_AUTHORIZED_GET_CHAIN_DEAL_FAILED,
    EXECUTION_NOT_AUTHORIZED_INVALID_SIGNATURE,
    // endregion

    // region Pre-compute
    PRE_COMPUTE_GET_DATASET_SECRET_FAILED,
    // endregion

    // region App-compute
    APP_COMPUTE_NO_ENCLAVE_CONFIG,
    APP_COMPUTE_INVALID_ENCLAVE_CONFIG,
    // endregion

    // region Post-compute
    POST_COMPUTE_GET_ENCRYPTION_TOKENS_FAILED_EMPTY_BENEFICIARY_KEY,
    POST_COMPUTE_GET_STORAGE_TOKENS_FAILED,

    @Deprecated(forRemoval = true)
    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_WORKER_ADDRESS,
    @Deprecated(forRemoval = true)
    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_PUBLIC_ENCLAVE_CHALLENGE,
    @Deprecated(forRemoval = true)
    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CHALLENGE,
    @Deprecated(forRemoval = true)
    POST_COMPUTE_GET_SIGNATURE_TOKENS_FAILED_EMPTY_TEE_CREDENTIALS,
    // endregion

    // region Secure session generation
    SECURE_SESSION_STORAGE_CALL_FAILED,
    SECURE_SESSION_GENERATION_FAILED,
    SECURE_SESSION_NO_TEE_FRAMEWORK,
    // endregion

    // region Miscellaneous
    GET_TASK_DESCRIPTION_FAILED,
    NO_SESSION_REQUEST,
    NO_TASK_DESCRIPTION,
    // worker side
    UNKNOWN_ISSUE
    // endregion
}

/*
 * Copyright 2022 IEXEC BLOCKCHAIN TECH
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

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

// TODO: merge with Common version
@Data
@Builder
public class ApiResponseBody<D, E> {
    private final D data;
    private final E errors;

    /**
     * Return whether this response contains error(s).
     * In case of a collection of errors, checks whether the collection is empty.
     * Otherwise, checks whether the object is null.
     *
     * @return {@literal false} if {@code errors} is null or empty,
     * {@literal true} otherwise.
     */
    public boolean isError() {
        boolean isError = errors == null;

        if (errors instanceof Collection) {
            isError = !((Collection<?>) errors).isEmpty();
        }

        return isError;
    }
}

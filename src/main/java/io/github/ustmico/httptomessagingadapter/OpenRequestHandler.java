/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.ustmico.httptomessagingadapter;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.ustmico.httptomessagingadapter.kafka.MicoCloudEventImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Scope("singleton")
public class OpenRequestHandler {

    private ConcurrentHashMap<String, CompletableFuture<MicoCloudEventImpl<JsonNode>>> openRequests = new ConcurrentHashMap<>();

    /**
     * Returns the future for the request with the specified correlationId
     *
     * @param correlationId
     * @return
     */
    public synchronized Optional<CompletableFuture<MicoCloudEventImpl<JsonNode>>> getRequest(String correlationId) {
        return Optional.ofNullable(openRequests.get(correlationId));
    }

    /**
     * Adds an open request to the store for later retrieval
     *
     * @param correlationId
     * @param value
     * @return {@code false} if there is already a request with the specified correlationId
     */
    public synchronized boolean addRequest(String correlationId, CompletableFuture<MicoCloudEventImpl<JsonNode>> value) {
        log.info("Add open request to store with the id", correlationId);
        if (openRequests.containsKey(correlationId)) {
            return false;
        } else {
            openRequests.put(correlationId, value);
            log.info("The store has no a size of '{}'", openRequests.size());
            return true;
        }
    }

    /**
     * Deletes a request from the store
     *
     * @param correlationId
     */
    public synchronized void deleteRequest(String correlationId) {
        log.info("Deleting request with the id '{}'", correlationId);
        openRequests.remove(correlationId);
    }
}

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class MessageListener {

    @Autowired
    OpenRequestHandler openRequestHandler;

    @KafkaListener(topics = "${kafka.input-topic}", groupId = "${kafka.group-id}")
    public void receive(MicoCloudEventImpl<JsonNode> cloudEvent) {
        log.info("Received CloudEvent message: {}", cloudEvent);
        if (cloudEvent.getCorrelationId().isPresent()) {
            Optional<CompletableFuture<MicoCloudEventImpl<JsonNode>>> openRequestOptinal = openRequestHandler.getRequest(cloudEvent.getCorrelationId().get());
            if (openRequestOptinal.isPresent()) {
                log.info("Found the right saved request");
                openRequestOptinal.get().complete(cloudEvent);
            } else {
                log.info("There is no saved request for the correlationId '{}'", cloudEvent.getCorrelationId().get());
            }
        }
    }
}

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

package io.github.ustmico.httptomessagingadapter.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class CloudEventSerializer implements Serializer<MicoCloudEventImpl<JsonNode>> {
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {

    }

    @Override
    public byte[] serialize(String topic, MicoCloudEventImpl<JsonNode> data) {
        if (data == null)
            return null;
        else {
            String eventAsString = Json.encode(data);
            byte[] eventAsBytes = eventAsString.getBytes(StandardCharsets.UTF_8);
            log.debug("Serializing the event:'{}' to '{}' on topic '{}'", data, eventAsString, topic);
            return eventAsBytes;
        }
    }

    @Override
    public void close() {

    }
}

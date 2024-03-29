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

package io.github.ustmico.httptomessagingadapter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

/**
 * Configuration of the kafka connection.
 */
@Component
@Setter
@Getter
@ConfigurationProperties("kafka")
public class KafkaConfig {

    /**
     * The URLs of the Kafka bootstrap servers in a comma separated list.
     * Example: localhost:9092,localhost:9093
     */
    @NotBlank
    private String bootstrapServers;

    /**
     * The group id is a string that uniquely identifies the group
     * of consumer processes to which this consumer belongs.
     */
    @NotBlank
    private String groupId;

    /**
     * The Kafka input topic.
     */
    @NotBlank
    private String inputTopic;

    /**
     * The Kafka output topic.
     */
    @NotBlank
    private String outputTopic;

    /**
     * Used to report message processing errors
     */
    @NotBlank
    private String invalidMessageTopic;

    /**
     * Used to report routing errors
     */
    @NotBlank
    private String deadLetterTopic;

    @NotBlank
    private String testMessageOutputTopic;
}

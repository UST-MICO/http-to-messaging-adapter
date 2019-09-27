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

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class HttpRequestWrapper {

    private String bodyBase64;

    private Map<String, String> header;

    @JsonIgnore
    public String getBody() {
        if (bodyBase64 == null) {
            return null;
        }
        byte[] decodedBytes = Base64.getDecoder().decode(bodyBase64);
        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);
        return decodedString;
    }

    @JsonIgnore
    public void setBody(String body) {
        this.bodyBase64 = Base64.getEncoder().encodeToString(body.getBytes());
    }

    @JsonSetter("base64body")
    public void setBodyBase64(String bodyBase64) {
        this.bodyBase64 = bodyBase64;
    }

    @JsonGetter("base64body")
    public String getBodyBase64() {
        return bodyBase64;
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }
}

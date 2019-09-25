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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ustmico.httptomessagingadapter.config.KafkaConfig;
import io.github.ustmico.httptomessagingadapter.kafka.MicoCloudEventImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(value = "/")
public class HttpToMessagingAdapter {


    protected static final String CLOUD_EVENT_ATTRIBUTE_ADAPTER_REQUEST_URL = "adapterRequestUrl";
    protected static final String CLOUD_EVENT_ATTRIBUTE_ADAPTER_REQUEST_METHOD = "adapterRequestMethod";
    protected static final String CLOUD_EVENT_ATTRIBUTE_SOURCE_HTTP_TO_MESSAGING_ADAPTER = "/http-to-messaging-adapter";
    protected static final int MESSAGE_RESPONSE_TIMEOUT = 5;
    protected static final String DEFAULT_HTTP_RESPONSE_VALUE = "500";
    protected static final String CLOUD_EVENT_ATTRIBUTE_HTTP_RESPONSE_STATUS = "httpResponseStatus";
    protected static JsonNode defaultValue = null;

    public HttpToMessagingAdapter() {
        defaultValue = mapper.valueToTree(DEFAULT_HTTP_RESPONSE_VALUE);
    }

    @Autowired
    private KafkaTemplate<String, MicoCloudEventImpl<JsonNode>> kafkaTemplate;

    @Autowired
    private KafkaConfig kafkaConfig;

    @Autowired
    OpenRequestHandler openRequestHandler;

    private ObjectMapper mapper = new ObjectMapper();


    @RequestMapping(value = "/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.HEAD})
    public ResponseEntity getRequest(HttpServletRequest request) throws URISyntaxException, InterruptedException, ExecutionException {
        String uriWithQueryString = getUriWithQueryString(request);
        log.info("Request to {}, with the method {}, url {}", request.getRequestURI(), request.getMethod(), uriWithQueryString);
        try {
            MicoCloudEventImpl<JsonNode> micoCloudEvent = getMicoCloudEventFromHttpRequest(request, uriWithQueryString);

            kafkaTemplate.send(kafkaConfig.getOutputTopic(), micoCloudEvent);
            CompletableFuture<MicoCloudEventImpl<JsonNode>> openRequestFuture = new CompletableFuture<>();

            MicoCloudEventImpl<JsonNode> response = waitForResponseMessage(micoCloudEvent.getId(), openRequestFuture);

            ResponseEntity.BodyBuilder responseBuild = getResponseBuilderWithHttpStatus(response);

            if (response.getData() != null) {
                return responseBuild.body(response.getData());
            } else {
                return responseBuild.build();
            }
        } catch (TimeoutException e) {
            return getErrorResponse(HttpStatus.GATEWAY_TIMEOUT, "No response in time", e);
        } catch (IOException e) {
            return getErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while reading the body", e);
        }
    }

    private ResponseEntity.BodyBuilder getResponseBuilderWithHttpStatus(MicoCloudEventImpl<JsonNode> response) {
        int httpStatus = Integer.valueOf(response.getExtensionsMap().getOrDefault(CLOUD_EVENT_ATTRIBUTE_HTTP_RESPONSE_STATUS, defaultValue).asText());
        return ResponseEntity.status(httpStatus);
    }

    private MicoCloudEventImpl<JsonNode> waitForResponseMessage(String messageId, CompletableFuture<MicoCloudEventImpl<JsonNode>> openRequestFuture) throws InterruptedException, ExecutionException, TimeoutException {
        openRequestHandler.addRequest(messageId, openRequestFuture);
        return openRequestFuture.get(MESSAGE_RESPONSE_TIMEOUT, TimeUnit.MINUTES);
    }

    private ResponseEntity getErrorResponse(HttpStatus status, String errorMsg, Exception e) {
        log.error(errorMsg, e);
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(e);
    }

    private MicoCloudEventImpl<JsonNode> getMicoCloudEventFromHttpRequest(HttpServletRequest request, String uriWithQueryString) throws URISyntaxException, IOException {
        MicoCloudEventImpl<JsonNode> micoCloudEvent = new MicoCloudEventImpl<>();

        JsonNode uri = mapper.valueToTree(uriWithQueryString);
        JsonNode method = mapper.valueToTree(request.getMethod());

        micoCloudEvent.setExtension(CLOUD_EVENT_ATTRIBUTE_ADAPTER_REQUEST_URL, uri);
        micoCloudEvent.setExtension(CLOUD_EVENT_ATTRIBUTE_ADAPTER_REQUEST_METHOD, method);
        micoCloudEvent.setSource(new URI(CLOUD_EVENT_ATTRIBUTE_SOURCE_HTTP_TO_MESSAGING_ADAPTER));
        micoCloudEvent.setTime(ZonedDateTime.now());
        micoCloudEvent.setRandomId();
        micoCloudEvent.setIsErrorMessage(false);
        micoCloudEvent.setIsTestMessage(false);
        micoCloudEvent.setReturnTopic(kafkaConfig.getInputTopic());

        HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper();
        httpRequestWrapper.setHeader(getRequestHeaderMap(request));
        setRequestBody(request, httpRequestWrapper);

        JsonNode messageBody = mapper.valueToTree(httpRequestWrapper);
        micoCloudEvent.setData(messageBody);
        return micoCloudEvent;
    }

    private void setRequestBody(HttpServletRequest request, HttpRequestWrapper httpRequestWrapper) throws IOException {
        String requestMethod = request.getMethod().toUpperCase();
        if (HttpMethod.POST.matches(requestMethod) || HttpMethod.PUT.matches(requestMethod)) {
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            httpRequestWrapper.setBody(body);
        }
    }

    private Map<String, String> getRequestHeaderMap(HttpServletRequest request) {
        List<String> headers = Collections.list(request.getHeaderNames());
        Map<String, String> headerMap = new HashMap<>();
        for (String header : headers) {
            headerMap.put(header, request.getHeader(header));
        }
        return headerMap;
    }


    public static String getUriWithQueryString(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String queryString = request.getQueryString();
        if (queryString == null) {
            return requestUri;
        } else {
            return requestUri + "?" + queryString;
        }
    }


}

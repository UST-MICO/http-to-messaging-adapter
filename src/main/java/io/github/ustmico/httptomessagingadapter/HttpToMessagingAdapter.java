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

            openRequestHandler.addRequest(micoCloudEvent.getId(), openRequestFuture);
            MicoCloudEventImpl<JsonNode> response = openRequestFuture.get(5, TimeUnit.MINUTES);

            JsonNode defaultValue = mapper.valueToTree("200");
            int httpStatus = Integer.valueOf(response.getExtensionsMap().getOrDefault("httpResponseStatus", defaultValue).asText());
            ResponseEntity.BodyBuilder responseBuild = ResponseEntity.status(httpStatus);
            if (response.getData() != null) {
                return responseBuild.body(response.getData());
            } else {
                return responseBuild.build();
            }
        } catch (TimeoutException e) {
            String errorMsg = "No response in time";
            log.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(e);
        } catch (IOException e) {
            String errorMsg = "An error occurred while reading the body";
            log.error(errorMsg, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e);
        }
    }

    private MicoCloudEventImpl<JsonNode> getMicoCloudEventFromHttpRequest(HttpServletRequest request, String uriWithQueryString) throws URISyntaxException, IOException {
        MicoCloudEventImpl<JsonNode> micoCloudEvent = new MicoCloudEventImpl<>();

        JsonNode uri = mapper.valueToTree(uriWithQueryString);
        JsonNode method = mapper.valueToTree(request.getMethod());

        micoCloudEvent.setExtension("adapterRequestUrl", uri);
        micoCloudEvent.setExtension("adapterRequestMethod", method);
        micoCloudEvent.setSource(new URI("/http-to-messaging-adapter"));
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

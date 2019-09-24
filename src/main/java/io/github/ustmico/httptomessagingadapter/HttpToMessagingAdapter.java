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


    @RequestMapping(value = "/**", method = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS, RequestMethod.HEAD })
    public ResponseEntity getRequest(HttpServletRequest request) throws URISyntaxException, InterruptedException, ExecutionException {
        String uriWithQueryString = getUriWithQueryString(request);
        log.info("Request to {}, with the method {}, url {}", request.getRequestURI(), request.getMethod(), uriWithQueryString);
        MicoCloudEventImpl<JsonNode> micoCloudEvent = new MicoCloudEventImpl<>();
        micoCloudEvent.setRandomId();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode uri = mapper.valueToTree(uriWithQueryString);
        JsonNode method = mapper.valueToTree(request.getMethod());
        micoCloudEvent.setExtension("adapterRequestUrl",uri);
        micoCloudEvent.setExtension("adapterRequestMethod",method);
        micoCloudEvent.setSource(new URI("/http-to-messaging-adapter"));
        if ("POST".equalsIgnoreCase(request.getMethod()) || "PUT".equalsIgnoreCase(request.getMethod()))
        {
            try {
                String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                JsonNode jsonNodebody = mapper.valueToTree(body);
                micoCloudEvent.setData(jsonNodebody);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        kafkaTemplate.send(kafkaConfig.getOutputTopic(), micoCloudEvent);
        CompletableFuture<MicoCloudEventImpl<JsonNode>> openRequestFuture = new CompletableFuture<>();

        openRequestHandler.addRequest(micoCloudEvent.getId(),openRequestFuture);
        MicoCloudEventImpl<JsonNode> response = null;
        try {
            response = openRequestFuture.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException e) {
            String errorMsg = "No response in time";
            log.error(errorMsg);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();

        }
        JsonNode defaultValue = mapper.valueToTree("200");
        int httpStatus = Integer.valueOf(response.getExtensionsMap().getOrDefault("x-http-status",defaultValue).asText());
        ResponseEntity.BodyBuilder responseBuild = ResponseEntity.status(httpStatus);
        if(response.getData() != null){
            return responseBuild.body(response.getData());
        }else{
            return responseBuild.build();
        }
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

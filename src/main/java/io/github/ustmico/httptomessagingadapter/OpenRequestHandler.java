package io.github.ustmico.httptomessagingadapter;


import com.fasterxml.jackson.databind.JsonNode;
import io.github.ustmico.httptomessagingadapter.kafka.MicoCloudEventImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@Scope("singleton")
public class OpenRequestHandler {

    private ConcurrentHashMap<String, CompletableFuture<MicoCloudEventImpl<JsonNode>>> openRequests = new ConcurrentHashMap<>();

    public synchronized CompletableFuture<MicoCloudEventImpl<JsonNode>> getRequest(String correlationId){
       return openRequests.get(correlationId);
    }

    public synchronized boolean addRequest(String correlationId, CompletableFuture<MicoCloudEventImpl<JsonNode>> value){
        if(openRequests.containsKey(correlationId)){
            return false;
        }else{
            openRequests.put(correlationId, value);
            return true;
        }
    }
}

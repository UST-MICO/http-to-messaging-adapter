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
                openRequestOptinal.get().complete(cloudEvent);
            }
        }
    }
}

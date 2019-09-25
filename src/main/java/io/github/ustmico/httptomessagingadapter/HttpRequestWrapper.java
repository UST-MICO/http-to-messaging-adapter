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

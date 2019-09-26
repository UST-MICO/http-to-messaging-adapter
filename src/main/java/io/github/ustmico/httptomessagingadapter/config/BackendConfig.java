package io.github.ustmico.httptomessagingadapter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

@Component
@Setter
@Getter
@ConfigurationProperties("backend")
public class BackendConfig {

    @NotBlank
    String url;
}

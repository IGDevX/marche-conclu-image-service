package org.igdevx.imageservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true", matchIfMissing = false)
public class EurekaConfigOverride {

    private final EurekaClientConfigBean eurekaClientConfigBean;

    public EurekaConfigOverride(EurekaClientConfigBean eurekaClientConfigBean) {
        this.eurekaClientConfigBean = eurekaClientConfigBean;
    }

    @PostConstruct
    public void disableAutoRegistration() {
    }
}


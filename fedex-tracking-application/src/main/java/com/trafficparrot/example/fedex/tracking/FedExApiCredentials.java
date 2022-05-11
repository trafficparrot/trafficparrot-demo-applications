package com.trafficparrot.example.fedex.tracking;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;
import static org.thymeleaf.util.StringUtils.isEmpty;

@Component
@Scope(SCOPE_SINGLETON)
public class FedExApiCredentials {
    private String apiBaseUrl;
    private String bearerToken;

    public String getBearerToken() {
        return bearerToken;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiCredentials(String apiBaseUrl, String bearerToken) {
        this.apiBaseUrl = removeEnd(apiBaseUrl, "/");
        this.bearerToken = bearerToken;
    }

    public boolean hasApiCredentials() {
        return isNotBlank(bearerToken);
    }
}

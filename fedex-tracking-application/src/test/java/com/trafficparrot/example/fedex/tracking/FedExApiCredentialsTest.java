package com.trafficparrot.example.fedex.tracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FedExApiCredentialsTest {

    private static final String BEARER_TOKEN = "token";
    private static final String API_BASE_URL = "http://localhost:1234";
    private final FedExApiCredentials fedExApiCredentials = new FedExApiCredentials();

    @Test
    void setApiCredentialsStoresRemovesTrailingSlashFromBaseUrl() {
        fedExApiCredentials.setApiCredentials(API_BASE_URL + "/", BEARER_TOKEN);

        assertThat(fedExApiCredentials.getApiBaseUrl()).isEqualTo(API_BASE_URL);
    }

    @Test
    void setApiCredentialsStoresCredentials() {
        fedExApiCredentials.setApiCredentials(API_BASE_URL, BEARER_TOKEN);

        assertThat(fedExApiCredentials.getBearerToken()).isEqualTo(BEARER_TOKEN);
        assertThat(fedExApiCredentials.getApiBaseUrl()).isEqualTo(API_BASE_URL);
        assertThat(fedExApiCredentials.hasApiCredentials()).isTrue();
    }

    @Test
    void hasApiCredentialsInitiallyFalse() {
        assertThat(fedExApiCredentials.hasApiCredentials()).isFalse();
    }
}
package com.trafficparrot.example.fedex.tracking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationErrorControllerTest {

    private final ApplicationErrorController applicationErrorController = new ApplicationErrorController();

    @Test
    void redirectToLoginOnError() {
        assertThat(applicationErrorController.response()).isEqualTo("redirect:/login");
    }
}
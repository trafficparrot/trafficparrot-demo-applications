package com.trafficparrot.example.fedex.tracking;

import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class FedExTrackingControllerTest {

    private static final String API_BASE_URL = "http://example.com";
    private static final String API_KEY = "api-key-example";
    private static final String SECRET_KEY = "api-secret-example";
    private static final String EXAMPLE_ERROR_MESSAGE = "example error message";
    private static final String TRACKING_NUMBER = "123456789012";
    private static final String EXAMPLE_STATUS = "Picked up at 2019-08-14T13:33:00-04:00";

    private final Model model = mock(Model.class);
    private final FedExTrackingService trackingService = mock(FedExTrackingService.class);
    private final FedExTrackingController trackingController = new FedExTrackingController(trackingService);

    @Test
    void getTrackWhenLoggedInReturnsTrackPage() {
        given(trackingService.isLoggedIn()).willReturn(true);

        String page = trackingController.getTrack();

        assertThat(page).isEqualTo("track");
    }

    @Test
    void getTrackWhenLoggedInRedirectsToLogInPage() {
        given(trackingService.isLoggedIn()).willReturn(false);

        String page = trackingController.getTrack();

        assertThat(page).isEqualTo("redirect:/login");
    }

    @Test
    void getLoginReturnsLoginPage() {
        String page = trackingController.getLogin();

        assertThat(page).isEqualTo("login");
    }

    @Test
    void getCurrentWhenLoggedInRedirectsToTrackPage() {
        given(trackingService.isLoggedIn()).willReturn(true);

        String page = trackingController.getCurrent();

        assertThat(page).isEqualTo("redirect:/track");
    }

    @Test
    void getCurrentWhenLoggedInRedirectsToLogInPage() {
        given(trackingService.isLoggedIn()).willReturn(false);

        String page = trackingController.getCurrent();

        assertThat(page).isEqualTo("redirect:/login");
    }

    @Test
    void postLoginSuccessRedirectsToTrackPage() {
        given(trackingService.loginToApi(API_KEY, SECRET_KEY, API_BASE_URL)).willReturn(new LogInResponse(false, ""));

        String page = trackingController.postLogin(API_KEY, SECRET_KEY, API_BASE_URL, model);

        assertThat(page).isEqualTo("redirect:/track");
    }

    @Test
    void postLoginErrorDisplaysErrorOnLogInPage() {
        given(trackingService.loginToApi(API_KEY, SECRET_KEY, API_BASE_URL)).willReturn(new LogInResponse(true, EXAMPLE_ERROR_MESSAGE));

        String page = trackingController.postLogin(API_KEY, SECRET_KEY, API_BASE_URL, model);

        then(model).should().addAttribute("error", EXAMPLE_ERROR_MESSAGE);
        assertThat(page).isEqualTo("login");
    }

    @Test
    void postTrackRedirectToLogInPageWhenNotLoggedIn() {
        given(trackingService.isLoggedIn()).willReturn(false);

        String page = trackingController.postTrack(TRACKING_NUMBER, model);

        assertThat(page).isEqualTo("redirect:/login");
    }

    @Test
    void postTrackSuccessDisplaysStatusOnTrackPage() {
        given(trackingService.isLoggedIn()).willReturn(true);
        given(trackingService.trackByTrackingNumber(TRACKING_NUMBER)).willReturn(new TrackingResponse(false, EXAMPLE_ERROR_MESSAGE, EXAMPLE_STATUS));

        String page = trackingController.postTrack(TRACKING_NUMBER, model);

        then(model).should().addAttribute("status", EXAMPLE_STATUS);
        assertThat(page).isEqualTo("track");
    }

    @Test
    void postTrackErrorDisplaysErrorOnTrackPage() {
        given(trackingService.isLoggedIn()).willReturn(true);
        given(trackingService.trackByTrackingNumber(TRACKING_NUMBER)).willReturn(new TrackingResponse(true, EXAMPLE_ERROR_MESSAGE, ""));

        String page = trackingController.postTrack(TRACKING_NUMBER, model);

        then(model).should().addAttribute("error", EXAMPLE_ERROR_MESSAGE);
        assertThat(page).isEqualTo("track");
    }
}
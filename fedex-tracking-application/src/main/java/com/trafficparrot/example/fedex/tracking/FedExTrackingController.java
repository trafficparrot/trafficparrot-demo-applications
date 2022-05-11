package com.trafficparrot.example.fedex.tracking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FedExTrackingController {

    private final FedExTrackingService trackingService;

    @Autowired
    public FedExTrackingController(FedExTrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("track")
    public String getTrack() {
        if (trackingService.isLoggedIn()) {
            return "track";
        } else {
            return "redirect:/login";
        }
    }

    @GetMapping("login")
    public String getLogin() {
        return "login";
    }

    @GetMapping("error")
    public String getError() {
        return "login";
    }

    @GetMapping("*")
    public String getCurrent() {
        if (trackingService.isLoggedIn()) {
            return "redirect:/track";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("/login")
    public String postLogin(
            @RequestParam(name = "fedex-api-base-url") String apiBaseUrl,
            @RequestParam(name = "fedex-api-key") String apiKey,
            @RequestParam(name = "fedex-secret-key") String secretKey,
            Model model) {

        LogInResponse logInResponse = trackingService.loginToApi(apiKey, secretKey, apiBaseUrl);
        if (logInResponse.isError) {
            model.addAttribute("error", logInResponse.errorMessage);
            return "login";
        } else {
            return "redirect:/track";
        }
    }

    @PostMapping("/track")
    public String postTrack(
            @RequestParam(name = "fedex-tracking-number") String trackingNumber,
            Model model) {

        if (!trackingService.isLoggedIn()) {
            return "redirect:/login";
        }

        TrackingResponse trackingResponse = trackingService.trackByTrackingNumber(trackingNumber);
        if (trackingResponse.isError) {
            model.addAttribute("error", trackingResponse.errorMessage);
        } else {
            model.addAttribute("status", trackingResponse.latestStatus);
        }
        model.addAttribute("trackingNumber", trackingNumber);
        return "track";
    }
}

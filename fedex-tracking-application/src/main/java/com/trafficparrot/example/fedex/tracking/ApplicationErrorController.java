package com.trafficparrot.example.fedex.tracking;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ApplicationErrorController implements ErrorController {

    @RequestMapping({"${server.error.path:${error.path:/error}}"})
    public String response() {
        return "redirect:/login";
    }
}

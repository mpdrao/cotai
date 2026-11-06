package dev.cotai.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping({"/l/{slug}", "/r/{reservationId}", "/dashboard", "/login", "/register", "/lists/new", "/lists/{id}"})
    public String forward() {
        return "forward:/index.html";
    }
}

package dev.cotai.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SpaForwardController {

    @GetMapping({"/l/{slug}", "/r/{reservationId}", "/dashboard", "/login", "/register", "/lists/new", "/lists/{id}"})
    String forward() {
        return "forward:/index.html";
    }
}

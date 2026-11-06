package dev.cotai.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards Angular client-side routes to the SPA entry point when the web build is bundled in the
 * jar (production). In development the Angular dev server handles these routes itself.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/l/{slug}", "/r/{reservationId}", "/dashboard", "/login", "/register", "/lists/new", "/lists/{id}"})
    public String forward() {
        return "forward:/index.html";
    }
}

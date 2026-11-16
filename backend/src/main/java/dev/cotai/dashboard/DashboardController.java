package dev.cotai.dashboard;

import dev.cotai.dashboard.DashboardDtos.ContributionPage;
import dev.cotai.dashboard.DashboardDtos.FundraisingMetrics;
import dev.cotai.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/gift-lists/{giftListId}")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/metrics")
    public FundraisingMetrics metrics(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId) {
        return service.metrics(JwtService.organizerId(jwt), giftListId);
    }

    @GetMapping("/payments")
    public ContributionPage payments(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID giftListId,
                                     @RequestParam(required = false) Long itemId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return service.contributions(JwtService.organizerId(jwt), giftListId, itemId, page, size);
    }
}

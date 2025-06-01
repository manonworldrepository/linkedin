package com.example.linkedin.controller;

import com.example.linkedin.service.LinkedInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/jobs")
@Tag(name = "Job Search API", description = "Search LinkedIn jobs and filter Easy Apply to get direct apply links")
public class JobController {
    private final LinkedInService linkedInService;

    public JobController(LinkedInService linkedInService) {
        this.linkedInService = linkedInService;
    }

    @Operation(summary = "Search jobs and return direct apply links by country")
    @GetMapping("/search")
    public Mono<List<String>> search(
        @RequestParam String query,
        @RequestParam String country
    ) {
        return linkedInService.getDirectApplyLinks(query, country);
    }
}


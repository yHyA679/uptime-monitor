package com.yahya.uptime_monitor.controller;

import com.yahya.uptime_monitor.exception.GlobalExceptionHandler;
import com.yahya.uptime_monitor.service.WebsiteCheckCoordinator;
import com.yahya.uptime_monitor.service.WebsiteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiErrorResponseTest {

    private WebsiteService websiteService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        websiteService = mock(WebsiteService.class);
        WebsiteController controller = new WebsiteController(
                websiteService,
                mock(WebsiteCheckCoordinator.class)
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void returnsConsistentValidationErrors() throws Exception {
        mockMvc.perform(post("/api/websites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " ",
                                  "url": "ftp://example.com",
                                  "checkIntervalSeconds": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message", containsString("name:")))
                .andExpect(jsonPath("$.message", containsString("url:")))
                .andExpect(jsonPath("$.message", containsString("checkIntervalSeconds:")))
                .andExpect(jsonPath("$.path").value("/api/websites"));
    }

    @Test
    void returnsConsistentConflictAndNotFoundErrors() throws Exception {
        when(websiteService.addWebsite(any())).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A website with this URL already exists"
        ));
        when(websiteService.getStats(404L)).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Website with id 404 was not found"
        ));

        mockMvc.perform(post("/api/websites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API",
                                  "url": "https://example.com",
                                  "checkIntervalSeconds": 60
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value(
                        "A website with this URL already exists"
                ))
                .andExpect(jsonPath("$.path").value("/api/websites"));

        mockMvc.perform(get("/api/websites/404/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Website with id 404 was not found"))
                .andExpect(jsonPath("$.path").value("/api/websites/404/stats"));
    }

    @Test
    void returnsConsistentMalformedJsonErrors() throws Exception {
        mockMvc.perform(post("/api/websites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or invalid request value"))
                .andExpect(jsonPath("$.path").value("/api/websites"));
    }
}

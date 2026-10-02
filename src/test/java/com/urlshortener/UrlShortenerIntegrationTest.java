package com.urlshortener;

import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class UrlShortenerIntegrationTest {
    @Autowired
    WebApplicationContext context;
    @Autowired
    ShortUrlRepository repository;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createRedirectThenAnalyticsShowsOneClick() throws Exception {
        String body = mockMvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://example.com/path\",\"customAlias\":\"flow123\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shortCode").value("flow123")).andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/flow123")).andExpect(status().isFound()).andExpect(header().string("Location", "https://example.com/path")).andExpect(header().string("Cache-Control", "no-store"));
        mockMvc.perform(get("/api/v1/urls/flow123/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void rejectsInvalidScheme() throws Exception {
        mockMvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"javascript:alert(1)\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void blankUrlFailsBeanValidation() throws Exception {
        mockMvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/urls/unknown")).andExpect(status().isNotFound());
    }

    @Test
    void customAliasCanBeCreatedAndDuplicateReturns409() throws Exception {
        String payload = "{\"url\":\"https://example.com\",\"customAlias\":\"custom1\"}";
        mockMvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
    }
}

package com.urlshortener;

import com.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UrlShortenerIntegrationTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    ShortUrlRepository repo;

    @BeforeEach
    void clean() {
        repo.deleteAll();
    }

    @Test
    void create() throws Exception {
        mvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://example.com/a\",\"customAlias\":\"abc123\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.shortCode").value("abc123")).andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123"));
    }

    @Test
    void redirectAndAnalytics() throws Exception {
        mvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://example.com/target\",\"customAlias\":\"go123\"}")).andExpect(status().isCreated());
        mvc.perform(get("/go123")).andExpect(status().isFound()).andExpect(header().string("Location", "https://example.com/target")).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/v1/urls/go123/analytics")).andExpect(status().isOk()).andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void metadata() throws Exception {
        mvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"https://example.com\",\"customAlias\":\"meta1\"}"));
        mvc.perform(get("/api/v1/urls/meta1")).andExpect(status().isOk()).andExpect(jsonPath("$.originalUrl").value("https://example.com"));
    }

    @Test
    void validationErrorEnvelope() throws Exception {
        mvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"\",\"customAlias\":\"x\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.path").value("/api/v1/urls/create")).andExpect(jsonPath("$.fieldErrors.url").exists());
    }

    @Test
    void aliasConflict() throws Exception {
        String body = "{\"url\":\"https://example.com\",\"customAlias\":\"same1\"}";
        mvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/urls/create").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
    }
}

package org.mangadex.mcw;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
class HelloControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homeGreeter() throws Exception {
        mockMvc.perform(get("/"))
               .andExpect(status().is2xxSuccessful())
               .andExpect(content().string(containsString("MCW")));
    }

    @Test
    void redirectsToHome() throws Exception {
        mockMvc.perform(get("/{rand}", UUID.randomUUID()))
               .andExpect(status().is3xxRedirection())
               .andExpect(header().string(LOCATION, "/"));
    }

}

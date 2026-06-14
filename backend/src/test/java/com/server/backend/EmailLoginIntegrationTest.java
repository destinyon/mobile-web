package com.server.backend;

import com.server.backend.auth.service.EmailCodeSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.email.debug=true",
        "app.email.interval-seconds=0"
})
@AutoConfigureMockMvc
class EmailLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void sendsCodeAndLogsInWithEmail() throws Exception {
        String sendResponse = mockMvc.perform(post("/api/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Player@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("player@example.com"))
                .andExpect(jsonPath("$.data.debugCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String code = sendResponse.replaceFirst(".*\"debugCode\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"player@example.com\",\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("player@example.com"))
                .andExpect(jsonPath("$.data.user.nickname").value("羽球用户player"));
    }

    @Test
    void rejectsWrongCode() throws Exception {
        mockMvc.perform(post("/api/auth/email-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@example.com\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/email-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@example.com\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @TestConfiguration
    static class EmailLoginTestConfig {
        @Bean
        @Primary
        EmailCodeSender emailCodeSender() {
            return (email, code, validSeconds) -> {
            };
        }
    }
}

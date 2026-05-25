package com.server.backend;

import com.server.backend.auth.service.WxSessionClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.wx.app-id=test-app-id",
        "app.wx.app-secret=test-app-secret"
})
@AutoConfigureMockMvc
class WxLoginIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTokenWhenWechatCodeResolvesToOpenid() throws Exception {
        StubWxSessionClient.mode = "success";

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid-code\",\"nickname\":\"羽球用户\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.nickname").value("羽球用户"));
    }

    @Test
    void wechatErrorResponseDoesNotBecomeServerError() throws Exception {
        StubWxSessionClient.mode = "wechat-error";

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad-code\"}"))
                .andExpect(status().is4xxClientError())
                .andExpect(status().is(not(500)))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void wechatNetworkFailureDoesNotLeakAsGenericServerError() throws Exception {
        StubWxSessionClient.mode = "network-error";

        mockMvc.perform(post("/api/auth/wx-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"timeout-code\"}"))
                .andExpect(status().is5xxServerError())
                .andExpect(status().is(not(500)))
                .andExpect(jsonPath("$.success").value(false));
    }

    @TestConfiguration
    static class WxLoginTestConfig {
        @Bean
        @Primary
        WxSessionClient wxSessionClient() {
            return new StubWxSessionClient();
        }
    }

    static class StubWxSessionClient implements WxSessionClient {
        static String mode = "success";

        @Override
        public Map<?, ?> exchangeCode(String code) {
            return switch (mode) {
                case "wechat-error" -> Map.of("errcode", 40029, "errmsg", "invalid code");
                case "network-error" -> throw new IllegalStateException("connect timeout");
                default -> Map.of("openid", "test-openid-" + code);
            };
        }
    }
}

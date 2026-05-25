package com.server.backend.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Component
public class WechatWxSessionClient implements WxSessionClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String appId;
    private final String appSecret;
    private final String sessionUrl;

    @Autowired
    public WechatWxSessionClient(
            @Value("${app.wx.app-id}") String appId,
            @Value("${app.wx.app-secret}") String appSecret,
            ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.objectMapper = objectMapper;
        this.sessionUrl = "https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    }

    WechatWxSessionClient(String appId, String appSecret, RestClient restClient, ObjectMapper objectMapper, String sessionUrl) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.sessionUrl = sessionUrl;
    }

    @Override
    public Map<?, ?> exchangeCode(String code) {
        String body = restClient.get()
                .uri(sessionUrl,
                        appId, appSecret, code)
                .retrieve()
                .body(String.class);
        try {
            return objectMapper.readValue(body, Map.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid WeChat session response", ex);
        }
    }
}

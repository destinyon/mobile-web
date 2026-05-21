package com.server.backend.auth;

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
    private final String appId;
    private final String appSecret;

    public WechatWxSessionClient(
            @Value("${app.wx.app-id}") String appId,
            @Value("${app.wx.app-secret}") String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public Map<?, ?> exchangeCode(String code) {
        return restClient.get()
                .uri("https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code",
                        appId, appSecret, code)
                .retrieve()
                .body(Map.class);
    }
}

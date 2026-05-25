package com.server.backend.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WechatWxSessionClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void parsesWechatTextPlainJsonErrorResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sns/jscode2session", exchange -> {
            byte[] body = """
                    {"errcode":40029,"errmsg":"invalid code"}
                    """.getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        WechatWxSessionClient client = new WechatWxSessionClient(
                "test-app-id",
                "test-secret",
                restClient(),
                new ObjectMapper(),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code");

        Map<?, ?> response = client.exchangeCode("bad-code");

        assertThat(response.get("errcode")).isEqualTo(40029);
        assertThat(response.get("errmsg")).isEqualTo("invalid code");
    }

    private RestClient restClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}

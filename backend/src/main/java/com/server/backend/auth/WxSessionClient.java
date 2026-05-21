package com.server.backend.auth;

import java.util.Map;

public interface WxSessionClient {
    Map<?, ?> exchangeCode(String code);
}

package com.server.backend.auth.service;

import java.util.Map;

public interface WxSessionClient {
    Map<?, ?> exchangeCode(String code);
}

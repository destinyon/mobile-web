package com.server.backend;

import com.server.backend.common.BusinessException;
import com.server.backend.upload.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceTest {

    @Test
    void rejectsFilesLargerThanConfiguredLimitBeforeOssUpload() {
        UploadService uploadService = new UploadService(
                "https://oss-cn-beijing.aliyuncs.com",
                "bucket",
                "access-key",
                "secret",
                "https://cdn.example.test",
                100);
        byte[] sixMb = new byte[50 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", sixMb);

        assertThatThrownBy(() -> uploadService.upload(file))
                .isInstanceOfSatisfying(BusinessException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
                    assertThat(ex.getMessage()).contains("100MB");
                });
    }
}

package com.server.backend;

import com.server.backend.auth.service.AuthService;
import com.server.backend.common.GlobalExceptionHandler;
import com.server.backend.upload.controller.UploadController;
import com.server.backend.upload.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UploadController.class)
@Import(GlobalExceptionHandler.class)
class UploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UploadService uploadService;

    @MockBean
    private AuthService authService;

    @Test
    void deleteUploadAcceptsObjectKeyQueryParameter() throws Exception {
        mockMvc.perform(delete("/api/upload")
                        .header("Authorization", "Bearer token")
                        .param("objectKey", "uploads/a.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(uploadService).delete("uploads/a.jpg");
    }

    @Test
    void deleteUploadMissingObjectKeyReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/upload")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}

package com.ttcsn.user.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.user.dto.response.AuthResponse;
import com.ttcsn.user.dto.response.UserSummary;
import com.ttcsn.user.entity.enums.UserRole;
import com.ttcsn.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test AuthController standalone MockMvc — kiểm tra wiring endpoint /api/auth/**,
 * không cần Spring context/DB. Logic service đã phủ ở AuthServiceImplTest.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .build();
    }

    @Test
    void refreshReturnsNewAccessTokenWithoutRotation() throws Exception {
        AuthResponse res = new AuthResponse(
                "acc-new", null, "Bearer", 7200,
                new UserSummary(12L, "a@b.c", "User 12", UserRole.CUSTOMER));
        when(authService.refresh(any())).thenReturn(res);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"ref-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc-new"))
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refreshBlankTokenRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginResponseContainsBothTokens() throws Exception {
        AuthResponse res = new AuthResponse(
                "acc", "ref", "Bearer", 7200,
                new UserSummary(12L, "a@b.c", "User 12", UserRole.CUSTOMER));
        when(authService.login(any())).thenReturn(res);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.c\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc"))
                .andExpect(jsonPath("$.refreshToken").value("ref"));
    }

    @Test
    void registerReturns201WithTokens() throws Exception {
        AuthResponse res = new AuthResponse(
                "acc", "ref", "Bearer", 7200,
                new UserSummary(13L, "new@b.c", "New User", UserRole.CUSTOMER));
        when(authService.register(any())).thenReturn(res);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "fullName", "New User", "email", "new@b.c",
                                "password", "password123", "phoneNumber", "0900000000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("acc"))
                .andExpect(jsonPath("$.refreshToken").value("ref"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }
}

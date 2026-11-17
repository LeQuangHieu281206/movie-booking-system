package com.ttcsn.payment.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.payment.dto.response.PaymentHistoryResponse;
import com.ttcsn.payment.entity.PaymentStatus;
import com.ttcsn.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    @Test
    void createPaymentUsesHeaderUserAndIgnoresClientUserAndAmount() throws Exception {
        PaymentService service = mock(PaymentService.class);
        when(service.createSePayPayment(1L, 42L))
                .thenReturn("https://qr.sepay.vn/img?acc=123&bank=MB&amount=100000&des=DH1");

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PaymentController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        mvc.perform(post("/api/createPayment")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bookingId": 1,
                                  "userId": 999,
                                  "amount": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrUrl").value(
                        "https://qr.sepay.vn/img?acc=123&bank=MB&amount=100000&des=DH1"));

        verify(service).createSePayPayment(1L, 42L);
    }

    @Test
    void createPaymentWithoutUserIdHeaderIsRejected() throws Exception {
        PaymentService service = mock(PaymentService.class);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PaymentController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        mvc.perform(post("/api/createPayment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\": 1}"))
                .andExpect(status().isBadRequest());

        verify(service, never()).createSePayPayment(anyLong(), anyLong());
    }

    @Test
    void getPaymentsByBookingReturnsHistoryForAuthenticatedUser() throws Exception {
        PaymentService service = mock(PaymentService.class);
        PaymentHistoryResponse item = new PaymentHistoryResponse(
                10L,
                1L,
                new BigDecimal("100000"),
                "VND",
                PaymentStatus.SUCCESS,
                "SEPAY",
                "9001",
                "DH1",
                "https://qr.sepay.vn/img?acc=123&bank=MB&amount=100000&des=DH1",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(service.getPaymentsByBookingId(1L, 42L, "CUSTOMER")).thenReturn(List.of(item));

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PaymentController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        mvc.perform(get("/api/getPayments/booking/1")
                        .header("X-User-Id", "42")
                        .header("X-User-Role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentId").value(10))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].referenceCode").value("DH1"));

        verify(service).getPaymentsByBookingId(1L, 42L, "CUSTOMER");
    }
}

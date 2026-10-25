package com.ttcsn.payment.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}

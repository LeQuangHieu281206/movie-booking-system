package com.ttcsn.payment.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.common.event.PaymentCompletedEvent;
import com.ttcsn.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest {

    @Test
    void createPaymentUsesHeaderUserAndIgnoresClientUserAndAmount() throws Exception {
        PaymentService service = mock(PaymentService.class);
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                UUID.randomUUID(), 1L, 42L, new BigDecimal("100000"),
                OffsetDateTime.now(), PaymentCompletedEvent.STATUS_SUCCESS);
        when(service.processPayment(1L, 42L, false)).thenReturn(event);

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
                .andExpect(status().isCreated());

        verify(service).processPayment(1L, 42L, false);
    }
}

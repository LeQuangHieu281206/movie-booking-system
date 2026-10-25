package com.ttcsn.booking.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttcsn.booking.dto.request.BookingCreateRequest;
import com.ttcsn.booking.dto.response.BookingResponse;
import com.ttcsn.booking.entity.enums.BookingStatus;
import com.ttcsn.booking.entity.enums.PaymentMethod;
import com.ttcsn.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerTest {

    @Test
    void createBookingIgnoresClientUserIdAndTotalAmount() throws Exception {
        BookingService service = mock(BookingService.class);
        BookingResponse response = new BookingResponse(
                99L, 42L, 10L, "BK-TEST", new BigDecimal("100000"),
                BookingStatus.PENDING, PaymentMethod.CASH, OffsetDateTime.now());
        when(service.createBooking(any(BookingCreateRequest.class), eq(42L))).thenReturn(response);

        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new BookingController(service))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        mvc.perform(post("/api/createBooking")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 999,
                                  "totalAmount": 1,
                                  "holdBy": "gia-mao",
                                  "showtimeId": 10,
                                  "seatIds": [100, 101],
                                  "paymentMethod": "CASH"
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<BookingCreateRequest> captor = ArgumentCaptor.forClass(BookingCreateRequest.class);
        verify(service).createBooking(captor.capture(), eq(42L));
        assertThat(captor.getValue().showtimeId()).isEqualTo(10L);
        assertThat(captor.getValue().seatIds()).containsExactly(100L, 101L);
        assertThat(captor.getValue().paymentMethod()).isEqualTo(PaymentMethod.CASH);
        // Record không có userId/totalAmount/holdBy -> các field giả trong JSON bị bỏ qua.
        assertThat(List.of(BookingCreateRequest.class.getRecordComponents()))
                .extracting(component -> component.getName())
                .doesNotContain("userId", "totalAmount", "holdBy");
    }
}

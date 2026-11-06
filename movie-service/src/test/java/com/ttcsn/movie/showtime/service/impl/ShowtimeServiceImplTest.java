/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.showtime.service.impl;

import com.ttcsn.movie.client.VenueClient;
import com.ttcsn.movie.repository.MovieRepository;
import com.ttcsn.movie.showtime.dto.response.ShowtimeSeatView;
import com.ttcsn.movie.showtime.entity.ShowtimeSeat;
import com.ttcsn.movie.showtime.entity.enums.SeatStatus;
import com.ttcsn.movie.showtime.repository.ShowtimeRepository;
import com.ttcsn.movie.showtime.repository.ShowtimeSeatRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
/**
 *
 * @author quanghieu
 */
@ExtendWith(MockitoExtension.class)
class ShowtimeServiceImplTest {

    @Mock
    private ShowtimeRepository showtimeRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private ShowtimeSeatRepository showtimeSeatRepository;
    @Mock
    private VenueClient venueClient;
    @Mock
    private EntityManager em;
    @Mock
    private Query query;

    private ShowtimeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ShowtimeServiceImpl(
                showtimeRepository, movieRepository, showtimeSeatRepository, venueClient);
        ReflectionTestUtils.setField(service, "em", em);
    }

    @Test
    void releaseHidesHolderFromPublicView() throws Exception {
        ShowtimeSeatView view = new ShowtimeSeatView(
                5L, 10L, 100L, "A", (short) 1, "VIP", SeatStatus.HOLDING.name(),
                OffsetDateTime.now().plusMinutes(10));

        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(view);

        assertThat(json).doesNotContain("holdBy");
    }

    @Test
    void otherCustomerCannotReleaseHeldSeat() {
        when(showtimeSeatRepository.findByShowtime_ShowtimeIdAndSeatId(10L, 100L))
                .thenReturn(Optional.of(heldBy("2")));

        assertThrows(com.ttcsn.common.exception.ConflictException.class,
                () -> service.releaseShowtimeSeat(10L, 100L, 1L, false));

        verify(em, never()).createNativeQuery(anyString());
    }

    @Test
    void holderCanReleaseOwnHeldSeat() {
        when(showtimeSeatRepository.findByShowtime_ShowtimeIdAndSeatId(10L, 100L))
                .thenReturn(Optional.of(heldBy("1")));
        stubDelete(1L);

        ShowtimeSeatView view = service.releaseShowtimeSeat(10L, 100L, 1L, false);

        assertThat(view.status()).isEqualTo(SeatStatus.AVAILABLE.name());
    }

    @Test
    void adminCanReleaseAnotherUsersHeldSeat() {
        stubDelete(1L);

        ShowtimeSeatView view = service.releaseShowtimeSeat(10L, 100L, 1L, true);

        assertThat(view.status()).isEqualTo(SeatStatus.AVAILABLE.name());
    }

    @Test
    void holdMarksSeatHoldingForCaller() {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(5L));

        ShowtimeSeatView view = service.holdShowtimeSeat(10L, 100L, 7L);

        assertThat(view.status()).isEqualTo(SeatStatus.HOLDING.name());
        assertThat(view.holdUntil()).isAfter(OffsetDateTime.now());
    }

    private void stubDelete(long count) {
        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getSingleResult()).thenReturn(count);
    }

    private static ShowtimeSeat heldBy(String userId) {
        return ShowtimeSeat.builder()
                .showtimeSeatId(5L)
                .seatId(100L)
                .status(SeatStatus.HOLDING)
                .holdBy(userId)
                .holdUntil(OffsetDateTime.now().plusMinutes(10))
                .build();
    }
}
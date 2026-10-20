/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.venue.service.impl;

import com.ttcsn.venue.dto.request.CinemaRequest;
import com.ttcsn.venue.dto.response.CinemaResponse;
import com.ttcsn.venue.entity.Cinema;
import com.ttcsn.venue.exception.CinemaNotFoundException;
import com.ttcsn.venue.repository.CinemaRepository;
import com.ttcsn.venue.service.CinemaService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@Service
@RequiredArgsConstructor
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;

    @Override
    public List<CinemaResponse> getAllCinemas() {
        return cinemaRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CinemaResponse getCinemaById(Long cinemaId) {
        Cinema cinema = findCinemaOrThrow(cinemaId);
        return toResponse(cinema);
    }

    @Override
    public List<String> getAllCities() {
        return cinemaRepository.findDistinctCities();
    }

    @Override
    public List<CinemaResponse> getCinemasByCity(String city) {
        return cinemaRepository.findByCityIgnoreCase(city)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaRequest request) {
        Cinema cinema = new Cinema();
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setCity(request.city());
        cinema.setPhoneNumber(request.phoneNumber());
        return toResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public CinemaResponse updateCinema(Long cinemaId, CinemaRequest request) {
        Cinema cinema = findCinemaOrThrow(cinemaId);
        cinema.setName(request.name());
        cinema.setAddress(request.address());
        cinema.setCity(request.city());
        cinema.setPhoneNumber(request.phoneNumber());
        cinema.setUpdatedAt(java.time.OffsetDateTime.now());
        return toResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public void deleteCinema(Long cinemaId) {
        Cinema cinema = findCinemaOrThrow(cinemaId);
        cinemaRepository.delete(cinema);
    }

    private Cinema findCinemaOrThrow(Long cinemaId) {
        return cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new CinemaNotFoundException(cinemaId));
    }

    private CinemaResponse toResponse(Cinema cinema) {
        return new CinemaResponse(
                cinema.getCinemaId(),
                cinema.getName(),
                cinema.getAddress(),
                cinema.getCity(),
                cinema.getPhoneNumber(),
                cinema.getCreatedAt(),
                cinema.getUpdatedAt()
        );
    }
}
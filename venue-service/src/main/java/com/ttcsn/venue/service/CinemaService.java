/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.venue.service;

import com.ttcsn.venue.dto.request.CinemaRequest;
import com.ttcsn.venue.dto.response.CinemaResponse;

import java.util.List;
/**
 *
 * @author quanghieu
 */
public interface CinemaService {

    List<CinemaResponse> getAllCinemas();

    CinemaResponse getCinemaById(Long cinemaId);

    List<String> getAllCities();

    List<CinemaResponse> getCinemasByCity(String city);

    CinemaResponse createCinema(CinemaRequest request);

    CinemaResponse updateCinema(Long cinemaId, CinemaRequest request);

    void deleteCinema(Long cinemaId);
}
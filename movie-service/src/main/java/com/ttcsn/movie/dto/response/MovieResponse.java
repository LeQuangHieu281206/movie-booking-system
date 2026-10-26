/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.movie.dto.response;

import com.ttcsn.movie.entity.enums.AgeRestriction;
import com.ttcsn.movie.entity.enums.MovieStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
/**
 *
 * @author quanghieu
 */
public record MovieResponse(
        Long movieId,
        String title,
        String description,
        Short durationMinutes,
        String language,
        LocalDate releaseDate,
        String posterUrl,
        String trailerUrl,
        AgeRestriction ageRestriction,
        MovieStatus status,
        String director,
        String actors,
        List<GenreResponse> genres,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

}
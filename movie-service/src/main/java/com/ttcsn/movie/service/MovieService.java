/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.movie.service;

import com.ttcsn.movie.dto.request.MovieRequest;
import com.ttcsn.movie.dto.response.MovieResponse;
import com.ttcsn.movie.entity.enums.MovieStatus;

import java.util.List;
/**
 *
 * @author quanghieu
 */
public interface MovieService {

    MovieResponse getMovieById(Long movieId);

    List<MovieResponse> getAllMovies();

    MovieResponse createMovie(MovieRequest request);

    MovieResponse updateMovie(Long movieId, MovieRequest request);

    void deleteMovie(Long movieId);

    List<MovieResponse> searchMovies(MovieStatus status, List<Long> genreIds, String title);

    List<MovieResponse> searchByKeyword(String keyword, int limit);
}
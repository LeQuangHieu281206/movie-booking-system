/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.controller;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ttcsn.movie.dto.request.GenreRequest;
import com.ttcsn.movie.dto.response.GenreResponse;
import com.ttcsn.movie.service.GenreService;

import java.util.List;
/**
 *
 * @author quanghieu
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GenreController {

    private final GenreService genreService;

    @GetMapping("/getGenres")
    public ResponseEntity<List<GenreResponse>> getAllGenres() {
        return ResponseEntity.ok(genreService.getAllGenres());
    }

    @GetMapping("/getGenreById")
    public ResponseEntity<GenreResponse> getGenreById(@RequestParam Long id) {
        return ResponseEntity.ok(genreService.getGenreById(id));
    }

    @PostMapping("/createGenre")
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreRequest request) {
        GenreResponse response = genreService.createGenre(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/updateGenre")
    public ResponseEntity<GenreResponse> updateGenre(
            @RequestParam Long id,
            @Valid @RequestBody GenreRequest request) {
        return ResponseEntity.ok(genreService.updateGenre(id, request));
    }

    @DeleteMapping("/deleteGenre")
    public ResponseEntity<Void> deleteGenre(@RequestParam Long id) {
        genreService.deleteGenre(id);
        return ResponseEntity.noContent().build();
    }
}
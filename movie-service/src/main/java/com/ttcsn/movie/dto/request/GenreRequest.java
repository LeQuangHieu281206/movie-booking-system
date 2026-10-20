/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Record.java to edit this template
 */
package com.ttcsn.movie.dto.request;

import jakarta.validation.constraints.NotBlank;
/**
 *
 * @author quanghieu
 */
public record GenreRequest(
        @NotBlank(message = "Name must not be blank")
        String name
) {
}
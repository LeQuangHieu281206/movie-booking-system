/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.exception;

import com.ttcsn.common.exception.NotFoundException;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
public class MovieNotFoundException extends NotFoundException {
    public MovieNotFoundException(Long movieId) {
        super("Không tìm thấy phim với movieId = " + movieId);
    }
    public MovieNotFoundException(String message) {
        super(message);
    }
}
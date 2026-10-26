/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.movie.exception;

import com.ttcsn.common.exception.NotFoundException;
/**
 *
 * @author quanghieu
 */
public class GenreNotFoundException extends NotFoundException {
    public GenreNotFoundException(Long genreId) {
        super("Không tìm thấy thể loại với genreId = " + genreId);
    }
    public GenreNotFoundException(String message) {
        super(message);
    }
}
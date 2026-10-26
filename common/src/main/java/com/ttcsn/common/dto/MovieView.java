/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ttcsn.common.dto;
/**
 *
 * @author quanghieu
 */
public record MovieView(
        Long movieId,
        String title,
        String language,
        String ageRestriction,
        String status,
        String director
) {
}
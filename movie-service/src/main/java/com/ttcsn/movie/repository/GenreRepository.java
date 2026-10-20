/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.ttcsn.movie.repository;

import com.ttcsn.movie.entity.Genre;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
/**
 *
 * @author quanghieu
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    boolean existsByName(String name);
}
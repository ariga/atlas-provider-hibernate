package org.example;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "Movies")
public class Movie {

    Movie(String title, Integer numberInSeries) {
        this.title = title;
        this.numberInSeries = numberInSeries;
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String title;

    public Integer numberInSeries;
}

package org.example.minimodel;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static jakarta.persistence.GenerationType.IDENTITY;

@Entity
public class Location {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
}

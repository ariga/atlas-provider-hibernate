package com.example.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import javax.annotation.processing.Generated

@Entity
open class LocationDefaultGenerationType {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    open var id: Long = 0
}
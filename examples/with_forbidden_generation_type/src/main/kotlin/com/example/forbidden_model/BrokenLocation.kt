package com.example.forbidden_model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
open class LocationSequenceGenerationType {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    open var id: Long = 0
}
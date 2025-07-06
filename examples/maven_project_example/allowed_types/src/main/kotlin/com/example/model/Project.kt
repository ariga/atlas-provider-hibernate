package com.example.model

import jakarta.persistence.*

@Entity
@Table(name = "project")
class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0

    @Column(name = "name", nullable = false)
    var name: String = ""

    @Column(name = "description")
    var description: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    var department: Department? = null

    constructor()

    constructor(name: String, description: String?, department: Department?) {
        this.name = name
        this.description = description
        this.department = department
    }
}

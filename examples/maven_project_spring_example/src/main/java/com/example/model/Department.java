package com.example.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    @OneToMany(mappedBy="department",cascade= CascadeType.PERSIST)
    private List<Employee> employees = new ArrayList<Employee>();

    public Department() {
        super();
    }
    
    public Department(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
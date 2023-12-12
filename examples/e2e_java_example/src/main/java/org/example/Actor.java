package org.example;

import jakarta.persistence.*;

import java.util.List;
import java.util.Set;

@Entity
@Table(name = "Actors")
public class Actor {
    Actor() {

    }
    Actor(String name) {
        this.name = name;
    }

    @Id
    public String name;

    @OneToMany(mappedBy = "actor", cascade = CascadeType.PERSIST)
    public Set<MovieParticipation> actedIn;
}

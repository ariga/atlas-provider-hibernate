package org.example;

import jakarta.persistence.*;

@Entity
public class MovieParticipation {
    MovieParticipation(Movie movie, Actor actor) {
        this.key = new MovieParticipationKey(movie.id, actor.name);
        this.actor = actor;
        this.movie = movie;
    }

    @EmbeddedId
    public MovieParticipationKey key;

    @ManyToOne(cascade=CascadeType.PERSIST)
    @MapsId("movieId")
    @JoinColumn(name = "movieId")
    Movie movie;

    @ManyToOne(cascade=CascadeType.PERSIST)
    @MapsId("actorName")
    @JoinColumn(name = "actorName")
    Actor actor;
}

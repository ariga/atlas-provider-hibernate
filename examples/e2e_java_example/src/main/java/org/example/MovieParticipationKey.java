package org.example;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public class MovieParticipationKey implements Serializable {
    public MovieParticipationKey(Long movieId, String actorName) {
        this.movieId = movieId;
        this.actorName = actorName;
    }
    public Long movieId;

    public String actorName;
}
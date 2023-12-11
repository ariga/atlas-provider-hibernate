package org.example;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        SessionFactory sessionFactory = new MetadataSources()
                .addAnnotatedClass(Movie.class)
                .addAnnotatedClass(Actor.class)
                .addAnnotatedClass(MovieParticipation.class)
                .buildMetadata()
                .buildSessionFactory();

        sessionFactory.inTransaction(session -> {
            Movie matrix = new Movie();
            matrix.title = "The Matrix";
            Actor keanuReeves = new Actor("Keanu Reeves");
            keanuReeves.actedIn = Set.of(new MovieParticipation(matrix, keanuReeves));
            session.persist(keanuReeves);
        });
    }
}
package org.example;

import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;

import java.util.List;
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
            List<Actor> actors = session.createQuery("from Actor").list();
            if (!actors.isEmpty()) {
                actors.forEach(x -> System.out.println("Found Actor " + x.name));
            } else {
                Movie matrix = new Movie("The Matrix", 1);
                Actor keanuReeves = new Actor("Keanu Reeves");
                keanuReeves.actedIn = Set.of(new MovieParticipation(matrix, keanuReeves));
                session.persist(keanuReeves);
            }
        });
    }
}
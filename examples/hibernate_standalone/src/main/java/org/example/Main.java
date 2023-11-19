package org.example;

import org.example.minimodel.Event;
import org.example.minimodel.Location;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;

public class Main {
    public static void main(String[] args) {
        Metadata metadata = new MetadataSources()
                .addAnnotatedClasses(Event.class)
                .addAnnotatedClasses(Location.class)
                .buildMetadata();
        Object o = new MetadataBuilderImpl();
        System.out.println("Hello world " + o);
    }
}

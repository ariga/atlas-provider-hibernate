package org.example;

import org.example.minimodel.Event;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class MetadataBuilderImpl implements Function<ServiceRegistry, Metadata> {
    @Override
    @NotNull
    public Metadata apply(@NotNull ServiceRegistry registry) {
        return new MetadataSources(registry)
                .addAnnotatedClasses(Event.class)
                .buildMetadata();
    }
}

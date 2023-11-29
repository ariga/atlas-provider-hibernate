package com.example;

import com.example.minimodel.Location;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.service.ServiceRegistry;

import java.util.function.Function;

public class MetadataBuilder implements Function<ServiceRegistry, Metadata> {
    @Override
    public Metadata apply(ServiceRegistry registry) {
        return new MetadataSources(registry)
            .addAnnotatedClasses(Location.class)
            .buildMetadata();
    }
}

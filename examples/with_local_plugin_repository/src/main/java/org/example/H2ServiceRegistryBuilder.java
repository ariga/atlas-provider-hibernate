package org.example;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.service.ServiceRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

public class H2ServiceRegistryBuilder implements Function<Properties, ServiceRegistry> {
    public H2ServiceRegistryBuilder() {

    }

    @Override
    public ServiceRegistry apply(Properties properties) {
        return new StandardServiceRegistryBuilder()
                .applySettings(Map.of(
                    "hibernate.connection.url","jdbc:h2:mem:testdb",
                    AvailableSettings.SCHEMA_MANAGEMENT_TOOL, properties.get(AvailableSettings.SCHEMA_MANAGEMENT_TOOL),
                    "hibernate.temp.use_jdbc_metadata_defaults", true,
                    "jakarta.persistence.database-product-name", "H2",
                    "jakarta.persistence.database-major-version", "",
                    "hibernate.connection.provider_class", ""))
                .build();
    }
}
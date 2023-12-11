package com.example;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(value = {"classpath:schema-export.properties"})
public class Main {
    public static void main(String[] args) {
        new AnnotationConfigApplicationContext(Main.class);
    }
}

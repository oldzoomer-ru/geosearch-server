package ru.oldzoomer.geosearch.server;

import org.springframework.boot.SpringApplication;

public class TestGeosearchServerApplication {

    static void main(String[] args) {
        SpringApplication.from(GeosearchServerApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}

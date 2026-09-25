package com.packsmart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PackSmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(PackSmartApplication.class, args);
    }
}

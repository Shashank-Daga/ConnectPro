package com.connectpro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ConnectproBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConnectproBackendApplication.class, args);
    }

}

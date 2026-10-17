package com.example;

import javafx.application.Application;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AuthorityApplication {

    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(AuthorityApplication.class, args);
    }


}

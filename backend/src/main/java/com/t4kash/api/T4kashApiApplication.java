package com.t4kash.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Sin usuario en memoria por defecto: la autenticacion es por token de sesion.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class T4kashApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(T4kashApiApplication.class, args);
    }
}

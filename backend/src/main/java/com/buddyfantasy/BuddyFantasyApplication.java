package com.buddyfantasy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BuddyFantasyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuddyFantasyApplication.class, args);
    }
}

package net.disc0.sonshine_inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SonshineInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SonshineInventoryApplication.class, args);
    }

}

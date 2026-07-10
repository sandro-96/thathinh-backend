package vn.thathinh;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableMongoAuditing(auditorAwareRef = "auditorAware")
@EnableScheduling
@SpringBootApplication
@EnableCaching
public class ThathinhApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThathinhApplication.class, args);
    }
}

package jp.readscape.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "jp.readscape.consumer.domain")
@EnableJpaRepositories(basePackages = "jp.readscape.consumer.domain")
public class ConsumerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApiApplication.class, args);
    }
}
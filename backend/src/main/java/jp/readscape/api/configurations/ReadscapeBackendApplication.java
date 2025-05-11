package jp.readscape.api.configurations;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "jp.readscape.api")
public class ReadscapeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReadscapeBackendApplication.class, args);
	}
}

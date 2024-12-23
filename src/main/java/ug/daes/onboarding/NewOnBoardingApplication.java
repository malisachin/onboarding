package ug.daes.onboarding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

//import springfox.documentation.swagger2.annotations.EnableSwagger2;

//@EnableSwagger2
@EnableAsync
@SpringBootApplication
@EnableScheduling
public class NewOnBoardingApplication {
	final static private Logger logger = LoggerFactory.getLogger(NewOnBoardingApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(NewOnBoardingApplication.class, args);
	}

}

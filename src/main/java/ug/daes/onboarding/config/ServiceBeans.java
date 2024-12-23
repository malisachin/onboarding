package ug.daes.onboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ug.daes.onboarding.exceptions.OnBoardingServiceException;
import ug.daes.onboarding.response.OnBoardingServiceResponse;

@Configuration
public class ServiceBeans {

    @Bean
    public OnBoardingServiceResponse onBoardingServiceResponse(){
        return new OnBoardingServiceResponse();
    }

    @Bean
    public OnBoardingServiceException onBoardingServiceException(){
        return new OnBoardingServiceException();
    }
}

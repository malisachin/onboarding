package ug.daes.onboarding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ug.daes.DAESService;
import ug.daes.PKICoreServiceException;
import ug.daes.Result;

@Configuration
public class SignatueServiceInitilizeConfig {

    @Bean
    public void signatueServiceInitilize() {
        try {
            Result result = DAESService.initPKINativeUtils();
            if (result.getStatus() == 0) {
                System.out.println(new String(result.getStatusMessage()));
            } else {
                System.out.println(new String(result.getResponse()));
            }
        } catch (PKICoreServiceException e) {
            e.printStackTrace();
        }
    }
}

package ug.daes.onboarding.service.impl;



import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.dto.ConsentDTO;

import ug.daes.onboarding.dto.SignedDataDto;
import ug.daes.onboarding.model.*;
import ug.daes.onboarding.repository.*;
import ug.daes.onboarding.service.iface.ConsentIface;
import ug.daes.onboarding.util.AppUtil;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class ConsentImpl implements ConsentIface {



    @Autowired
    ConsentRepoIface consentRepoIface;

    @Value("${signed.data.url}")
    String signedURL;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    SubscriberRepoIface subscriberRepoIface;


    @Autowired
    ConsentHistoryRepo consentHistoryRepo;

    @Autowired
    SubscriberConsentsRepo subscriberConsentsRepo;


    @Autowired
    MessageSource messageSource;

    @Value("${signed.required.by.user}")
    private boolean signRequired;





    @Override
    public ApiResponse signData(HttpHeaders httpHeaders) {
        try {
            String consentVersion;
            String subscriberMail = httpHeaders.getFirst("adminugpassemail");
            System.out.println(subscriberMail);
            String consentData = "I agreed to above Terms and conditions and Data privacy terms";
//            String singedData = "I agrred to above Terms and conditions and Data privacy terms";
            if(signRequired) {
                if (subscriberMail != null) {
                    String url = signedURL;
                    SignedDataDto signedDataDto = new SignedDataDto();
                    String base64 = Base64.getEncoder().encodeToString(consentData.getBytes());

                    signedDataDto.setDocumentType("CADES");
                    signedDataDto.setSubscriberUniqueId(subscriberMail);
                    signedDataDto.setDocData(base64);
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<Object> reqEntity1 = new HttpEntity<>(signedDataDto, headers);
                    ResponseEntity<ApiResponse> res1 = restTemplate.exchange(url, HttpMethod.POST, reqEntity1, ApiResponse.class);
                    if (!res1.getBody().isSuccess()) {
                        System.out.println(res1.getBody().getMessage());
                        return AppUtil.createApiResponse(false, "Something went wrong. Please try after sometime", res1.getBody().getMessage());
                    }
                    Subscriber subscriber = subscriberRepoIface.findByemailId(subscriberMail);
                    if(subscriber==null){
                        return AppUtil.createApiResponse(false,"No subscriber found by given Email",null);
                    }
                    ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
//                    SubscriberConsents subscriberConsents1= subscriberConsentsRepo.findSubscriberConsentBySuidAndConsentId();
                    SubscriberConsents subscriberConsents = new SubscriberConsents();
                    subscriberConsents.setConsentData(consentData);
                    subscriberConsents.setSignedConsentData(res1.getBody().getResult().toString());
                    subscriberConsents.setConsentId(consentHistory.getId());
//                    if(s)
                    subscriberConsents.setSuid(subscriber.getSubscriberUid());
                    subscriberConsents.setCreatedOn(AppUtil.getDate());
                    subscriberConsentsRepo.save(subscriberConsents);
                    return AppUtil.createApiResponse(true, "data Signed and saved in database successfully", null);
                }
                else {
                    return AppUtil.createApiResponse(false,"Email cannot be null or empty",null);
                }
            }
            else{
                if(subscriberMail==null){
                    return AppUtil.createApiResponse(false,"Subscriber not found with given mail",null);
                }
                    Subscriber subscriber = subscriberRepoIface.findByemailId(subscriberMail);
                    ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
                    SubscriberConsents consents= subscriberConsentsRepo.findSubscriberConsentBySuidAndConsentId(subscriber.getSubscriberUid(),consentHistory.getId());
                    if(consents!=null){
                        return AppUtil.createApiResponse(true,"Consent saved successfully",null);
                    }
                    SubscriberConsents subscriberConsents = new SubscriberConsents();
                    subscriberConsents.setConsentData(consentData);
//                    subscriberConsents.setSignedConsentData();
                    subscriberConsents.setConsentId(consentHistory.getId());
                    subscriberConsents.setSuid(subscriber.getSubscriberUid());
                    subscriberConsents.setCreatedOn(AppUtil.getDate());
                    subscriberConsentsRepo.save(subscriberConsents);
                }
                 return AppUtil.createApiResponse(true, "consent saved successfully", null);
        }catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
                | PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            return AppUtil.createApiResponse(false, "Database Exception Occurred", null);
        } catch (Exception e) {
            e.printStackTrace();
            return AppUtil.createApiResponse(false, "An Exception Occurred.Please Try Again Later", null);
        }
    }
}

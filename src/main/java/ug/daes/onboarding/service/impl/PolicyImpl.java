package ug.daes.onboarding.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.model.SubscriberDevice;
import ug.daes.onboarding.repository.SubscriberDeviceRepoIface;
import ug.daes.onboarding.service.iface.PolicyIface;
import ug.daes.onboarding.util.AppUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


@Service
public class PolicyImpl implements PolicyIface {

    @Autowired
    private SubscriberDeviceRepoIface subscriberDeviceRepoIface;
    
    @Autowired
	MessageSource messageSource;


    @Override
    public boolean checkPolicy(String date, String pattern, long policy) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
            LocalDateTime currTime= LocalDateTime.now();
            long noOfHours=dateTime.until(currTime, ChronoUnit.HOURS);
            return noOfHours >= policy;
        }catch (Exception e)
        {
            throw new RuntimeException("Error parsing date. Use format yyyy-MM-dd HH:mm:ss");
        }

    }

    @Override
    public String matchDeviceUid(String suid, String deviceUid) {

        try {
            SubscriberDevice subscriberDevice= subscriberDeviceRepoIface.findBydeviceUid(deviceUid);
            return subscriberDevice==null?null:subscriberDevice.getDeviceUid();
        }catch (Exception e)
        {
            throw new RuntimeException("Something went wrong.");
        }
    }

    @Override
    public ApiResponse  checkPolicyRange(String date, String pattern, long minLimit) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            LocalDateTime dateTime = LocalDateTime.parse(date, formatter);
            LocalDateTime currTime= LocalDateTime.now();
            long noOfHours=dateTime.until(currTime, ChronoUnit.HOURS);
           // boolean isMax= minLimit <= maxLimit;
            boolean isMin = noOfHours >= minLimit;
            if(!isMin) {
            	return AppUtil.createApiResponse(false,"less than min limit",isMin);
            }
           
//            if(!isMax && isMin){
//                return AppUtil.createApiResponse(false,"greater than max limit",isMax);  , long maxLimit
//            }
//            if(!isMin && isMax){
//                return AppUtil.createApiResponse(false,"less than min limit",isMin);
//            }
            return AppUtil.createApiResponse(true,"within range",noOfHours);
        }catch (Exception e)
        {
            throw new RuntimeException("Error parsing date. Use format yyyy-MM-dd HH:mm:ss");
        }
    }
}

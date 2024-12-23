package ug.daes.onboarding.controller;

import java.text.ParseException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.dto.DeviceInfo;
import ug.daes.onboarding.dto.MobileOTPDto;
import ug.daes.onboarding.service.iface.DeviceUpdateIface;
import ug.daes.onboarding.service.impl.SubscriberServiceImpl;
import ug.daes.onboarding.util.AppUtil;

@RestController
public class UpdateDeviceController {

    private static Logger logger = LoggerFactory.getLogger(SubscriberServiceImpl.class);

    /** The Constant CLASS. */
    final static String CLASS = "DeviceUpdateImpl";

    @Autowired
    DeviceUpdateIface deviceUpdateIface;
    
    @Autowired
    MessageSource messageSource;
    

    @PostMapping("/api/post/verify-new-device")
    public ApiResponse verifyNewDevice(HttpServletRequest request, @RequestBody MobileOTPDto subscriberDTO) throws ParseException {
        logger.info(CLASS + " verify new device req for {} ", subscriberDTO);

        String deviceId = request.getHeader("deviceId");
        String appVersion = request.getHeader("appVersion");
        String osVersion = request.getHeader("osVersion");
        logger.info(CLASS + " verifySubscriberDetails req for deviceId , appVersion , osVersion {},{},{}",request.getHeader("deviceId"),request.getHeader("appVersion"),request.getHeader("osVersion"));
        
        DeviceInfo deviceInfoObj = new DeviceInfo(deviceId, appVersion, osVersion);
        if (deviceInfoObj.getDeviceId() == null || deviceInfoObj.getOsVersion() == null || deviceInfoObj.getAppVersion() == null) {
        	return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.one.or.moredevice.info.is.missing", null, Locale.ENGLISH), null);
        } 
        subscriberDTO.setSubscriberEmail(subscriberDTO.getSubscriberEmail().toLowerCase());
        ApiResponse response = deviceUpdateIface.validateSubscriberAndDevice(deviceInfoObj, subscriberDTO);
        return response;
    }


    @PostMapping("/api/post/activate-new-device")
    public ApiResponse verifySubscriberDetails(HttpServletRequest request, @RequestBody MobileOTPDto mobileOTPDto)
    {
        try{
            String deviceId= request.getHeader("deviceId");
            String appVersion= request.getHeader("appVersion");
            String osVersion= request.getHeader("osVersion");
            logger.info(CLASS + " verifySubscriberDetails req for deviceId , appVersion , osVersion , mobileOTPDto {},{},{},{}",request.getHeader("deviceId"),request.getHeader("appVersion"),request.getHeader("osVersion"),mobileOTPDto);
            
            DeviceInfo deviceInfoObj= new DeviceInfo(deviceId,appVersion,osVersion);
            
            if(deviceInfoObj.getDeviceId()==null || deviceInfoObj.getOsVersion()==null || deviceInfoObj.getAppVersion()==null) {
            	return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.one.or.moredevice.info.is.missing", null, Locale.ENGLISH),null);
            }
            //verify subscriber exists with mobile number and email
            return deviceUpdateIface.activateNewDevice(deviceInfoObj,mobileOTPDto);

        }catch (Exception e){
            e.printStackTrace();
            return AppUtil.createApiResponse(false,e.getLocalizedMessage(),null);
        }

    }
}

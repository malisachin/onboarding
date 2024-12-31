package ug.daes.onboarding.controller;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;

import javax.mail.SendFailedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.dto.MobileOTPDto;
import ug.daes.onboarding.service.iface.OtpServiceIface;
import ug.daes.onboarding.util.AppUtil;

@RestController
@CrossOrigin
public class OTPController {

	private static Logger logger = LoggerFactory.getLogger(OTPController.class);

	/** The Constant CLASS. */
	final static String CLASS = "OTPController";

	
	@Autowired
	OtpServiceIface otpServiceIface;

	@Value(value = "${service.version}")
	private String version;
	
	@PostMapping("/api/post/register-subscriber")
	public ApiResponse sendOtpMobile(@RequestBody MobileOTPDto otpDto)
			throws NoSuchAlgorithmException, SendFailedException, ParseException, UnknownHostException {
		logger.info(CLASS + "sendOtpMobile req {} ",otpDto);
		otpDto.getSubscriberEmail().toLowerCase();
		return otpServiceIface.sendOTPMobileSms(otpDto);
	}


	@GetMapping("/api/get/onboarding/version")
	public ApiResponse getProjectVersion(){
		String msg = "OnBoarding service version: " + version;
		return AppUtil.createApiResponse(true,msg,null);
	}
	
}

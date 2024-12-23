package ug.daes.onboarding.util;


import java.util.Locale;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import ug.daes.onboarding.model.SubscriberDevice;
import ug.daes.onboarding.model.SubscriberDeviceHistory;
import ug.daes.onboarding.repository.SubscriberDeviceHistoryRepoIface;
import ug.daes.onboarding.repository.SubscriberDeviceRepoIface;


@Aspect
@Component
public class ExecutionTimeAspectPolicy {
	
	@Autowired
	MessageSource messageSource;

	@Autowired
    private SubscriberDeviceRepoIface subscriberDeviceRepoIface;

	@Autowired
	SubscriberDeviceHistoryRepoIface subscriberDeviceHistoryRepoIface;


	/** The Constant CLASS. */
	final static String CLASS = "ExecutionTimeAspect";

	@Pointcut("execution(* ug.daes.onboarding.controller.SubscriberController.saveSubscriberObData(..))")
	private void forsaveSubscriberObData() {
	};

	@Pointcut("execution(* ug.daes.onboarding.controller.SubscriberController.reOnboardAddSubscriberObData(..))")
	private void forreOnboardAddSubscriberObData() {
	};

	@Pointcut("execution(* ug.daes.onboarding.controller.SubscriberController.getSubscriberObData(..))")
	private void forgetSubscriberObData() {
	};

	@Pointcut("execution(* ug.daes.onboarding.controller.SubscriberController.resetPin(..))")
	private void forresetPin() {
	};

//    @Pointcut("execution(* ug.daes.onboarding.controller.TemplateController.getActviteTemplate(..))")
//    private void forgetActviteTemplate(){};

	@Pointcut("execution(* ug.daes.onboarding.controller.LogController.saveNiraApiLogs(..))")
	private void forLogController() {
	};

	@Pointcut("execution(* ug.daes.onboarding.controller.UpdateSubscriberController.updatePhoneNumber(..))")
	private void forUpdateSubscriberController() {
	};

	@Around("forsaveSubscriberObData() || forreOnboardAddSubscriberObData() || forgetSubscriberObData() || forresetPin() || forLogController() || forUpdateSubscriberController()")																																											// methods
	public Object controllerPolicy(ProceedingJoinPoint joinPoint) throws Throwable {
		return checkPolicy(joinPoint);
	}

	private Object checkPolicy(ProceedingJoinPoint joinPoint) throws Throwable {
		String methodName = joinPoint.getSignature().toShortString();

		//System.out.println("method name: " + methodName);
		String deviceUid = "";
		String appVersion = "";
		for (Object arg : joinPoint.getArgs()) {
			if (arg instanceof HttpServletRequest) {
				HttpServletRequest httpServletRequest = (HttpServletRequest) arg;
				
				deviceUid = httpServletRequest.getHeader("deviceId");
				appVersion = httpServletRequest.getHeader("appVersion");
				

				break;
			}
		}
		
		Optional<SubscriberDeviceHistory> subscriberDeviceHistoryOptional = Optional.ofNullable(subscriberDeviceHistoryRepoIface.findBydeviceUid(deviceUid));
		SubscriberDevice checkSubscriberDetails = null;
		SubscriberDevice subscriberDeviceDetails = subscriberDeviceRepoIface.findBydeviceUid(deviceUid);
		
		Object result;
		boolean checkPolicy = true;
		boolean deviceEmpty = false;
		if(deviceUid.equals("WEB")) {
			checkPolicy = true;
		}else {
			if(appVersion == null || appVersion.equals("") || appVersion == "") {
				System.out.println("appVersion is empty appVersion and deviceUid "+appVersion + "-- "+deviceUid);
				deviceEmpty = true;
				
			}else if (subscriberDeviceHistoryOptional.isPresent()) {
					
					//Optional<SubscriberDevice> subscriberDevice = Optional.ofNullable(subscriberDeviceRepoIface.findBydeviceUidAndStatus(deviceUid,"ACTIVE"));
					checkSubscriberDetails = subscriberDeviceRepoIface.getSubscriber(subscriberDeviceHistoryOptional.get().getSubscriberUid());
					SubscriberDevice subscriberDevice = subscriberDeviceRepoIface.findBydeviceUidAndStatus(deviceUid,"ACTIVE");
					if(subscriberDevice == null) {
						checkPolicy = false;
						
					}else if(subscriberDevice.getDeviceStatus() == "DISABLED" || subscriberDevice.getDeviceStatus().equalsIgnoreCase("DISABLED")) {
						checkPolicy = false;
						System.out.println("inside else if");
						result = AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.account.registered.on.new.device.services.disabled.on.this.device", null, Locale.ENGLISH),
								null);
					}else {
						checkPolicy = true;
					}
					
				}else if(subscriberDeviceDetails == null) {
					checkPolicy = false;
					
				}else if(subscriberDeviceDetails.getDeviceStatus() == "ACTIVE" || subscriberDeviceDetails.getDeviceStatus().equalsIgnoreCase("ACTIVE")) {
					checkPolicy = true;
					System.out.println("inside else if active");

				}else if(subscriberDeviceDetails.getDeviceStatus() == "DISABLED" || subscriberDeviceDetails.getDeviceStatus().equalsIgnoreCase("DISABLED")) {
					checkPolicy = false;
					System.out.println("inside else if DISABLED");
					
				}else {
					checkPolicy = false;
					System.out.println("inside else");
					result = AppUtil.createApiResponse(false,messageSource.getMessage("api.error.account.registered.on.new.device.services.disabled.on.this.device", null, Locale.ENGLISH),	null);
				}
		}
		
		if(deviceEmpty) {
			result = AppUtil.createApiResponse(false,messageSource.getMessage("api.error.please.update.your.app", null, Locale.ENGLISH),null);
		}else if (checkPolicy) {
			result = joinPoint.proceed();
		}else {
			
			//Account registered on new device, services temporarily disabled on this device.
			if(subscriberDeviceDetails == null && checkSubscriberDetails == null) {
				result = AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH),
						null);
			}else {
				result = AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.account.registered.on.new.device.services.disabled.on.this.device", null, Locale.ENGLISH),
						null);
			}
			

//			result = AppUtil.createApiResponse(false,"We apologize for any inconvenience.  You can use the service after " +remainHour+ " hours, as it seems you changed your Device.",null);
		}

		return result;
	}

}


package ug.daes.onboarding.service.impl;

import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.hibernate.PessimisticLockException;
import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.exception.JDBCConnectionException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.SQLGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ug.daes.DAESService;
import ug.daes.PKICoreServiceException;
import ug.daes.Result;
import ug.daes.onboarding.config.SentryClientExceptions;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.constant.Constant;
import ug.daes.onboarding.dto.*;
import ug.daes.onboarding.enums.LogMessageType;
import ug.daes.onboarding.enums.ServiceNames;
import ug.daes.onboarding.enums.TransactionType;
import ug.daes.onboarding.exceptions.OnBoardingServiceException;
import ug.daes.onboarding.model.*;
import ug.daes.onboarding.repository.*;
import ug.daes.onboarding.service.iface.DeviceUpdateIface;
import ug.daes.onboarding.service.iface.SubscriberServiceIface;
import ug.daes.onboarding.service.iface.TemplateServiceIface;
import ug.daes.onboarding.util.AppUtil;

@Primary
@Service
public class SubscriberServiceImpl implements SubscriberServiceIface {

	private static Logger logger = LoggerFactory.getLogger(SubscriberServiceImpl.class);

	/** The Constant CLASS. */
	final static String CLASS = "SubscriberServiceImpl";


	@Autowired
	SubscriberRepoIface subscriberRepoIface;

	@Autowired
	SubscriberDeviceRepoIface deviceRepoIface;

	@Autowired
	SubscriberDeviceHistoryRepoIface subscriberDeviceHistoryRepoIface;

	@Autowired
	SubscriberFcmTokenRepoIface fcmTokenRepoIface;

	@Autowired
	SubscriberOnboardingDataRepoIface onboardingDataRepoIface;

	@Autowired
	SubscriberStatusRepoIface statusRepoIface;

	@Autowired
	SubscriberRaDataRepoIface raRepoIface;

	@Autowired
	OnBoardingTemplateRepoIface onBoardingTemplateRepoIface;

	@Autowired
	SubscriberCertificatesRepoIface subscriberCertificatesRepoIface;

	@Autowired
	SubscriberCertPinHistoryRepoIface subscriberCertPinHistoryRepoIface;

	@Autowired
	OnboardingLivelinessRepository livelinessRepository;

	@Autowired
	TemplateServiceIface templateServiceIface;

	@Autowired
	MapMethodObStepRepoIface mapStepRepoIface;

	@Autowired
	TrustedUserRepoIface trustedUserRepoIface;

	@Autowired
	SubscriberCertificateDetailsRepoIface subscriberCertificateDetailsRepoIface;

	@Autowired
	SubscriberCompleteDetailRepoIface subscriberCompleteDetailRepoIface;

	@Autowired
	RabbitMQSender mqSender;

	@Autowired
	EdmsServiceImpl edmsService;

	@Autowired
	public RestTemplate restTemplate;

	@Autowired
	SubscriberViewRepoIface subscriberViewRepoIface;

	@Autowired
	SusbcriberDetailsViewRepo susbcriberDetailsRepository;
	@Autowired
	SubscriberConsentsRepo subscriberConsentsRepo;

	@Autowired
	ConsentHistoryRepo consentHistoryRepo;

	ObjectMapper objectMapper = new ObjectMapper();

	@Value(value = "${email.url}")
	private String emailBaseUrl;
	@Value(value = "${ind.api.sms}")
	private String indApiSMS;
	@Value(value = "${nira.api.sms}")
	private String niraApiSMS;

	@Value(value = "${nira.username}")
	private String niraUserName;

	@Value(value = "${nira.password}")
	private String niraPassword;
	@Value(value = "${nira.api.token}")
	private String niraApiToken;

	@Value(value = "${uae.api.sms}")
	private String uaeApiSMS;

	@Value(value ="${registerface.url}")
	private String registerFaceURL;

//	@Value("${edms.username}")
//	private String username;
//
//	@Value("${edms.password}")
//	private String password;
//
//	@Value("${edms.baseurl}")
//	private String baseUrl;

	@Value("${ra.base.url}")
	private String raBaseUrl;

	@Value(value = "${nira.api.timetolive}")
	private int timeToLive;

	@Value("${au.log.url}")
	private String auditLogUrl;

	@Value("${config.validation.allowTrustedUsersOnly}")
	private String trustedUserStatus;

	// Re-onboard
	@Value("${re.onboard.dateofbirth}")
	private boolean checkDateOfBirth;

	@Value("${re.onboard.gender}")
	private boolean checkGender;

	@Value("${re.onboard.documentnumber}")
	private boolean checkDocumentNumber;

	@Value("${expiry.days}")
	private int expiryDays;

	@Value("${signed.required.by.user}")
	private boolean signRequired;


	@Value("${visitorCardUrl}")
	private String visitorCardUrl;

	@Autowired
	LogModelServiceImpl logModelServiceImpl;

	@Autowired
	SubscriberHistoryRepo subscriberHistoryRepo;

	@Autowired
	OnBoardingMethodRepoIface onBoardingMethodRepoIface;

	@Autowired
	OrgContactsEmailRepository orgContactsEmailRepository;

	@Autowired
	DeviceUpdateIface deviceUpdateIface;

	@Autowired
	MessageSource messageSource;

	@Autowired
	SentryClientExceptions sentryClientExceptions;


	public String generateSubscriberUniqueId() {
		UUID uuid = UUID.randomUUID();
		logger.info(CLASS + "Generate Subscriber UniqueId {}", uuid.toString());
		return uuid.toString();
	}

	private String encryptedString(String s) {
		try {
			// System.out.println("s => " + s);
			Result result = DAESService.encryptData(s);
			return new String(result.getResponse());
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@SuppressWarnings("unused")
	@Override
	public ApiResponse saveSubscribersData(MobileOTPDto subscriberDTO) throws ParseException, UnknownHostException {
		if (subscriberDTO.getOsName() == null || subscriberDTO.getAppVersion() == null
				|| subscriberDTO.getOsVersion() == null || subscriberDTO.getDeviceInfo() == null) {
			return AppUtil.createApiResponse(false,
					messageSource.getMessage("api.error.application.info.not.found", null, Locale.ENGLISH), null);
		}
		Date startTime = new Date();
		String OtpReqTime = AppUtil.getTimeStamping();
//		String URI = auditLogUrl;
//		logger.info(CLASS + " saveSubscriberData auditLogUrl {} ", URI);
//		OtpDTO dto = new OtpDTO();
//		String email = encryptedString(subscriberDTO.getSubscriberEmail());
//		dto.setIdentifier(email);
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		HttpEntity<OtpDTO> requestEntity = new HttpEntity<>(dto, headers);
//		ResponseEntity<String> correlation = restTemplate.exchange(URI, HttpMethod.POST, requestEntity, String.class);
//		ObjectMapper objectMapper = new ObjectMapper();
//		AuditDTO auditDTO = null;
//		String correlationId = null;
//		try {
//			if (correlation.getStatusCode() != HttpStatus.NO_CONTENT || correlation != null) {
//				auditDTO = objectMapper.readValue(correlation.getBody(), AuditDTO.class);
//			}
//		} catch (Exception e) {
//			logger.error(CLASS + " saveSubscriberData Exception in AuditLogUrl {} ", e.getMessage());
//			System.out.println(e.getMessage());
//		}
//		if (auditDTO != null) {
//			correlationId = auditDTO.getCorrelationID();
//		}
		Subscriber subscriber = new Subscriber();
		SubscriberDevice subscriberDevice = new SubscriberDevice();
		SubscriberFcmToken fcmToken = new SubscriberFcmToken();
		SubscriberStatus subscriberStatus = new SubscriberStatus();

		SubscriberRegisterResponseDTO responseDTO = new SubscriberRegisterResponseDTO();

		if (!subscriberDTO.getOtpStatus()) {
			return AppUtil.createApiResponse(false,
					messageSource.getMessage("api.error.otp.not.verified", null, Locale.ENGLISH), null);
		}
		ApiResponse response = checkValidationForSubscriber(subscriberDTO);
		// ApiResponse response = checkValidationForSubscriberNew(subscriberDTO);
		System.out.println(" checkValidationForSubscriber >> " + response);
		logger.info(CLASS + " saveSubscriberData res for checkValidationForSubscriber {}", response);
		if (!response.isSuccess() && response.getResult() != null) {
			response.setSuccess(true);
			return response;
		}
		if (!response.isSuccess() && response.getResult() == null) {
			return response;
		}

		try {
			String suid = generateSubscriberUniqueId();
			logger.info(CLASS + "saveSubscriberData req for suid {}", suid);
			if (subscriberDTO != null) {
				Subscriber previousSuid = subscriberRepoIface.getSubscriberUidByEmailAndMobile(
						subscriberDTO.getSubscriberEmail(), subscriberDTO.getSubscriberMobileNumber());
				if (previousSuid != null) {
					SubscriberFcmToken preSubscriberFcmToken = fcmTokenRepoIface
							.findBysubscriberUid(previousSuid.getSubscriberUid());
					SubscriberStatus preSubscriberStatus = statusRepoIface
							.findBysubscriberUid(previousSuid.getSubscriberUid());
					SubscriberDevice preSubscriberDevice = deviceRepoIface
							.getSubscriber(previousSuid.getSubscriberUid());

					if (preSubscriberDevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {

						SubscriberDeviceHistory subscriberDeviceHistory = new SubscriberDeviceHistory();
						subscriberDeviceHistory.setSubscriberUid(previousSuid.getSubscriberUid());
						subscriberDeviceHistory.setDeviceUid(preSubscriberDevice.getDeviceUid());
						subscriberDeviceHistory.setDeviceStatus(Constant.DEVICE_STATUS_DISABLED);
						subscriberDeviceHistory.setCreatedDate(AppUtil.getDate());
						subscriberDeviceHistory.setUpdatedDate(AppUtil.getDate());
						subscriberDeviceHistoryRepoIface.save(subscriberDeviceHistory);

					}

					System.out.println("previousSuid :: " + previousSuid);
					subscriber.setSubscriberId(previousSuid.getSubscriberId());
					subscriber.setSubscriberUid(previousSuid.getSubscriberUid());

					subscriberDevice.setSubscriberDeviceId(preSubscriberDevice.getSubscriberDeviceId());
					subscriberDevice.setSubscriberUid(previousSuid.getSubscriberUid());

					fcmToken.setSubscriberFcmTokenId(preSubscriberFcmToken.getSubscriberFcmTokenId());
					fcmToken.setSubscriberUid(previousSuid.getSubscriberUid());
					subscriberStatus.setSubscriberStatusId(preSubscriberStatus.getSubscriberStatusId());
					subscriberStatus.setSubscriberUid(previousSuid.getSubscriberUid());
					responseDTO.setSuID(previousSuid.getSubscriberUid());

				} else {
					subscriber.setSubscriberUid(suid);
					subscriberDevice.setSubscriberUid(suid);
					fcmToken.setSubscriberUid(suid);
					subscriberStatus.setSubscriberUid(suid);
					responseDTO.setSuID(suid);
				}

				subscriber.setCreatedDate(AppUtil.getDate());
				subscriber.setUpdatedDate(AppUtil.getDate());
				subscriber.setEmailId(subscriberDTO.getSubscriberEmail().toLowerCase());
				subscriber.setMobileNumber(subscriberDTO.getSubscriberMobileNumber());
				subscriber.setFullName(subscriberDTO.getSubscriberName());
				subscriber.setOsName(subscriberDTO.getOsName());
				subscriber.setOsVersion(subscriberDTO.getOsVersion());
				subscriber.setDeviceInfo(subscriberDTO.getDeviceInfo());
				subscriber.setAppVersion(subscriberDTO.getAppVersion());

				// subscriber.setSmartPhoneUser(true);

				subscriberDevice.setCreatedDate(AppUtil.getDate());
				subscriberDevice.setUpdatedDate(AppUtil.getDate());
				subscriberDevice.setDeviceUid(subscriberDTO.getDeviceId());
				subscriberDevice.setDeviceStatus(Constant.DEVICE_STATUS_ACTIVE);

				fcmToken.setCreatedDate(AppUtil.getDate());
				fcmToken.setFcmToken(subscriberDTO.getFcmToken());

				subscriberStatus.setOtpVerifiedStatus(Constant.OTP_VERIFIED_STATUS);
				subscriberStatus.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
				subscriberStatus.setCreatedDate(AppUtil.getDate());
				subscriberStatus.setUpdatedDate(AppUtil.getDate());

				subscriber = subscriberRepoIface.save(subscriber);

				if (previousSuid != null) {
//					deviceRepoIface.insertSubscriber(previousSuid.getSubscriberId(),previousSuid.getSubscriberUid(), subscriberDTO.getDeviceId(),
//							"ACTIVE", AppUtil.getDate(), AppUtil.getDate());

					SubscriberDevice device = deviceRepoIface.getSubscriber(previousSuid.getSubscriberUid());

					System.out.println("old device  >> " + device.getSubscriberDeviceId());
					deviceRepoIface.updateSubscriber(subscriberDTO.getDeviceId(), "ACTIVE", AppUtil.getDate(),
							device.getSubscriberDeviceId());

					System.out.println("Old device updated with new deviceid and Status ");

				} else {
					subscriberDevice = deviceRepoIface.save(subscriberDevice);
				}
//				if (previousSuid != null) {
//					deviceRepoIface.insertSubscriber(previousSuid.getSubscriberUid(), subscriberDTO.getDeviceId(),
//							"ACTIVE", AppUtil.getDate(), AppUtil.getDate());
//				} else {
//					subscriberDevice = deviceRepoIface.save(subscriberDevice);
//				}

				if (previousSuid != null) {
					String firstTimeOnboarding = subscriberRepoIface
							.firstTimeOnboardingPaymentStatus(previousSuid.getSubscriberUid());
					if (firstTimeOnboarding != null) {
						responseDTO.setFirstTimeOnboarding(false);
					} else {
						responseDTO.setFirstTimeOnboarding(true);
					}
				} else {
					responseDTO.setFirstTimeOnboarding(true);
				}

				fcmToken = fcmTokenRepoIface.save(fcmToken);
				subscriberStatus = statusRepoIface.save(subscriberStatus);

				if (subscriber != null) {

					responseDTO.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
					Date endTime = new Date();

					double toatlTime = AppUtil.getDifferenceInSeconds(startTime, endTime);
					System.out.println("toatlTime :: " + toatlTime);

					logModelServiceImpl.setLogModel(true, subscriber.getSubscriberUid(), null,
							"SUBSCRIBER_REGISTRATION", subscriber.getSubscriberUid(), String.valueOf(toatlTime),
							startTime, endTime, null);
					logger.info(CLASS + " saveSubscriberData Subscriber Detail saved {}", responseDTO);
					if (!signRequired) {
						SubscriberConsents subscriberConsents = new SubscriberConsents();
						String consentData = "I agreed to above Terms and conditions and Data privacy terms";
						ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
						if (subscriberConsentsRepo.findSubscriberConsentBySuidAndConsentId(responseDTO.getSuID(),
								consentHistory.getId()) == null) {
							subscriberConsents.setCreatedOn(AppUtil.getDate());
							subscriberConsents.setConsentData(consentData);
							subscriberConsents.setSuid(responseDTO.getSuID());
							subscriberConsents.setConsentId(consentHistory.getId());
							subscriberConsentsRepo.save(subscriberConsents);
						}
					}
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.subscriber.email.and.mobile.number.is.verified",
									null, Locale.ENGLISH),
							responseDTO);
				} else {
					logModelServiceImpl.setLogModel(false, subscriber.getSubscriberUid(), null,
							"SUBSCRIBER_REGISTRATION", subscriber.getSubscriberUid(), null, null, null, null);
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.response.subscriber.email.and.mobile.number.is.not.verified",
									null, Locale.ENGLISH),
							null);
				}
			} else {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.empty.fields", null, Locale.ENGLISH), null);
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			sentryClientExceptions.captureTags(subscriberDTO.getSuID(), subscriberDTO.getSubscriberMobileNumber(),
					"saveSubscribersData", "SubscriberController");
			sentryClientExceptions.captureExceptions(ex);
			logger.error(CLASS + "saveSubscriberData Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			sentryClientExceptions.captureTags(subscriberDTO.getSuID(), subscriberDTO.getSubscriberMobileNumber(),
					"saveSubscribersData", "SubscriberController");
			sentryClientExceptions.captureExceptions(e);
			logger.error(CLASS + "saveSubscriberData Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public ApiResponse checkValidationForSubscriber(MobileOTPDto mobileOTPDto) throws UnknownHostException {
		logger.info(CLASS + " checkValidationForSubscriber request {}", mobileOTPDto);
		int countDevice;
		int countMobile;
		int countEmail;
		SubscriberFcmToken fcmToken = new SubscriberFcmToken();
		SubscriberRegisterResponseDTO responseDTO = new SubscriberRegisterResponseDTO();
		SubscriberDevice deviceDetails = null;
		SubscriberDevice subscriberDeviceDetails = null;
		Subscriber previousSuid = null;
		try {
			if (mobileOTPDto.getOtpStatus()) {
				countDevice = subscriberRepoIface.countSubscriberDevice(mobileOTPDto.getDeviceId());
				logger.info(CLASS + "checkValidationForSubscriber countDevice {}, DeviceId {} ", countDevice,
						mobileOTPDto.getDeviceId());
				countMobile = subscriberRepoIface.countSubscriberMobile(mobileOTPDto.getSubscriberMobileNumber());
				logger.info(CLASS + "checkValidationForSubscriber countMobile {} , SubscriberMobileNumber {} ",
						countMobile, mobileOTPDto.getSubscriberMobileNumber());
				countEmail = subscriberRepoIface
						.countSubscriberEmailId(mobileOTPDto.getSubscriberEmail().toLowerCase());
				logger.info(CLASS + "checkValidationForSubscriber countEmail {}, SubscriberEmail {} ", countEmail,
						mobileOTPDto.getSubscriberEmail().toLowerCase());

				if (countEmail == 1 && countMobile == 1 && countDevice >= 1) {
					previousSuid = subscriberRepoIface.getSubscriberDetailsByEmailAndMobile(
							mobileOTPDto.getSubscriberEmail().toLowerCase(), mobileOTPDto.getSubscriberMobileNumber());
					if (previousSuid == null) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.this.mobile.no.is.already.register.with.different.email.id", null,
										Locale.ENGLISH),
								null);
					} else {
						SubscriberDevice subscriberDevice = deviceRepoIface
								.getSubscriber(previousSuid.getSubscriberUid());
						deviceDetails = deviceRepoIface.findBydeviceUidAndStatus(mobileOTPDto.getDeviceId(), "ACTIVE");
						if (subscriberDevice.getDeviceUid().equals(mobileOTPDto.getDeviceId())) {

							// if(deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)){
							if (deviceDetails != null) {
								if (!subscriberDevice.getSubscriberUid().equals(deviceDetails.getSubscriberUid())) {
									return AppUtil.createApiResponse(false, messageSource.getMessage(
											"api.error.this.device.is.already.register.with.differet.email.or.mobile.no",
											null, Locale.ENGLISH), null);
								} else {
									countDevice = 1;
								}
							} else {
								countDevice = 1;
							}

						} else if (deviceDetails != null) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.device.is.already.register.with.differet.email.or.mobile.no", null,
									Locale.ENGLISH), null);
						} else {
							subscriberDeviceDetails = deviceRepoIface.findDeviceDetailsById(mobileOTPDto.getDeviceId());
							SubscriberStatus subscriberStatus = statusRepoIface
									.findBysubscriberUid(subscriberDeviceDetails.getSubscriberUid());
							if (subscriberDeviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.this.device.is.already.register.with.differet.email.or.mobile.no",
										null, Locale.ENGLISH), null);
							} else {
								countDevice = 1;
							} // if(subscriberDeviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED))
								// {
								// countDevice =
								// subscriberRepoIface.countSubscriberDevice(mobileOTPDto.getDeviceId());
								// }
						}
					}

				}
				System.out.println(" countDevice = 0;  " + countDevice);
				if (countDevice == 1 && countMobile == 1 && countDevice == 0) {
					countEmail = 1;
				}

				if (countDevice == 1 && countMobile == 1 && countEmail == 1) {

					logger.info(CLASS
							+ " checkValidationForSubscriber countDevice == 1 && countMobile == 1 && countEmail == 1 ");
					// deviceDetails = deviceRepoIface.findBydeviceUid(mobileOTPDto.getDeviceId());
					deviceDetails = deviceRepoIface.getSubscriber(previousSuid.getSubscriberUid());

					if (deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)
							|| deviceDetails.getDeviceStatus() == Constant.DEVICE_STATUS_DISABLED) {
						System.out.println("checkValidationForSubscriber This Device is disabled ");
						// return
						// AppUtil.createApiResponse(false,messageSource.getMessage("api.error.this.device.is.disabled",
						// null, Locale.ENGLISH),null);
					}

					fcmToken = fcmTokenRepoIface.findBysubscriberUid(deviceDetails.getSubscriberUid());
					responseDTO.setSuID(deviceDetails.getSubscriberUid());
					fcmToken.setSubscriberUid(deviceDetails.getSubscriberUid());
					fcmToken.setCreatedDate(AppUtil.getDate());
					fcmToken.setFcmToken(mobileOTPDto.getFcmToken());

					deviceDetails.setDeviceUid(mobileOTPDto.getDeviceId());
					deviceDetails.setDeviceStatus(Constant.DEVICE_STATUS_ACTIVE);
					deviceDetails.setUpdatedDate(AppUtil.getDate());

					Subscriber subscriber = subscriberRepoIface.findBysubscriberUid(deviceDetails.getSubscriberUid());

					if (!subscriber.getEmailId().equals(mobileOTPDto.getSubscriberEmail().toLowerCase())
							|| !subscriber.getMobileNumber().equals(mobileOTPDto.getSubscriberMobileNumber())) {
						logger.info(CLASS
								+ "checkValidationForSubscriber This Device is already register with differet Email or Mobile No.");
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.this.device.is.already.register.with.differet.email.or.mobile.no",
										null, Locale.ENGLISH),
								null);
					}
					SubscriberStatus subscriberStatus = statusRepoIface
							.findBysubscriberUid(deviceDetails.getSubscriberUid());
					if (subscriberStatus != null) {
						if (subscriberStatus.getSubscriberStatus() == Constant.SUBSCRIBER_STATUS
								|| subscriberStatus.getSubscriberStatus().equals(Constant.SUBSCRIBER_STATUS)) {
							responseDTO.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
						} else {
							responseDTO.setSubscriberStatus(subscriberStatus.getSubscriberStatus());
						}
					} else {
						logger.info(CLASS
								+ " checkValidationForSubscriber This Device is Already Registered. Please Continue");
						if (!signRequired) {
							String consentData = "I agreed to above Terms and conditions and Data privacy terms";
							ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
							SubscriberConsents subscriberConsents = subscriberConsentsRepo
									.findSubscriberConsentBySuidAndConsentId(deviceDetails.getSubscriberUid(),
											consentHistory.getId());
							if (subscriberConsents == null) {
								SubscriberConsents subscriberConsents1 = new SubscriberConsents();
								subscriberConsents1.setConsentData(consentData);
								subscriberConsents1.setSuid(deviceDetails.getSubscriberUid());
								subscriberConsents1.setCreatedOn(AppUtil.getDate());
								subscriberConsents1.setConsentId(consentHistory.getId());
								subscriberConsentsRepo.save(subscriberConsents1);
							}
						}

						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.this.device.is.already.registered.please.continue",
										null, Locale.ENGLISH),
								responseDTO);
					}
					if (!subscriberStatus.getSubscriberStatus().equals(Constant.SUBSCRIBER_STATUS)) {
						SubscriberOnboardingData subscriberOnboardingData = null;
						SubscriberDetails subscriberDetails = new SubscriberDetails();
						List<SubscriberOnboardingData> subscriberOnboardingDataList = onboardingDataRepoIface
								.getBySubUid(deviceDetails.getSubscriberUid());
						if (!subscriberOnboardingDataList.isEmpty()) {
							if (subscriberOnboardingDataList.size() > 1) {
								subscriberOnboardingData = findLatestOnboardedSub(subscriberOnboardingDataList);
							} else {
								subscriberOnboardingData = subscriberOnboardingDataList.get(0);
							}
						}

						if (subscriberOnboardingData != null) {
							String method = subscriberOnboardingData.getOnboardingMethod();
							SubscriberDTO subscriberDTO = new SubscriberDTO();
							subscriberDTO.setMethodName(method);

							subscriberOnboardingData = onboardingDataRepoIface
									.findLatestSubscriber(subscriber.getSubscriberUid());

							ApiResponse editTemplateDTORes = templateServiceIface
									.getTemplateLatestById(subscriberOnboardingData.getTemplateId());

							if (editTemplateDTORes.isSuccess()) {
								EditTemplateDTO editTemplateDTO = (EditTemplateDTO) editTemplateDTORes.getResult();
								String certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
										deviceDetails.getSubscriberUid(), Constant.SIGN, Constant.ACTIVE);
//								if (certStatus == null) {
//									certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
//											deviceDetails.getSubscriberUid(), "SIGN", "REVOKED");
//									if (certStatus == null) {
//										certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
//												deviceDetails.getSubscriberUid(), "SIGN", "EXPIRED");
//									}
//								}
//
//								if (certStatus == null) {
//									certStatus = subscriberCertificatesRepoIface
//											.getSubscriberCertificateStatusLifeHistory(deviceDetails.getSubscriberUid(),
//													"SIGN", "fail");
//									if (certStatus == null) {
//										certStatus = subscriberCertificatesRepoIface
//												.getSubscriberCertificateStatusLifeHistory(
//														deviceDetails.getSubscriberUid(), "AUTH", "fail");
//									}
//									if (certStatus.equals("fail") || certStatus == "fail") {
//										certStatus = "FAILED";
//									}
//								}

								subscriberDetails.setSubscriberName(subscriber.getFullName());
								subscriberDetails.setOnboardingMethod(method);
								subscriberDetails.setTemplateDetails(editTemplateDTO);
								subscriberDetails.setCertificateStatus(certStatus);
								PinStatus pinStatus = new PinStatus();
								if (certStatus != null) {
									if (certStatus.equals(Constant.ACTIVE)) {
										SubscriberCertificatePinHistory certificatePinHistory = subscriberCertPinHistoryRepoIface
												.findBysubscriberUid(deviceDetails.getSubscriberUid());
										if (certificatePinHistory != null) {
											if (certificatePinHistory.getAuthPinList() != null) {
												pinStatus.setAuthPinSet(true);
											}
											if (certificatePinHistory.getSignPinList() != null) {
												pinStatus.setSignPinSet(true);
											}
											subscriberDetails.setPinStatus(pinStatus);
										} else {
											subscriberDetails.setCertificateStatus(certStatus);
											subscriberDetails.setPinStatus(pinStatus);
										}
									} else {
										subscriberDetails.setCertificateStatus(certStatus);
										subscriberDetails.setPinStatus(pinStatus);
									}
								} else {
									subscriberDetails.setCertificateStatus(Constant.PENDING);
									subscriberDetails.setPinStatus(pinStatus);
								}
							} else {
								subscriberDetails = null;
							}
							responseDTO.setSubscriberDetails(subscriberDetails);
						} else {
							responseDTO.setSubscriberDetails(null);
						}
					} else {
						responseDTO.setSubscriberDetails(null);
					}

					String paymentStatus = subscriberRepoIface.subscriberPaymnetStatus(subscriber.getSubscriberUid());
					if (paymentStatus != null) {
						responseDTO.setOnboardingPaymentStatus(paymentStatus);
					} else {
						responseDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_PENDING);
					}

					String firstTimeOnboarding = subscriberRepoIface
							.firstTimeOnboardingPaymentStatus(subscriber.getSubscriberUid());
					if (firstTimeOnboarding != null) {
						responseDTO.setFirstTimeOnboarding(false);
					} else {
						responseDTO.setFirstTimeOnboarding(true);
					}

					fcmToken = fcmTokenRepoIface.save(fcmToken);
					deviceDetails = deviceRepoIface.save(deviceDetails);
					logger.info(
							CLASS + " checkValidationForSubscriber This Device is Already Registered. Please Continue");
					if (!signRequired) {
						String consentData = "I agreed to above Terms and conditions and Data privacy terms";
						ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
						SubscriberConsents subscriberConsents = subscriberConsentsRepo
								.findSubscriberConsentBySuidAndConsentId(deviceDetails.getSubscriberUid(),
										consentHistory.getId());
						if (subscriberConsents == null) {
							SubscriberConsents subscriberConsents1 = new SubscriberConsents();
							subscriberConsents1.setConsentData(consentData);
							subscriberConsents1.setSuid(deviceDetails.getSubscriberUid());
							subscriberConsents1.setCreatedOn(AppUtil.getDate());
							subscriberConsents1.setConsentId(consentHistory.getId());
							subscriberConsentsRepo.save(subscriberConsents1);
						}
					}
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.this.device.is.already.registered.please.continue",
									null, Locale.ENGLISH),
							responseDTO);
				}

				if (countDevice >= 1) {
					// deviceDetails =
					// deviceRepoIface.findBydeviceDetails(mobileOTPDto.getDeviceId());
					deviceDetails = deviceRepoIface.findBydeviceUidAndStatus(mobileOTPDto.getDeviceId(), "ACTIVE");
					logger.info(CLASS + " checkValidationForSubscriber deviceDetails : {}", deviceDetails);
					// SubscriberStatus subscriberStatus =
					// statusRepoIface.findBysubscriberUid(deviceDetails.getSubscriberUid());

					// logger.info(CLASS + " checkValidationForSubscriber subscriberStatus : {}",
					// subscriberStatus);
					// if(deviceDetails != null) {
					// return
					// AppUtil.createApiResponse(false,messageSource.getMessage("api.error.this.device.is.already.register.with.differet.email.or.mobile.no",
					// null, Locale.ENGLISH),
					// null);
					// }else {

					// }
					System.out.println(" deviceDetails " + deviceDetails);
					// if (deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
					if (deviceDetails != null) {
						if (countDevice >= 1 && countEmail == 0) {
							logger.info(CLASS
									+ " checkValidationForSubscriber This device is already register with differnt email");
							return AppUtil.createApiResponse(false,
									messageSource.getMessage(
											"api.error.this.device.is.already.registered.with.different.email", null,
											Locale.ENGLISH),
									null);
						}

						if (countDevice >= 1 && countMobile == 0) {
							logger.info(CLASS
									+ "checkValidationForSubscriber This device is already register with differnt mobile number");
							return AppUtil.createApiResponse(false,
									messageSource.getMessage(
											"api.error.this.device.is.already.register.with.different.mobile.number",
											null, Locale.ENGLISH),
									null);
						}
					}
				}

				if (countDevice == 0 && countEmail == 1) {

					Subscriber subscriber = subscriberRepoIface.getSubscriberUidByEmailAndMobile(
							mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());

					SubscriberDevice device = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());

					if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
						if (countEmail == 1) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.email.id.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						}
					}
				} else if (countDevice == 0 && countMobile == 1) {
					Subscriber subscriber = subscriberRepoIface.getSubscriberUidByEmailAndMobile(
							mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());

					SubscriberDevice device = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());

					if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
						if (countMobile == 1) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.mobile.no.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						}
					}

				}
				if (countDevice == 0) {
					int activeDeviceCount = subscriberCompleteDetailRepoIface
							.getActiveDeviceCountStatusByEmailAndMobileNo(Constant.ACTIVE,
									mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());
					if (activeDeviceCount != 0) {
						if (countDevice == 0 && countEmail == 1) {
							logger.info(CLASS
									+ "checkValidationForSubscriber This email id is already register with different device, Please De-activate the other device");
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.email.id.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						}
						if (countDevice == 0 && countMobile == 1) {
							logger.info(CLASS
									+ "checkValidationForSubscriber This mobile no. is already register with different device, Please De-activate the other device");
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.mobile.no.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						} else {
							return AppUtil.createApiResponse(true, "", null);
						}
					} else {
						return AppUtil.createApiResponse(true, "", null);
					}
				} else {
					return AppUtil.createApiResponse(true, "", null);
				}

			} else {
				logger.info(CLASS + "checkValidationForSubscriber OTP verification is failed");
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.otp.verification.is.failed", null, Locale.ENGLISH), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			sentryClientExceptions.captureTags(mobileOTPDto.getSuID(), mobileOTPDto.getSubscriberMobileNumber(),
					"checkValidationSubscriber", "SubscriberController");
			sentryClientExceptions.captureExceptions(ex);
			logger.error(CLASS + "checkValidationForSubscriber Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource
					.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "checkValidationForSubscriber Exception {}", e.getMessage());
			e.printStackTrace();
			sentryClientExceptions.captureTags(mobileOTPDto.getSuID(), mobileOTPDto.getSubscriberMobileNumber(),
					"checkValidationSubscriber", "SubscriberController");
			sentryClientExceptions.captureExceptions(e);
			return AppUtil.createApiResponse(false, messageSource
					.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
		}
	}

	public ApiResponse checkValidationForSubscriberNew(MobileOTPDto mobileOTPDto) {
		logger.info(CLASS + " checkValidationForSubscriber request {}", mobileOTPDto);
		int countDevice;
		int countMobile;
		int countEmail;
		SubscriberFcmToken fcmToken = new SubscriberFcmToken();
		SubscriberRegisterResponseDTO responseDTO = new SubscriberRegisterResponseDTO();
		SubscriberDevice deviceDetails = null;
		SubscriberDevice subscriberDeviceDetails = null;
		Optional<Subscriber> previousSuid = null;
		try {
			if (mobileOTPDto.getOtpStatus()) {
				countDevice = subscriberRepoIface.countSubscriberDevice(mobileOTPDto.getDeviceId());
				logger.info(CLASS + "checkValidationForSubscriber countDevice {}, DeviceId {} ", countDevice,
						mobileOTPDto.getDeviceId());
				countMobile = subscriberRepoIface.countSubscriberMobile(mobileOTPDto.getSubscriberMobileNumber());
				logger.info(CLASS + "checkValidationForSubscriber countMobile {} , SubscriberMobileNumber {} ",
						countMobile, mobileOTPDto.getSubscriberMobileNumber());
				countEmail = subscriberRepoIface
						.countSubscriberEmailId(mobileOTPDto.getSubscriberEmail().toLowerCase());
				logger.info(CLASS + "checkValidationForSubscriber countEmail {}, SubscriberEmail {} ", countEmail,
						mobileOTPDto.getSubscriberEmail().toLowerCase());

				if (countEmail == 1 && countMobile == 1 && countDevice >= 1) {
					previousSuid = Optional.ofNullable(subscriberRepoIface.getSubscriberDetailsByEmailAndMobile(
							mobileOTPDto.getSubscriberEmail().toLowerCase(), mobileOTPDto.getSubscriberMobileNumber()));
					if (previousSuid == null) {
						return AppUtil.createApiResponse(false,
								"This mobile no. is already register with different email id", null);
					} else {
						SubscriberDevice subscriberDevice = deviceRepoIface
								.getSubscriber(previousSuid.get().getSubscriberUid());
						if (subscriberDevice.getDeviceUid().equals(mobileOTPDto.getDeviceId())) {
							if (subscriberDevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {
								return AppUtil.createApiResponse(false, messageSource
										.getMessage("api.error.this.device.is.disabled", null, Locale.ENGLISH), null);
							} else {
								countDevice = 1;
							}
						} else {
							subscriberDeviceDetails = deviceRepoIface.findDeviceDetailsById(mobileOTPDto.getDeviceId());
							SubscriberStatus subscriberStatus = statusRepoIface
									.findBysubscriberUid(subscriberDeviceDetails.getSubscriberUid());
							if (subscriberDeviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.this.device.is.already.register.with.differet.email.or.mobile.no",
										null, Locale.ENGLISH), null);
							} else if (subscriberDeviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)
									&& subscriberStatus.getSubscriberStatus()
											.equals(Constant.SUBSCRIBER_STATUS_INACTIVE)) {
								countDevice = subscriberRepoIface.countSubscriberDevice(mobileOTPDto.getDeviceId());
							}
						}
					}

				}

				if (countDevice == 1 && countMobile == 1 && countEmail == 1) {

					logger.info(CLASS
							+ " checkValidationForSubscriber countDevice == 1 && countMobile == 1 && countEmail == 1 ");
					deviceDetails = deviceRepoIface.findBydeviceUid(mobileOTPDto.getDeviceId());
					// deviceDetails =
					// deviceRepoIface.getSubscriber(previousSuid.getSubscriberUid());

					if (deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)
							|| deviceDetails.getDeviceStatus() == Constant.DEVICE_STATUS_DISABLED) {
						System.out.println("checkValidationForSubscriber This Device is disabled ");
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.this.device.is.disabled", null, Locale.ENGLISH),
								null);
					}

					fcmToken = fcmTokenRepoIface.findBysubscriberUid(deviceDetails.getSubscriberUid());
					responseDTO.setSuID(deviceDetails.getSubscriberUid());
					fcmToken.setSubscriberUid(deviceDetails.getSubscriberUid());
					fcmToken.setCreatedDate(AppUtil.getDate());
					fcmToken.setFcmToken(mobileOTPDto.getFcmToken());
					Subscriber subscriber = subscriberRepoIface.findBysubscriberUid(deviceDetails.getSubscriberUid());

					if (!subscriber.getEmailId().equals(mobileOTPDto.getSubscriberEmail().toLowerCase())
							|| !subscriber.getMobileNumber().equals(mobileOTPDto.getSubscriberMobileNumber())) {
						logger.info(CLASS
								+ "checkValidationForSubscriber This Device is already register with differet Email or Mobile No.");
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.this.device.is.already.register.with.differet.email.or.mobile.no",
										null, Locale.ENGLISH),
								null);
					}
					SubscriberStatus subscriberStatus = statusRepoIface
							.findBysubscriberUid(deviceDetails.getSubscriberUid());
					if (subscriberStatus != null) {
						if (subscriberStatus.getSubscriberStatus() == Constant.SUBSCRIBER_STATUS
								|| subscriberStatus.getSubscriberStatus().equals(Constant.SUBSCRIBER_STATUS)) {
							responseDTO.setSubscriberStatus(Constant.SUBSCRIBER_STATUS);
						} else {
							responseDTO.setSubscriberStatus(subscriberStatus.getSubscriberStatus());
						}
					} else {
						logger.info(CLASS
								+ " checkValidationForSubscriber This Device is Already Registered. Please Continue");
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.this.device.is.already.registered.please.continue",
										null, Locale.ENGLISH),
								responseDTO);
					}
					if (!subscriberStatus.getSubscriberStatus().equals(Constant.SUBSCRIBER_STATUS)) {
						SubscriberOnboardingData subscriberOnboardingData = null;
						SubscriberDetails subscriberDetails = new SubscriberDetails();
						List<SubscriberOnboardingData> subscriberOnboardingDataList = onboardingDataRepoIface
								.getBySubUid(deviceDetails.getSubscriberUid());
						if (!subscriberOnboardingDataList.isEmpty()) {
							if (subscriberOnboardingDataList.size() > 1) {
								subscriberOnboardingData = findLatestOnboardedSub(subscriberOnboardingDataList);
							} else {
								subscriberOnboardingData = subscriberOnboardingDataList.get(0);
							}
						}

						if (subscriberOnboardingData != null) {
							String method = subscriberOnboardingData.getOnboardingMethod();
							SubscriberDTO subscriberDTO = new SubscriberDTO();
							subscriberDTO.setMethodName(method);

							subscriberOnboardingData = onboardingDataRepoIface
									.findLatestSubscriber(subscriber.getSubscriberUid());

							ApiResponse editTemplateDTORes = templateServiceIface
									.getTemplateLatestById(subscriberOnboardingData.getTemplateId());

							if (editTemplateDTORes.isSuccess()) {
								EditTemplateDTO editTemplateDTO = (EditTemplateDTO) editTemplateDTORes.getResult();
								String certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
										deviceDetails.getSubscriberUid(), Constant.SIGN, Constant.ACTIVE);
//								if (certStatus == null) {
//									certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
//											deviceDetails.getSubscriberUid(), "SIGN", "REVOKED");
//									if (certStatus == null) {
//										certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
//												deviceDetails.getSubscriberUid(), "SIGN", "EXPIRED");
//									}
//								}
//
//								if (certStatus == null) {
//									certStatus = subscriberCertificatesRepoIface
//											.getSubscriberCertificateStatusLifeHistory(deviceDetails.getSubscriberUid(),
//													"SIGN", "fail");
//									if (certStatus == null) {
//										certStatus = subscriberCertificatesRepoIface
//												.getSubscriberCertificateStatusLifeHistory(
//														deviceDetails.getSubscriberUid(), "AUTH", "fail");
//									}
//									if (certStatus.equals("fail") || certStatus == "fail") {
//										certStatus = "FAILED";
//									}
//								}

								subscriberDetails.setSubscriberName(subscriber.getFullName());
								subscriberDetails.setOnboardingMethod(method);
								subscriberDetails.setTemplateDetails(editTemplateDTO);
								subscriberDetails.setCertificateStatus(certStatus);
								PinStatus pinStatus = new PinStatus();
								if (certStatus != null) {
									if (certStatus.equals(Constant.ACTIVE)) {
										SubscriberCertificatePinHistory certificatePinHistory = subscriberCertPinHistoryRepoIface
												.findBysubscriberUid(deviceDetails.getSubscriberUid());
										if (certificatePinHistory != null) {
											if (certificatePinHistory.getAuthPinList() != null) {
												pinStatus.setAuthPinSet(true);
											}
											if (certificatePinHistory.getSignPinList() != null) {
												pinStatus.setSignPinSet(true);
											}
											subscriberDetails.setPinStatus(pinStatus);
										} else {
											subscriberDetails.setCertificateStatus(certStatus);
											subscriberDetails.setPinStatus(pinStatus);
										}
									} else {
										subscriberDetails.setCertificateStatus(certStatus);
										subscriberDetails.setPinStatus(pinStatus);
									}
								} else {
									subscriberDetails.setCertificateStatus(Constant.PENDING);
									subscriberDetails.setPinStatus(pinStatus);
								}
							} else {
								subscriberDetails = null;
							}
							responseDTO.setSubscriberDetails(subscriberDetails);
						} else {
							responseDTO.setSubscriberDetails(null);
						}
					} else {
						responseDTO.setSubscriberDetails(null);
					}

					String paymentStatus = subscriberRepoIface.subscriberPaymnetStatus(subscriber.getSubscriberUid());
					if (paymentStatus != null) {
						responseDTO.setOnboardingPaymentStatus(paymentStatus);
					} else {
						responseDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_PENDING);
					}

					String firstTimeOnboarding = subscriberRepoIface
							.firstTimeOnboardingPaymentStatus(subscriber.getSubscriberUid());
					if (firstTimeOnboarding != null) {
						responseDTO.setFirstTimeOnboarding(false);
					} else {
						responseDTO.setFirstTimeOnboarding(true);
					}

					fcmToken = fcmTokenRepoIface.save(fcmToken);
					logger.info(
							CLASS + " checkValidationForSubscriber This Device is Already Registered. Please Continue");
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.this.device.is.already.registered.please.continue",
									null, Locale.ENGLISH),
							responseDTO);
				}

				if (countDevice >= 1) {
					deviceDetails = deviceRepoIface.findBydeviceDetails(mobileOTPDto.getDeviceId());
					logger.info(CLASS + " checkValidationForSubscriber deviceDetails : {}", deviceDetails);
					SubscriberStatus subscriberStatus = statusRepoIface
							.findBysubscriberUid(deviceDetails.getSubscriberUid());

					logger.info(CLASS + " checkValidationForSubscriber subscriberStatus : {}", subscriberStatus);
					if (!subscriberStatus.getSubscriberStatus().equals(Constant.SUBSCRIBER_STATUS_INACTIVE)
							&& !deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {
						if (countDevice == 1 && countEmail == 0) {
							logger.info(CLASS
									+ " checkValidationForSubscriber This device is already register with differnt email");
							return AppUtil.createApiResponse(false,
									messageSource.getMessage(
											"api.error.this.device.is.already.registered.with.different.email", null,
											Locale.ENGLISH),
									null);
						}

						if (countDevice == 1 && countMobile == 0) {
							logger.info(CLASS
									+ "checkValidationForSubscriber This device is already register with differnt mobile number");
							return AppUtil.createApiResponse(false,
									messageSource.getMessage(
											"api.error.this.device.is.already.register.with.different.mobile.number",
											null, Locale.ENGLISH),
									null);
						}
					}
				}
				if (countDevice == 0 && countEmail == 1) {

					Subscriber subscriber = subscriberRepoIface.getSubscriberUidByEmailAndMobile(
							mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());

					SubscriberDevice device = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());

					if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
						if (countEmail == 1) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.email.id.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						}
					}
				} else if (countDevice == 0 && countMobile == 1) {
					Subscriber subscriber = subscriberRepoIface.getSubscriberUidByEmailAndMobile(
							mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());

					SubscriberDevice device = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());

					if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
						if (countMobile == 1) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.mobile.no.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						}
					}

				}
				if (countDevice == 0) {
					int activeDeviceCount = subscriberCompleteDetailRepoIface
							.getActiveDeviceCountStatusByEmailAndMobileNo(Constant.ACTIVE,
									mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());
					if (activeDeviceCount != 0) {
						if (countDevice == 0 && countEmail == 1) {
							logger.info(CLASS
									+ "checkValidationForSubscriber This email id is already register with different device, Please De-activate the other device");
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.email.id.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						}
						if (countDevice == 0 && countMobile == 1) {
							logger.info(CLASS
									+ "checkValidationForSubscriber This mobile no. is already register with different device, Please De-activate the other device");
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.mobile.no.is.already.register.with.different.device.please.deactivate.the.other.device",
									null, Locale.ENGLISH), null);
						} else {
							return AppUtil.createApiResponse(true, "", null);
						}
					} else {
						return AppUtil.createApiResponse(true, "", null);
					}
				} else {
					return AppUtil.createApiResponse(true, "", null);
				}

			} else {
				logger.info(CLASS + "checkValidationForSubscriber OTP verification is failed");
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.otp.verification.is.failed", null, Locale.ENGLISH), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "checkValidationForSubscriber Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource
					.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "checkValidationForSubscriber Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource
					.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse addSubscriberObData(SubscriberObRequestDTO obRequestDTO) throws Exception {
		Date startTime = new Date();
		SubscriberObData subscriberObData = new SubscriberObData();
		SubscriberObData additionalFile = new SubscriberObData();
		Subscriber subscriber = new Subscriber();
		Subscriber savedSubscriber = new Subscriber();
		SubscriberOnboardingData onboardingData = new SubscriberOnboardingData();
		SubscriberDevice subscriberDevice = new SubscriberDevice();
		SubscriberRaData raData = new SubscriberRaData();
		SubscriberStatus status = new SubscriberStatus();
		IssueCertDTO issueCertDTO = new IssueCertDTO();
		int idDocNumberCount;
		String subscriberStatus = null;
		try {
			subscriber = subscriberRepoIface.findBysubscriberUid(obRequestDTO.getSuID());
			if (subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
			}
		}catch (Exception e){
			e.printStackTrace();
			return OnBoardingServiceException.handleExceptionWithStaticMessageWithSentry(e,obRequestDTO.getSuID());
		}

		subscriberObData = obRequestDTO.getSubscriberData();

		if (!obRequestDTO.getSubscriberType().equals(Constant.RESIDENT)
				&& obRequestDTO.getSubscriberType() != Constant.RESIDENT) {
			if (obRequestDTO.getSubscriberData().getOptionalData1() != null
					&& !obRequestDTO.getSubscriberData().getOptionalData1().isEmpty()) {
				int count = isOptionData1Present(obRequestDTO.getSubscriberData().getOptionalData1());
				if (count == 1) {
					String suid = onboardingDataRepoIface
							.getOptionalData1Subscriber(obRequestDTO.getSubscriberData().getOptionalData1());
					if (!suid.equals(obRequestDTO.getSuID())) {
						logger.info(CLASS
								+ "addSubscriberObData isOptionData1Present Onboarding can not be processed because the same national id already exists: ",
								count);
						return AppUtil.createApiResponse(false, messageSource.getMessage(
								"api.error.onboarding.can.not.be.processed.because.the.same.national.id.already.exists",
								null, Locale.ENGLISH), null);
					}
				}
			} else {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.optional.data.is.empty", null, Locale.ENGLISH), null);
			}
		}

		try {
			subscriberStatus = subscriberRepoIface.getSubscriberStatus(obRequestDTO.getSuID());
			logger.info(CLASS + "addSubscriberObData req for subscriberStatus: ", subscriberStatus);
			if (subscriberStatus == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
			}

			subscriberDevice = deviceRepoIface.getSubscriber(obRequestDTO.getSuID());
			if (subscriberDevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)
					|| subscriberDevice.getDeviceStatus() == Constant.DEVICE_STATUS_DISABLED) {
				logger.info(CLASS + " addSubscriberObData req for subscriberDevice: ", subscriberDevice);
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.this.device.is.disabled", null, Locale.ENGLISH), null);
			}

			int idDocCount = subscriberRepoIface.getIdDocCount(subscriberObData.getDocumentNumber());
			idDocNumberCount = subscriberRepoIface.getSubscriberIdDocNumber(subscriberObData.getDocumentNumber(),
					obRequestDTO.getSuID());
			if (idDocCount > 0) {
				if (idDocCount > 0 && idDocNumberCount == 0) {
					return AppUtil.createApiResponse(false, messageSource
							.getMessage("api.error.this.document.is.already.onboarded", null, Locale.ENGLISH), null);
				}
			}
		} catch (Exception e){
			e.printStackTrace();
			return OnBoardingServiceException.handleExceptionWithStaticMessageWithSentry(e,obRequestDTO.getSuID());
		}
		subscriber.setFullName(subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier());
		subscriber.setDateOfBirth(subscriberObData.getDateOfBirth());
		subscriber.setIdDocType(subscriberObData.getDocumentType());
		subscriber.setIdDocNumber(subscriberObData.getDocumentNumber());
		subscriber.setUpdatedDate(AppUtil.getDate());
		subscriber.setSubscriberUid(obRequestDTO.getSuID());
		if (!obRequestDTO.getSubscriberType().equals(Constant.RESIDENT)
				&& obRequestDTO.getSubscriberType() != Constant.RESIDENT) {
			subscriber.setNationalId(obRequestDTO.getSubscriberData().getOptionalData1());
		}

		onboardingData.setCreatedDate(AppUtil.getDate());
		onboardingData.setIdDocType(subscriberObData.getDocumentType());
		onboardingData.setIdDocNumber(subscriberObData.getDocumentNumber());
		onboardingData.setOnboardingMethod(obRequestDTO.getOnboardingMethod());
		onboardingData.setSubscriberUid(obRequestDTO.getSuID());
		onboardingData.setTemplateId(obRequestDTO.getTemplateId());
		onboardingData.setOnboardingMethod(obRequestDTO.getOnboardingMethod());
		onboardingData.setSubscriberType(obRequestDTO.getSubscriberType());
		onboardingData.setIdDocCode(subscriberObData.getDocumentCode());
		onboardingData.setGender(subscriberObData.getGender());
		onboardingData.setGeolocation(subscriberObData.getGeoLocation());
		onboardingData.setOptionalData1(subscriberObData.getOptionalData1());
		onboardingData.setDateOfExpiry(subscriberObData.getDateOfExpiry());

		// set LOA based on onboarding method
		OnboardingMethod onboardingMethod = onBoardingMethodRepoIface
				.findByonboardingMethod(obRequestDTO.getOnboardingMethod());
		onboardingData.setLevelOfAssurance(onboardingMethod.getLevelOfAssurance());

		raData.setCommonName(subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier());
		raData.setCertificateType(Constant.BOTH);
		raData.setCountryName(subscriberObData.getNationality());
		raData.setCreatedDate(AppUtil.getDate());
		raData.setPkiPassword(
				subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier());
		raData.setPkiPasswordHash(
				subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier().hashCode());
		raData.setPkiUserName(
				subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier());
		raData.setPkiUserNameHash(
				subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier().hashCode());
		raData.setSubscriberUid(obRequestDTO.getSuID());

		issueCertDTO.setSubscriberUniqueId(obRequestDTO.getSuID());

		try {
			FaceFeaturesDto faceFeaturesDto = new FaceFeaturesDto();
			faceFeaturesDto.setSubscriberPhoto(obRequestDTO.getSubscriberData().getSubscriberSelfie());
			logger.info(CLASS + " selfie"+ obRequestDTO.getSubscriberData().getSubscriberSelfie());

			ObjectMapper objectMapper = new ObjectMapper();
			Selfie selfie = new Selfie();
			selfie.setSubscriberSelfie(obRequestDTO.getSubscriberData().getSubscriberSelfie());
			selfie.setSubscriberUniqueId(obRequestDTO.getSuID());
			CompletableFuture<ApiResponse> selfieResponse  = edmsService.saveFileToEdms(selfie,"selfie",null);
			ApiResponse apiResponse = selfieResponse.get();
			if (apiResponse.isSuccess()) {
				logger.info(CLASS + " addSubscriberObData res for saveFileToEdms: ", selfieResponse);
				String selfieURI = (String) apiResponse.getResult();
				onboardingData.setSelfieUri(selfieURI);
			} else {
				logger.info(CLASS + "addSubscriberObData res in false for saveFileToEdms: ", apiResponse.getMessage());
				return AppUtil.createApiResponse(false, apiResponse.getMessage(), null);
			}
			CompletableFuture<ApiResponse>  selfieThumbnailResponse = edmsService.createThumbnailOfSelfie(selfie);
			ApiResponse selfieApiResponse = selfieThumbnailResponse.get();
			if (selfieApiResponse.isSuccess()) {
				logger.info(CLASS + "addSubscriberObData res for createThumlbnailOfSelfie: ", selfieApiResponse.isSuccess());
				onboardingData.setSelfieThumbnailUri(selfieApiResponse.getResult().toString());
			} else {
				return AppUtil.createApiResponse(false, selfieApiResponse.getMessage(), null);
			}

			savedSubscriber = subscriberRepoIface.save(subscriber);

			additionalFile = subscriberObData;
			additionalFile.setSubscriberSelfie(null);
			additionalFile.setSubscriberUniqueId(onboardingData.getSubscriberUid());
			String additionalFieldSaved = objectMapper.writeValueAsString(additionalFile);
			onboardingData.setOnboardingDataFieldsJson(additionalFieldSaved);
			onboardingData.setRemarks(obRequestDTO.getSubscriberData().getRemarks());


			faceFeaturesDto.setSuid(onboardingData.getSubscriberUid());
			faceFeaturesDto.setSubscriberName(savedSubscriber.getFullName());
			faceFeaturesDto.setSubscriberDataJson(additionalFieldSaved);
			// for faceFeatures rest call
			//saveFaceFeaturesAsync(faceFeaturesDto,registerFaceURL, restTemplate);

			onboardingData = onboardingDataRepoIface.save(onboardingData);
			status = statusRepoIface.findBysubscriberUid(onboardingData.getSubscriberUid());

			String subStatus = subscriberRepoIface.getSubscriberStatus(onboardingData.getSubscriberUid());
			logger.info(CLASS + "addSubscriberObData getSubscriberStatus {}", subStatus);
			if (subStatus != null) {
				if (subStatus.equals(Constant.ACTIVE)) {
					status.setSubscriberStatus(Constant.ACTIVE);
					status.setSubscriberStatusDescription(Constant.LOA_UPDATED);
					status.setUpdatedDate(AppUtil.getDate());
					status = statusRepoIface.save(status);
				} else if (subStatus.equals(Constant.PIN_SET_REQUIRED)) {
					status.setSubscriberStatus(Constant.PIN_SET_REQUIRED);
					status.setSubscriberStatusDescription(Constant.LOA_UPDATED);
					status.setUpdatedDate(AppUtil.getDate());
					status = statusRepoIface.save(status);
				} else {
					status.setSubscriberStatus(Constant.ONBOARDED);
					status.setSubscriberStatusDescription(Constant.ONBOARDED_SUCESSFULLY);
					status.setUpdatedDate(AppUtil.getDate());
					status = statusRepoIface.save(status);
					raData = raRepoIface.save(raData);
				}
			} else {
				status.setSubscriberStatus(Constant.ONBOARDED);
				status.setSubscriberStatusDescription(Constant.ONBOARDED_SUCESSFULLY);
				status.setUpdatedDate(AppUtil.getDate());
				status = statusRepoIface.save(status);
				raData = raRepoIface.save(raData);
			}

			Subscriber s = subscriberRepoIface.findBysubscriberUid(onboardingData.getSubscriberUid());
			if (s != null) {

				System.out.println(" dt stging ");
				
				SubscriberOnboardingData finalOnboardingData = onboardingData;
				Subscriber finalSavedSubscriber = s;
				SubscriberRaData finalRaData = raData;

//				VisitorCardRequestDTO visitorCardRequestDTO = new VisitorCardRequestDTO();
//				visitorCardRequestDTO.setVisitorCardNumber(finalSavedSubscriber.getSubscriberUid());
//				visitorCardRequestDTO.setNationality(finalRaData.getCountryName());
//				visitorCardRequestDTO.setSuid(finalSavedSubscriber.getSubscriberUid());
//				visitorCardRequestDTO.setDateOfBirth(finalSavedSubscriber.getDateOfBirth().substring(0, 10));
//				visitorCardRequestDTO.setSelfieUri(finalOnboardingData.getSelfieUri());
//				visitorCardRequestDTO.setFullName(finalSavedSubscriber.getFullName());
//				visitorCardRequestDTO.setIdDocNumber(finalSavedSubscriber.getIdDocNumber());
//				visitorCardRequestDTO.setGender(finalOnboardingData.getGender());
//				visitorCardRequestDTO.setSubscriberType(finalOnboardingData.getSubscriberType());
//				HttpHeaders httpHeaders=new HttpHeaders();
//				httpHeaders.setContentType(MediaType.APPLICATION_JSON);
//				HttpEntity<Object> requestEntity = new HttpEntity<>(visitorCardRequestDTO, httpHeaders);
//				restTemplate.exchange(visitorCardUrl,HttpMethod.POST,requestEntity,ApiResponse.class);
//				Date endTime = new Date();
//				double toatlTime = AppUtil.getDifferenceInSeconds(startTime, endTime);
//				logModelServiceImpl.setLogModel(true, s.getSubscriberUid(),
//						onboardingData.getGeolocation(), Constant.SUBSCRIBER_ONBOARDED,
//						s.getSubscriberUid(), String.valueOf(toatlTime), startTime, endTime, null);
				logger.info(CLASS + " addSubscriberObData Subscriber OnBoarding Data Saved {}", s);
				return AppUtil.createApiResponse(true, messageSource
						.getMessage("api.response.ugPass.application.submitted.successfully", null, Locale.ENGLISH),
						s);
			} else {
			//	logModelServiceImpl.setLogModel(false, s.getSubscriberUid(), null,
			//			Constant.SUBSCRIBER_ONBOARDED, s.getSubscriberUid(), null, null, null, null);
				return AppUtil.createApiResponse(false, messageSource.getMessage(
						"api.error.ugPass.application.submission.failed", null, Locale.ENGLISH), subscriber);
			}
		}catch (Exception e){
			e.printStackTrace();
			return OnBoardingServiceException.handleExceptionWithStaticMessageWithSentry(e,obRequestDTO.getSuID());
		}
	}

	@Async
	public void saveFaceFeaturesAsync(FaceFeaturesDto faceFeaturesDto,String faceUrl,RestTemplate restTemplate) {
		try {
			logger.info(CLASS + " Calling Face Features Save URL " + faceFeaturesDto.toString());
			// Set up headers and request entity
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> reqEntity = new HttpEntity<>(faceFeaturesDto, headers);
			// Send POST request asynchronously
			ResponseEntity<ApiResponse> res = restTemplate.exchange(faceUrl, HttpMethod.POST, reqEntity, ApiResponse.class);
			// Return the result in a CompletableFuture
			logger.info("saveFaceFeaturesAsync res: " + res);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " Error in saveFaceFeaturesAsync: " + e.getMessage());
		}
	}

	@Override
	public ApiResponse getSubscriberObData(GetSubscriberObDataDTO subscriberUID) {
		logger.info(CLASS + "getSubscriberObData req {}", subscriberUID);
		Subscriber subscriber = new Subscriber();
		String certStatus = null;
		SubscriberObData onboardingData = new SubscriberObData();
		SubscriberOnboardingData data = new SubscriberOnboardingData();
		List<SubscriberOnboardingData> dataList = new LinkedList<>();
		SubscriberObRequestDTO obRequestDTO = new SubscriberObRequestDTO();
		CertificateDetailDto certificateDetailDto = new CertificateDetailDto();
		SubscriberCertificate subscriberCertificate = new SubscriberCertificate();
		SubscriberStatus status = new SubscriberStatus();
		try {
			String paymentStatus = subscriberRepoIface.subscriberPaymnetStatus(subscriberUID.getSuid());
			logger.info(CLASS + "subscriberPaymnetStatus paymentStatus {}", paymentStatus);
			String paymentIntiatiedStatus = subscriberRepoIface
					.subscriberPaymnetInitaiatedStatus(subscriberUID.getSuid());
			logger.info(CLASS + " subscriberPaymnetInitaiatedStatus paymentIntiatiedStatus ", paymentIntiatiedStatus);
			String paymentCertStatus = subscriberRepoIface.subscriberPaymnetCertStatus(subscriberUID.getSuid());
			logger.info(CLASS + "subscriberPaymnetCertStatus paymentCertStatus {}", paymentCertStatus);
			if (subscriberUID != null) {
				subscriber = subscriberRepoIface.findBysubscriberUid(subscriberUID.getSuid());
				if (subscriber == null) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.subscriber.details.not.found", null, Locale.ENGLISH),
							null);
				}
				certStatus = subscriberRepoIface.getCertStatus(subscriberUID.getSuid());

				dataList = onboardingDataRepoIface.getBySubUid(subscriberUID.getSuid());
				subscriberCertificate = subscriberCertificatesRepoIface
						.findBySubscriberUniqueId(subscriberUID.getSuid());

				if (subscriberCertificate != null) {
					String[] expDate = subscriberCertificate.getCerificateExpiryDate().toString().split(" ");
					String[] issueDate = subscriberCertificate.getCertificateIssueDate().toString().split(" ");
					certificateDetailDto.setCertStatus(subscriberCertificate.getCertificateStatus().toString());
					certificateDetailDto.setIssueDate(issueDate[0]);
					certificateDetailDto.setExpiryDate(expDate[0]);

					if (subscriberCertificate.getUpdatedDate() != null) {
						String[] revokeDate = subscriberCertificate.getUpdatedDate().toString().split(" ");
						certificateDetailDto.setRevokeDate(revokeDate[0]);
					}
					obRequestDTO.setCertificateDetailDto(certificateDetailDto);
				}

				if (!dataList.isEmpty()) {
					if (dataList.size() > 1) {
						data = findLatestOnboardedSub(dataList);
					} else {
						data = dataList.get(0);
					}
				}

				status = statusRepoIface.findBysubscriberUid(subscriberUID.getSuid());
				if (data != null) {

					onboardingData.setDateOfBirth(subscriber.getDateOfBirth());
					onboardingData.setDocumentCode(data.getIdDocCode());
					onboardingData.setDocumentNumber(data.getIdDocNumber());
					onboardingData.setDocumentType(data.getIdDocType());
					onboardingData.setSubscriberUniqueId(data.getSubscriberUid());
					onboardingData.setNationality("UDA");
					ObjectMapper mapper = new ObjectMapper();
					onboardingData = mapper.readValue(data.getOnboardingDataFieldsJson(), SubscriberObData.class);

					if (subscriberUID.isSelfieRequired()) {
						ApiResponse response = getBase64String(data.getSelfieUri());
						if (response.isSuccess()) {
							onboardingData.setSubscriberSelfie((String) response.getResult());
						} else {
							onboardingData.setSubscriberSelfie("");
						}
					}
					obRequestDTO.setSubscriberData(onboardingData);
					obRequestDTO.setSubscriberType(data.getSubscriberType());
					obRequestDTO.setConsentId(1);
					obRequestDTO.setSuID(data.getSubscriberUid());
					obRequestDTO.setOnboardingMethod(data.getOnboardingMethod());
					obRequestDTO.setLevelOfAssurance(data.getLevelOfAssurance());
					obRequestDTO.setTemplateId(data.getTemplateId());
					obRequestDTO.setOnboardingApprovalStatus(status.getSubscriberStatus());

					if (subscriber.getMobileNumber() != null) {
						obRequestDTO.setMobileNo(subscriber.getMobileNumber());
					}
					if (subscriber.getEmailId() != null) {
						obRequestDTO.setEmailId(subscriber.getEmailId());
					}

					if (paymentStatus != null) {
						obRequestDTO.setOnboardingPaymentStatus(paymentStatus);
					} else if (paymentIntiatiedStatus != null) {
						obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_INITIATED);
					} else {
						obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_PENDING);
					}

					if (certStatus == null) {
						obRequestDTO.setCertStatus(Constant.PENDING);
					} else if (certStatus.equalsIgnoreCase(Constant.FAIL) || certStatus.equals(Constant.FAILED)) {
						obRequestDTO.setCertStatus(Constant.FAILED);
					} else if (certStatus.equalsIgnoreCase(Constant.CERT_REVOKED)
							|| certStatus.equals(Constant.REVOKED)) {

//						obRequestDTO.setCertStatus("REVOKED");
//						obRequestDTO.setOnboardingPaymentStatus("Pending");

						if (paymentCertStatus == null) {
							obRequestDTO.setCertStatus(Constant.REVOKED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_PENDING);
						} else if (paymentCertStatus.equalsIgnoreCase(Constant.SUCCESS)) {
							obRequestDTO.setCertStatus(Constant.REVOKED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_SUCCESS);
						} else if (paymentCertStatus.equalsIgnoreCase(Constant.FAILED)) {
							obRequestDTO.setCertStatus(Constant.REVOKED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_FAILED);
						} else {
							obRequestDTO.setCertStatus(Constant.REVOKED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_INITIATED);
						}
					} else if (certStatus.equalsIgnoreCase(Constant.CERT_EXPIRED)
							|| certStatus.equals(Constant.EXPIRED)) {
						// obRequestDTO.setCertStatus(Constant.EXPIRED);
						// obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_PENDING);
						if (paymentCertStatus == null) {
							obRequestDTO.setCertStatus(Constant.EXPIRED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_PENDING);
						} else if (paymentCertStatus.equalsIgnoreCase(Constant.SUCCESS)) {
							obRequestDTO.setCertStatus(Constant.EXPIRED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_SUCCESS);
						} else if (paymentCertStatus.equalsIgnoreCase(Constant.FAILED)) {
							obRequestDTO.setCertStatus(Constant.EXPIRED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_FAILED);
						} else {
							obRequestDTO.setCertStatus(Constant.REVOKED);
							obRequestDTO.setOnboardingPaymentStatus(Constant.PAYMENT_STATUS_INITIATED);
						}
					} else {
						obRequestDTO.setCertStatus(certStatus);
					}

					logger.info(CLASS + " getSubscriberObData Subscriber On-Boarding Data {}", obRequestDTO);
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.subscriber.onboarding.data", null, Locale.ENGLISH),
							obRequestDTO);
				} else {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.subscriber.id.not.matched", null, Locale.ENGLISH),
							null);
				}

			} else {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.id.cant.be.empty", null, Locale.ENGLISH), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + "getSubscriberObData Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "getSubscriberObData Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public void setLogModel(Boolean response, Subscriber subscriber, String geoLocation)
			throws ParseException, PKICoreServiceException {
		logger.info(CLASS + "Set LogModel {} and subscriber {} and geoLocation {}", response, subscriber, geoLocation);
		LogModelDTO logModel = new LogModelDTO();
		logModel.setIdentifier(subscriber.getSubscriberUid());
		logModel.setCorrelationID(generateSubscriberUniqueId());
		logModel.setTransactionID(generateSubscriberUniqueId());
		logModel.setTimestamp(null);
		logModel.setStartTime(getTimeStampString());
		logModel.setEndTime(getTimeStampString());
		logModel.setServiceName(ServiceNames.SUBSCRIBER_ONBOARDED.toString());
		logModel.setLogMessage("RESPONSE");
		logModel.setTransactionType(TransactionType.BUSINESS.toString());
		logModel.setGeoLocation(geoLocation);
		logModel.seteSealUsed(false);
		logModel.setSignatureType(null);

		if (response) {
			logModel.setLogMessageType(LogMessageType.SUCCESS.toString());
		} else {
			logModel.setLogMessageType(LogMessageType.FAILURE.toString());
		}
		logModel.setChecksum(null);

		try {

			ObjectMapper objectMapper = new ObjectMapper();
			String json = objectMapper.writeValueAsString(logModel);
			System.out.println("json => " + json);
			Result checksumResult = DAESService.addChecksumToTransaction(json);
			String push = new String(checksumResult.getResponse());
			LogModelDTO log = objectMapper.readValue(push, LogModelDTO.class);
			mqSender.send(log);
		} catch (Exception e) {
			logger.error("Set LogModel Exception {}", e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public ApiResponse resetPin(GetSubscriberObDataDTO subscriberObDataDTO) {
		logger.info(CLASS + " Reset Pin request {}", subscriberObDataDTO);
		SubscriberOnboardingData onboardingData = new SubscriberOnboardingData();
		ResetPinDTO pinDTO = new ResetPinDTO();
		ApiResponse response = null;
		try {
			List<SubscriberOnboardingData> onboardingDataList = onboardingDataRepoIface
					.getBySubUid(subscriberObDataDTO.getSuid());
			if (onboardingDataList != null) {
				if (onboardingDataList.size() > 1) {
					onboardingData = findLatestOnboardedSub(onboardingDataList);
				} else {
					onboardingData = onboardingDataList.get(0);
				}
			}
			if (onboardingData != null) {
				pinDTO.setIdDocNumber(onboardingData.getIdDocNumber());
				if (subscriberObDataDTO.isSelfieRequired()) {
					response = getBase64String(onboardingData.getSelfieUri());
					if (response.isSuccess()) {
						pinDTO.setSelfie((String) response.getResult());
					}
				} else {
					pinDTO.setSelfie(null);
				}
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.reset.pin.data", null, Locale.ENGLISH), pinDTO);
			} else {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.no.data.found", null, Locale.ENGLISH), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "resetPin Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "resetPin Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getBase64String(String uri) {
		logger.info(CLASS + " getBase64String uri {}", uri);
		try {
			HttpHeaders headersForGet = new HttpHeaders();
			HttpEntity<Object> requestEntityForGet = new HttpEntity<>(headersForGet);
			ResponseEntity<Resource> downloadUrlResult = restTemplate.exchange(uri, HttpMethod.GET, requestEntityForGet,
					Resource.class);

			byte[] buffer = IOUtils.toByteArray(downloadUrlResult.getBody().getInputStream());
			String image2 = new String(Base64.getEncoder().encode(buffer));
			return AppUtil.createApiResponse(true,
					messageSource.getMessage("api.response.base64.of.image.fetched.successfully", null, Locale.ENGLISH),
					image2);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "getBase64String Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "getBase64String Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ResponseEntity<Object> getVideoLiveStreaming(String subscriberUid) {
		logger.info(CLASS + "getVideoLiveStreaming subscriberUid {}", subscriberUid);
		try {
			if (!(Objects.isNull(subscriberUid) || subscriberUid.isEmpty())) {
				String url = subscriberRepoIface.getSubscriberUid(subscriberUid);
				if (!Objects.isNull(url)) {
					HttpHeaders headersForGet = new HttpHeaders();
					HttpEntity<Object> requestEntityForGet = new HttpEntity<>(headersForGet);
					ResponseEntity<Resource> downloadUrlResult = restTemplate.exchange(url, HttpMethod.GET,
							requestEntityForGet, Resource.class);

					return ResponseEntity.status(HttpStatus.OK).header("Content-Type", "video/mp4")
							.body(downloadUrlResult.getBody());
				} else {
					logger.info(CLASS + "getVideoLiveStreaming No video found {}", HttpStatus.NOT_FOUND);
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.no.video.found", null, Locale.ENGLISH), null));
				}

			} else {
				logger.info(CLASS + "getVideoLiveStreaming Subscriber not found {}", HttpStatus.NOT_FOUND);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null));
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "saveSubscriberData Exception {}", ex.getMessage());
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(AppUtil.createApiResponse(false, messageSource.getMessage(
							"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null));
		} catch (Exception e) {
			logger.error(CLASS + "getVideoLiveStreaming Exception {}", e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(AppUtil.createApiResponse(false, messageSource.getMessage(
							"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null));
		}

	}

	public static SubscriberOnboardingData findLatestOnboardedSub(
			List<SubscriberOnboardingData> subscriberOnboardingData) {
		Date[] dates = new Date[subscriberOnboardingData.size() - 1];
		int i = 0;
		SimpleDateFormat simpleDateFormat = null;
		for (SubscriberOnboardingData s : subscriberOnboardingData) {

			try {
				simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				Date date = simpleDateFormat.parse(s.getCreatedDate());

				dates[i] = date;
				i++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Date latestDate = getLatestDate(dates);
		String latestDateString = simpleDateFormat.format(latestDate);
		for (SubscriberOnboardingData s : subscriberOnboardingData) {
			if (s.getCreatedDate().equals(latestDateString)) {
				return s;
			}
		}
		return null;
	}

	public static Date getLatestDate(Date[] dates) {
		Date latestDate = null;
		if ((dates != null) && (dates.length > 0)) {
			for (Date date : dates) {
				if (date != null) {
					if (latestDate == null) {
						latestDate = date;
					}
					latestDate = date.after(latestDate) ? date : latestDate;
				}
			}
		}
		return latestDate;
	}

	private String getTimeStampString() throws ParseException {
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return f.format(new Date());
	}

	@Override
	public ResponseEntity<Object> getVideoLiveStreamingLocalEdms(String subscriberUid) {
		try {
			logger.info(CLASS + "getVideoLiveStreamingLocalEdms req subscriberUid {}", subscriberUid);
			if (!(Objects.isNull(subscriberUid) || subscriberUid.isEmpty())) {
				String url = livelinessRepository.getSubscriberUid(subscriberUid);
				if (!Objects.isNull(url)) {
					HttpHeaders headersForGet = new HttpHeaders();
					HttpEntity<Object> requestEntityForGet = new HttpEntity<>(headersForGet);
					ResponseEntity<Resource> downloadUrlResult = restTemplate.exchange(url, HttpMethod.GET,
							requestEntityForGet, Resource.class);

					return ResponseEntity.status(HttpStatus.OK).header("Content-Type", "video/mp4")
							.body(downloadUrlResult.getBody());
				} else {
					logger.error(CLASS + "getVideoLiveStreamingLocalEdms No video found {}", HttpStatus.NOT_FOUND);
					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.no.video.found", null, Locale.ENGLISH), null));
				}
			} else {
				logger.error(CLASS + "getVideoLiveStreamingLocalEdms Subscriber not found {}", HttpStatus.NOT_FOUND);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null));
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "saveSubscriberData Exception {}", ex.getMessage());
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(AppUtil.createApiResponse(false, messageSource.getMessage(
							"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null));
		} catch (Exception e) {
			logger.error(CLASS + "getVideoLiveStreamingLocalEdms Exception {}", e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
					.body(AppUtil.createApiResponse(false, messageSource.getMessage(
							"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null));
		}
	}

	@Override
	public ApiResponse addTrustedUsers(TrustedUserDto emails) {
		try {
			List<String> emailsListDb = trustedUserRepoIface.getTrustedEmails();
			List<String> secondList = new ArrayList<String>();
			List<TrustedUser> saveTrustedUser = new ArrayList<TrustedUser>();
			logger.info(CLASS + "addTrustedUsers emailsListDb {}", emailsListDb);
			logger.info(CLASS + "addTrustedUsers secondList {}", secondList);
			if (Objects.nonNull(emails) && !CollectionUtils.isEmpty(emails.getEmails())) {
				if (!CollectionUtils.isEmpty(emailsListDb)) {
					for (TrustedEmails trustedEmails : emails.getEmails()) {
						secondList.add(trustedEmails.getEmail());
					}

					secondList.retainAll(emailsListDb);
					if (!secondList.isEmpty()) {
						return AppUtil.createApiResponse(false, messageSource.getMessage(
								"api.error.duplicate.emails.are.present", null, Locale.ENGLISH), secondList);
					} else {

						for (TrustedEmails trustedEmails : emails.getEmails()) {
							saveTrustedUser.add(saveTrustedUsers(trustedEmails));
						}
						trustedUserRepoIface.saveAll(saveTrustedUser);
						return AppUtil.createApiResponse(true,
								messageSource.getMessage("api.response.list.save.successfully", null, Locale.ENGLISH),
								null);
					}

				} else {
					return AppUtil.createApiResponse(false, messageSource
							.getMessage("api.error.trusted.user.email.list.is.empty", null, Locale.ENGLISH), null);
				}
			} else {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.trusted.user.email.list.is.empty", null, Locale.ENGLISH),
						null);
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public TrustedUser saveTrustedUsers(TrustedEmails trustedEmails) {
		TrustedUser trustedUser = new TrustedUser();
		trustedUser.setEmailId(trustedEmails.getEmail());
		trustedUser.setFullName(trustedEmails.getName());
		trustedUser.setMobileNumber(trustedEmails.getMobileNo());
		trustedUser.setTrustedUserStatus(trustedUserStatus);
		return trustedUser;
	}

	@Override
	public ApiResponse getSubscriberDetailsReports(String startDate, String endDate) {
		try {
			logger.info(CLASS + "getSubscriberDetailsReport req startDate {} and endDate {}", startDate, endDate);
			if (startDate != null && endDate != null) {
				List<SubscriberCertificateDetails> completeDetail = subscriberCertificateDetailsRepoIface
						.getSubscriberReports(startDate, endDate);
				List<SubscriberReportsResponseDto> details = new ArrayList<>();

				if (Objects.nonNull(completeDetail) && !completeDetail.isEmpty()) {

					for (SubscriberCertificateDetails subscriberCompleteDetail : completeDetail) {
						SubscriberReportsResponseDto reportsResponseDto = new SubscriberReportsResponseDto();
						reportsResponseDto.setFullName(subscriberCompleteDetail.getFullName());
						reportsResponseDto.setIdDocNumber(subscriberCompleteDetail.getIdDocNumber());
						reportsResponseDto.setOnboardingMethod(subscriberCompleteDetail.getOnboardingMethod());
						reportsResponseDto
								.setCertificateSerialNumber(subscriberCompleteDetail.getCertificateSerialNumber());
						reportsResponseDto
								.setCertificateIssueDate(subscriberCompleteDetail.getCertificateIssueDate().toString());
						reportsResponseDto
								.setCerificateExpiryDate(subscriberCompleteDetail.getCerificateExpiryDate().toString());
						details.add(reportsResponseDto);
					}
					logger.info(
							CLASS + "getSubscriberDetailsReports Succssfully fetched subscriber certificate details {}",
							details);
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.error.successfully.fetched.subscriber.certificate.details",
									null, Locale.ENGLISH),
							details);
				} else {
					logger.info(CLASS + " getSubscriberDetailsReports No Records Found");
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.no.records.found", null, Locale.ENGLISH), null);
				}
			} else {
				logger.info(CLASS + "getSubscriberDetailsReports Date cant should be null or empty");
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.date.cant.should.be.null.or.empty", null, Locale.ENGLISH),
						null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "getSubscriberDetailsReports Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getSubscriberDetailsReports Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	int isOptionData1Present(String optionalData1) {

		int optionalDataCount = onboardingDataRepoIface.getOptionalData1(optionalData1);

		return optionalDataCount;
	}

	@Override
	public ApiResponse updatePhoneNumber(UpdateDto updateDto) {
		try {
			logger.info(CLASS + " updatePhoneNumber Suid {}", updateDto.toString());
			if (updateDto.getSuid() == null || updateDto.getSuid().isEmpty()) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriberuid.cant.be.null", null, Locale.ENGLISH), null);
			}
			if (updateDto.getMobileNumber() == null || updateDto.getMobileNumber().isEmpty()) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.mobile.number.cant.be.null.or.empty", null, Locale.ENGLISH),
						null);
			}

			Date d1 = subscriberHistoryRepo.getLatestForMobile(updateDto.getSuid());
			logger.info(CLASS + " updatePhoneNumber latest date {} ", d1);
			if (d1 != null) {
				Date d2 = AppUtil.getCurrentDate();
				long difference_In_Time = d2.getTime() - d1.getTime();
				long difference_In_Days = TimeUnit.MILLISECONDS.toDays(difference_In_Time) % 365;
				System.out.println(difference_In_Days);
				if (difference_In_Days <= 30) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage(
									"api.error.cant.change.the.phone.number.because.you.changed.it.recently", null,
									Locale.ENGLISH),
							null);
				}
			}
//			SubscriberContactHistory subscriberContactHistory1 = subscriberContactHistories.get(subscriberContactHistories.size()-1);
//			System.out.println(subscriberContactHistory1);
//			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
//			Date d1 = subscriberContactHistory1.getCreatedDate();

			Subscriber sub = subscriberRepoIface.findBymobileNumber(updateDto.getMobileNumber());

			Subscriber subscriber = subscriberRepoIface.findBysubscriberUid(updateDto.getSuid());
			if (subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
			}
			if (subscriber.getMobileNumber().equals(updateDto.getMobileNumber())) {
				return AppUtil.createApiResponse(false, messageSource.getMessage(
						"api.error.your.old.number.and.entered.mobile.number.are.same", null, Locale.ENGLISH), null);
			}
			if (sub != null) {
				return AppUtil.createApiResponse(false, messageSource
						.getMessage("api.error.this.mobile.number.is.already.in.use", null, Locale.ENGLISH), null);
			}
			// create new subHistory instance and save old records
			SubscriberContactHistory subscriberContactHistory = new SubscriberContactHistory();
			subscriberContactHistory.setSubscriberUid(subscriber.getSubscriberUid());
			subscriberContactHistory.setMobileNumber(subscriber.getMobileNumber());
			// subscriberContactHistory.setEmailId(subscriber.getEmailId());
			subscriberContactHistory.setCreatedDate(AppUtil.getCurrentDate());
			subscriberHistoryRepo.save(subscriberContactHistory);

			// update subscriber phone
			subscriber.setMobileNumber(updateDto.getMobileNumber());
			subscriberRepoIface.save(subscriber);

			return AppUtil.createApiResponse(true,
					messageSource.getMessage("api.error.phone.number.updated", null, Locale.ENGLISH), subscriber);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " updatePhoneNumber Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " updatePhoneNumber Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse updateEmail(UpdateDto updateDto) {
		try {
			logger.error(CLASS + " updatePhoneNumber Suid {}", updateDto.getSuid());
			if (updateDto.getSuid() == null || updateDto.getSuid().isEmpty()) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriberuid.cant.be.null", null, Locale.ENGLISH), null);
			}
			if (updateDto.getEmail() == null || updateDto.getEmail().isEmpty()) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.email.id.cant.be.empty", null, Locale.ENGLISH), null);
			}
			Date d1 = subscriberHistoryRepo.getLatestForEmail(updateDto.getSuid());
			if (d1 != null) {
				Date d2 = AppUtil.getCurrentDate();
				long difference_In_Time = d2.getTime() - d1.getTime();
				long difference_In_Days = TimeUnit.MILLISECONDS.toDays(difference_In_Time) % 365;
				System.out.println(difference_In_Days);
				if (difference_In_Days <= 30) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.cant.change.the.email.because.you.changed.it.recently",
									null, Locale.ENGLISH),
							null);
				}
			}
//			SubscriberContactHistory subscriberContactHistory1 = subscriberContactHistories.get(subscriberContactHistories.size()-1);
//			System.out.println(subscriberContactHistory1);
//			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			// Date d1 = subscriberContactHistory1.getCreatedDate();

			Subscriber sub = subscriberRepoIface.findByemailId(updateDto.getEmail());

			Subscriber subscriber = subscriberRepoIface.findBysubscriberUid(updateDto.getSuid());
			if (subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
			}
			if (subscriber.getEmailId().equals(updateDto.getEmail())) {
				return AppUtil.createApiResponse(false, messageSource.getMessage(
						"api.error.your.old.email.and.entered.emailId.are.same", null, Locale.ENGLISH), null);

			}
			// checking if entered mail is already in use with other subscriber
			if (sub != null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.this.email.is.already.in.use", null, Locale.ENGLISH), null);
			}

			int orgEmailCount = orgContactsEmailRepository.findByOrgEmailAndNotUgPassEmail(updateDto.getEmail(),
					subscriber.getEmailId());
			int orgMobileCount = orgContactsEmailRepository.findByOrgEmailAndNotMobile(updateDto.getEmail(),
					subscriber.getMobileNumber());
			int orgNinCount = orgContactsEmailRepository.findByOrgEmailAndNotNin(updateDto.getEmail(),
					subscriber.getIdDocNumber());
			int orgPassportCount = orgContactsEmailRepository.findByOrgEmailAndNotPassport(updateDto.getEmail(),
					subscriber.getIdDocNumber());
			if (orgEmailCount != 0 || orgPassportCount != 0 || orgMobileCount != 0 || orgNinCount != 0) {

				return AppUtil.createApiResponse(false,
						messageSource.getMessage(
								"api.error.this.email.is.already.registered.with.another.organization.subscriber.email",
								null, Locale.ENGLISH),
						null);

			}

			// create new subHistory instance and save old records
			SubscriberContactHistory subscriberContactHistory = new SubscriberContactHistory();
			subscriberContactHistory.setSubscriberUid(subscriber.getSubscriberUid());
			// subscriberContactHistory.setMobileNumber(subscriber.getMobileNumber());
			subscriberContactHistory.setEmailId(subscriber.getEmailId());
			subscriberContactHistory.setCreatedDate(AppUtil.getCurrentDate());
			subscriberHistoryRepo.save(subscriberContactHistory);

			// update subscriber email
			// subscriber.setMobileNumber(updateDto.getMobileNumber());
			subscriber.setEmailId(updateDto.getEmail());
			subscriberRepoIface.save(subscriber);

			return AppUtil.createApiResponse(true,
					messageSource.getMessage("api.response.email.updated", null, Locale.ENGLISH), subscriber);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " updatePhoneNumber Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " updatePhoneNumber Exception ", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse sendOtpEmail(UpdateOtpDto otpDto) {
		try {
			if (otpDto.getEmail().isEmpty()) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.email.id.cant.be.empty", null, Locale.ENGLISH), null);
			}
			OTPResponseDTO otpResponse = new OTPResponseDTO();
			String emailOTP = generateOtp(5);
			System.out.println("emailOTP >> " + emailOTP + " : " + AppUtil.encrypt(emailOTP));
			EmailReqDto dto = new EmailReqDto();
			dto.setEmailOtp(emailOTP);
			dto.setEmailId(otpDto.getEmail());
			dto.setTtl(timeToLive);

			ApiResponse res = sendEmailToSubscriber(dto);

			otpResponse.setMobileOTP(null);
			otpResponse.setEmailOTP(null);
			otpResponse.setTtl(timeToLive);
			otpResponse.setEmailEncrptyOTP(encryptedString(emailOTP));

			if (res.isSuccess()) {
				System.out.println("email res >> " + res.getMessage());
				System.out.println("Email Sent Successfully");
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.ok", null, Locale.ENGLISH), otpResponse);

			} else {
				System.out.println("IN Email Excption >> " + res);
				return AppUtil.createApiResponse(false, messageSource.getMessage(
						"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			logger.error(CLASS + " sendOtpEmail Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse sendOtpMobile(UpdateOtpDto otpDto) {
		try {
			if (otpDto.getMobileNumber().isEmpty()) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.mobile.number.cant.be.empty", null, Locale.ENGLISH), null);
			} else {
				Date startTime = new Date();
				String correlationId = generatecorrelationIdUniqueId();
				Object obj = null;
				OTPResponseDTO otpResponse = new OTPResponseDTO();
				ApiResponse apiResponse = null;
				String mobileOTP = generateOtp(6);
				System.out.println("mobileOTP >> " + mobileOTP + " : " + AppUtil.encrypt(mobileOTP));

				if (otpDto.getMobileNumber().startsWith("+91")) {
					if (otpDto.getMobileNumber().length() == 13) {
						System.out.println("IND");
						logger.info(CLASS + "sendOTPMobileSms req IND {}", otpDto.getMobileNumber());
						apiResponse = sendSMSIND(mobileOTP, otpDto.getMobileNumber().substring(3, 13));
						if (apiResponse.isSuccess()) {
							otpResponse.setMobileOTP(null);
							otpResponse.setEmailOTP(null);
							otpResponse.setTtl(timeToLive);
							otpResponse.setMobileEncrptyOTP(encryptedString(mobileOTP));
							return AppUtil.createApiResponse(true,
									messageSource.getMessage("api.response.ok", null, Locale.ENGLISH), otpResponse);
						}
					} else {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.phone.number.is.invalid.please.enter.correct.phone.number", null,
										Locale.ENGLISH),
								null);

					}
				} else if (otpDto.getMobileNumber().startsWith("+256")) {
					if (otpDto.getMobileNumber().length() == 13) {
						logger.info(CLASS + "sendOTPMobileSms req UGA {}", otpDto.getMobileNumber());
						ApiResponse response = sendSMSUGA(mobileOTP, otpDto.getMobileNumber(), timeToLive);
						try {
							SmsOtpResponseDTO smsOtpResponse = objectMapper.readValue(response.getResult().toString(),
									SmsOtpResponseDTO.class);
							if (smsOtpResponse.getNon_field_errors() != null) {
								return AppUtil.createApiResponse(false, smsOtpResponse.getNon_field_errors().get(0),
										null);
							} else {
								otpResponse.setMobileOTP(null);
								otpResponse.setEmailOTP(null);
								otpResponse.setTtl(timeToLive);
								otpResponse.setMobileEncrptyOTP(encryptedString(mobileOTP));
								return AppUtil.createApiResponse(true,
										messageSource.getMessage("api.response.ok", null, Locale.ENGLISH), otpResponse);
							}
						} catch (Exception e) {
							logger.error(CLASS + "sendSMSUGA IN UGA Exception {}", e.getMessage());
							e.printStackTrace();
							return AppUtil.createApiResponse(false,
									messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
											null, Locale.ENGLISH),
									null);
						}
					} else {

						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.phone.number.is.invalid.please.enter.correct.phone.number", null,
										Locale.ENGLISH),
								otpResponse);
					}
				} else if (otpDto.getMobileNumber().startsWith("+971")) {
					if (otpDto.getMobileNumber().length() == 13) {
						logger.info(CLASS + "sendOTPMobileSms req +971 {}", otpDto.getMobileNumber());
						obj = sendSMSUAE(mobileOTP, otpDto.getMobileNumber(), timeToLive);

						try {
							String sms = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
							LinkedHashMap<String, String> smsOtpResponse = objectMapper.readValue(sms,
									LinkedHashMap.class);
							if (smsOtpResponse.get("code") == "406") {
								return AppUtil.createApiResponse(false,
										messageSource.getMessage("api.error.invalid.number", null, Locale.ENGLISH),
										null);

							} else {
								otpResponse.setMobileOTP(null);
								otpResponse.setEmailOTP(null);
								otpResponse.setTtl(timeToLive);
								otpResponse.setMobileEncrptyOTP(encryptedString(mobileOTP));
								return AppUtil.createApiResponse(true,
										messageSource.getMessage("api.response.ok", null, Locale.ENGLISH), otpResponse);
							}
						} catch (Exception e) {
							e.printStackTrace();
							logger.error(CLASS + "sendSMSUAE IN UAE Exception {}", e.getMessage());
							return AppUtil.createApiResponse(false,
									messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime",
											null, Locale.ENGLISH),
									null);
						}
					} else {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.phone.number.is.invalid.please.enter.correct.phone.number", null,
										Locale.ENGLISH),
								null);

					}
				}
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.invalid.country.code", null, Locale.ENGLISH), null);

			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	private ApiResponse sendSMSINDNEW(String otp, String mobileNumber) {
		logger.info(CLASS + "sendSMSIND req  otp {} and mobileNumber {}", otp, mobileNumber);
		String smsBody = "Dear Subscriber, " + otp + " is your DigitalTrust Mobile verification one-time code";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String smsUrlWithBody = indApiSMS + "?senderid=DGTRST&channel=Trans&DCS=0&flashsms=0&number=" + mobileNumber
				+ "&text=" + smsBody + "&route=47&PEID=1301162592212041556&user=devesh.mishra@digitaltrusttech.com"
				+ "&password=DigitalTrust@20&DLTTemplateId=1307162619898313468";

		HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
		try {

			logger.info(CLASS + "sendSMSIND req for restTemplate smsUrlWithBody {} and requestEntity {}",
					smsUrlWithBody, requestEntity);

			ResponseEntity<Object> res = restTemplate.exchange(smsUrlWithBody, HttpMethod.GET, requestEntity,
					Object.class);
			String smsResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getBody());
			LinkedHashMap<String, String> indiaSmsOtpResponse = objectMapper.readValue(smsResponse,
					LinkedHashMap.class);
			if (indiaSmsOtpResponse.get("ErrorCode") == "000" || indiaSmsOtpResponse.get("ErrorCode").equals("000")) {
				logger.info(CLASS + "sendSMSIND res for restTemplate {}", indiaSmsOtpResponse);
				return AppUtil.createApiResponse(true, indiaSmsOtpResponse.get("ErrorMessage"), null);
			} else {
				return AppUtil.createApiResponse(false, indiaSmsOtpResponse.get("ErrorMessage"), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "sendSMSIND Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	private ApiResponse sendSMSIND(String otp, String mobileNumber) {
		logger.info(CLASS + "sendSMSIND req  otp {} and mobileNumber {}", otp, mobileNumber);
		String smsBody = "Dear Subscriber, " + otp + " is your DigitalTrust Mobile verification one-time code";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String smsUrlWithBody = indApiSMS
				+ "?APIKey=E2X4Ixz65kKlawWUBVUKkA&senderid=DGTRST&channel=2&DCS=0&flashsms=0&number=" + mobileNumber
				+ "&text=" + smsBody + "&route=1&dlttemplateid=1307162619898313468";

		HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
		try {

			logger.info(CLASS + "sendSMSIND req for restTemplate smsUrlWithBody {} and requestEntity {}",
					smsUrlWithBody, requestEntity);

			ResponseEntity<Object> res = restTemplate.exchange(smsUrlWithBody, HttpMethod.GET, requestEntity,
					Object.class);
			String smsResponse = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getBody());
			LinkedHashMap<String, String> indiaSmsOtpResponse = objectMapper.readValue(smsResponse,
					LinkedHashMap.class);
			if (indiaSmsOtpResponse.get("ErrorCode") == "000" || indiaSmsOtpResponse.get("ErrorCode").equals("000")) {
				logger.info(CLASS + "sendSMSIND res for restTemplate {}", indiaSmsOtpResponse);
				return AppUtil.createApiResponse(true, indiaSmsOtpResponse.get("ErrorMessage"), null);
			} else {
				return AppUtil.createApiResponse(false, indiaSmsOtpResponse.get("ErrorMessage"), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "sendSMSIND Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public ApiResponse sendSMSUGA(String otp, String mobileNumber, int timeToLive) throws ParseException {
		logger.info(CLASS + "sendSMSUGA otp {} and mobileNumber {} and timeToLive {} ", otp, mobileNumber, timeToLive);
		String url = niraApiSMS;
		String basicAuth = getBasicAuth();
		SmsDTO smsDTO = new SmsDTO();
		smsDTO.setPhoneNumber(mobileNumber);
		smsDTO.setSmsText("Dear Customer, your OTP for UgPass Registration is " + otp
				+ ", Please use this OTP to validate your Mobile number. This OTP is valid for " + timeToLive
				+ " Seconds - UgPass System");
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("daes-authorization", basicAuth);
		headers.set("access_token", getToken());
		HttpEntity<Object> requestEntity = new HttpEntity<>(smsDTO, headers);
		try {
			logger.info(CLASS + " sendSMSUGA req for restTemplate url {} and requestEntity {} ", url, requestEntity);
			ResponseEntity<ApiResponse> res = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					ApiResponse.class);
			ApiResponse api = res.getBody();
			logger.info("sendSMSUGA res for restTemplate {}", res);
			return api;
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error(CLASS + "sendSMSUGA Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public Object sendSMSUAE(String otp, String mobileNumber, int timeToLive) throws ParseException {
		logger.info("sendSMSUAE  otp {} and mobileNumber {} and timeToLive {}", otp, mobileNumber, timeToLive);
		String url = uaeApiSMS;
		String text = "Your ICA-Pass OTP Phone verification code  is " + otp + "The code is valid for " + timeToLive
				+ " seconds. Don't share this code with anyone.";

		Map<String, String> uaeSmsBody = new HashMap<String, String>();
		uaeSmsBody.put("mobileno", mobileNumber);
		uaeSmsBody.put("smstext", text);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		System.out.println("getToken() :: " + getToken());
		headers.set("access_token", getToken());
		HttpEntity<Object> requestEntity = new HttpEntity<>(uaeSmsBody, headers);
		try {
			logger.info(CLASS + "sendSMSUAE req for restTemplate url {} and requestEntity {} ", url, requestEntity);
			ResponseEntity<Object> res = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Object.class);
			ApiResponse api = new ApiResponse();
			api.setSuccess(true);
			api.setMessage("");
			api.setResult(res.getBody());
			logger.info(CLASS + "sendSMSUAE res for restTemplate {}", res);
			return api.getResult();
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			logger.error("sendSMSUAE Exception {}", e.getMessage());
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public String getBasicAuth() {
		String userCredentials = niraUserName + ":" + niraPassword;
		String basicAuth = new String(Base64.getEncoder().encode(userCredentials.getBytes()));
		return basicAuth;
	}

	public String getToken() {
		String url = niraApiToken;
		logger.info(CLASS + "getToken req url {}", url);
		String basicAuth = getBasicAuth();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("daes-authorization", basicAuth);
		HttpEntity<Object> requestEntity = new HttpEntity<>(headers);
		try {
			logger.info(CLASS + "getToken req for restTemplate {}", requestEntity);
			ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
			logger.info(CLASS + "getToken res for restTemplate {}", res);
			return res.getBody();
		}

		catch (Exception e) {
			logger.error(CLASS + "getToken Exception {}", e.getMessage());
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String generatecorrelationIdUniqueId() {
		UUID correlationID = UUID.randomUUID();
		return correlationID.toString();
	}

	public String generateOtp(int maxLength) {
		try {
			SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
			StringBuilder otp = new StringBuilder(maxLength);

			for (int i = 0; i < maxLength; i++) {
				otp.append(secureRandom.nextInt(9));
			}
			return otp.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public ApiResponse sendEmailToSubscriber(EmailReqDto emailReqDto) {
		try {
			String url = emailBaseUrl;
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Object> requestEntity = new HttpEntity<>(emailReqDto, headers);
			System.out.println("requestEntity >> " + requestEntity);
			ResponseEntity<ApiResponse> res = restTemplate.exchange(url, HttpMethod.POST, requestEntity,
					ApiResponse.class);
			System.out.println("res >> " + res);
			if (res.getStatusCodeValue() == 200) {
				return AppUtil.createApiResponse(true, res.getBody().getMessage(), res);
			} else if (res.getStatusCodeValue() == 400) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.bad.request", null, Locale.ENGLISH), null);
			} else if (res.getStatusCodeValue() == 500) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.internal.server.error", null, Locale.ENGLISH), null);
			}
			return AppUtil.createApiResponse(false, res.getBody().getMessage(), null);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}

		catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}

	}

	@Override
	public ApiResponse reOnboardAddSubscriberObData(SubscriberObRequestDTO obRequestDTO) throws Exception {
		try {
			SubscriberOnboardingData subscriberOnboardingData = onboardingDataRepoIface
					.findLatestSubscriber(obRequestDTO.getSuID());
			System.out.println(" obRequestDTO " + obRequestDTO);
			Date d2 = AppUtil.getCurrentDate();
			String oldExpireDate = subscriberOnboardingData.getDateOfExpiry();
			String docDate = subscriberOnboardingData.getCreatedDate();

			Date d1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(docDate);
			System.out.println("noe date "+d2);
			// Date oldExpiryDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
			// Locale.ENGLISH).parse(oldExpireDate);
			System.out.println("date in table::::::"+d1);
			System.out.println(d1.getTime());
			long difference_In_Time = d2.getTime() - d1.getTime();
			System.out.println(difference_In_Time);
//			long difference_In_Days = TimeUnit.MILLISECONDS.toDays(difference_In_Time) % 365;
			long difference_In_Days = TimeUnit.MILLISECONDS.toDays(difference_In_Time);
			System.out.println(difference_In_Days);
			System.out.println(expiryDays);

			logger.info(CLASS + "reOnboardAddSubscriberObData request {}", obRequestDTO);
			if (obRequestDTO == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.reonboarding.request.cant.be.null", null, Locale.ENGLISH),
						null);
			} else {
				if (obRequestDTO.getSuID() == null) {
					return AppUtil.createApiResponse(false, messageSource
							.getMessage("api.error.subscriber.unique.id.cant.be.null", null, Locale.ENGLISH), null);
				}

				Subscriber subscriberData = subscriberRepoIface.findBysubscriberUid(obRequestDTO.getSuID());
				if (subscriberData == null) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
				}

				SubscriberObData subscriberObData = obRequestDTO.getSubscriberData();

				// check gender must be same
				if (checkGender) {
					if (!subscriberObData.getGender().equals(subscriberOnboardingData.getGender())) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.gender.must.be.same", null, Locale.ENGLISH), null);
					}
				}

				// check date of birth must be same
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				System.out.println(" dob " + subscriberData.getDateOfBirth());

				Date oldDateDOB = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
						.parse(subscriberData.getDateOfBirth());

				String dob = sdf.format(oldDateDOB);

				Date newdateDOB = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
						.parse(subscriberObData.getDateOfBirth());
				String reOnboardDOB = sdf.format(newdateDOB);
				if (checkDateOfBirth) {
					if (!reOnboardDOB.equals(dob)) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.date.of.birth.must.be.same", null, Locale.ENGLISH),
								null);
					}
				}

				// check id document number must be different
				if (subscriberObData.getDocumentNumber() == null) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.id.document.number.cant.be.null", null, Locale.ENGLISH),
							null);
				}
				System.out.println("Old Expiry date " + oldExpireDate);
				String latest = AppUtil.getDate().toString();
				System.out.println("Latest : " + latest);
				// check expiry date of old id. if expired allow to update

				Subscriber subscriber2 = subscriberRepoIface.findbyDocumentNumber(subscriberObData.getDocumentNumber());
				if (subscriber2 != null) {
					if (!subscriber2.getSubscriberUid().equals(obRequestDTO.getSuID())) {
						return AppUtil.createApiResponse(false, messageSource.getMessage(
								"api.error.this.document.is.already.onboarded", null, Locale.ENGLISH), null);
					}
				}

				if (oldExpireDate.compareTo(latest) < 0) {
					System.out.println(" oldExpireDate.compareTo(latest) if ");
					DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

					// new document date of expiry
					String expiryDateTime = subscriberObData.getDateOfExpiry().toString();
					LocalDateTime newExpiryDate = LocalDateTime.parse(expiryDateTime, dateTimeFormatter);

					// new current date and time
					String localDateTime = AppUtil.getDate().toString();
					LocalDateTime currentDateTime = LocalDateTime.parse(localDateTime, dateTimeFormatter);

					if (newExpiryDate.isAfter(currentDateTime)) {
						long daysBetween = Duration.between(currentDateTime, newExpiryDate).toDays();
						System.out.println("Days: " + daysBetween);
						if (daysBetween <= 1) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.cant.do.reonboard.because.your.document.date.of.expiry.is.less.then.days",
									null, Locale.ENGLISH), null);
						}

					} else if (newExpiryDate.isBefore(currentDateTime)) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.the.expiry.date.with.time.is.before.the.current.date.with.time",
										null, Locale.ENGLISH),
								null);
					} else {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.the.expiry.date.with.time.is.the.same.as.the.current.date.with.time",
										null, Locale.ENGLISH),
								null);
					}

					// check LOA level
					// String loa = subscriberView.getLoa();
					String loa = subscriberOnboardingData.getLevelOfAssurance();

					if (loa.equals(Constant.LOA1)) {
						// if(obRequestDTO.getOnboardingMethod().equals("UNID") ||
						// obRequestDTO.getOnboardingMethod().equals("UNID"))
					} else if (loa.equals(Constant.LOA2)) {
						if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
						}
					} else if (loa.equals(Constant.LOA3)) {
						if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
						}
						if (obRequestDTO.getOnboardingMethod().equals(Constant.PASSPORT)) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
						}
					}
					return addSubscriberObData(obRequestDTO);

				} else {
					System.out.println(" oldExpireDate.compareTo(latest) inside else ");

					if (difference_In_Days < expiryDays && !obRequestDTO.getSubscriberData().getDocumentNumber()
							.equals(subscriberOnboardingData.getIdDocNumber())) {
						return AppUtil.createApiResponse(false,
								"We can't processed. it's seem your last updation of your id document is less than "
										+ expiryDays + " days.",
								null);
					} else {

						if (checkDocumentNumber) {
							SubscriberView subscriberCertRevoked = subscriberViewRepoIface
									.findSubscriberByDocIdCertRevoked(subscriberObData.getDocumentNumber());
							if (subscriberCertRevoked != null) {
								if (subscriberObData.getDocumentNumber()
										.equals(subscriberCertRevoked.getIdDocNumber())) {
									return AppUtil.createApiResponse(false,
											messageSource.getMessage("api.error.id.document.number.must.be.different",
													null, Locale.ENGLISH),
											null);
								}
							}
						}

						DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

						// new document date of expiry
						String expiryDateTime = subscriberObData.getDateOfExpiry().toString();
						LocalDateTime newExpiryDate = LocalDateTime.parse(expiryDateTime, dateTimeFormatter);

						// new current date and time
						String localDateTime = AppUtil.getDate().toString();
						LocalDateTime currentDateTime = LocalDateTime.parse(localDateTime, dateTimeFormatter);

						if (newExpiryDate.isAfter(currentDateTime)) {
							long daysBetween = Duration.between(currentDateTime, newExpiryDate).toDays();
							System.out.println("Days: " + daysBetween);
							if (daysBetween <= 1) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.cant.do.reonboard.because.your.document.date.of.expiry.is.less.then.days",
										null, Locale.ENGLISH), null);
							}

						} else if (newExpiryDate.isBefore(currentDateTime)) {
							return AppUtil.createApiResponse(false,
									messageSource.getMessage(
											"api.error.the.expiry.date.with.time.is.before.the.current.date.with.time",
											null, Locale.ENGLISH),
									null);
						} else {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.the.expiry.date.with.time.is.the.same.as.the.current.date.with.time",
									null, Locale.ENGLISH), null);
						}

						// check LOA level
						String loa = subscriberOnboardingData.getLevelOfAssurance();

						if (loa.equals(Constant.LOA1)) {
							// if(obRequestDTO.getOnboardingMethod().equals("UNID") ||
							// obRequestDTO.getOnboardingMethod().equals("UNID"))
						} else if (loa.equals(Constant.LOA2)) {
							if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
							}
						} else if (loa.equals(Constant.LOA3)) {
							if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
							}
							if (obRequestDTO.getOnboardingMethod().equals(Constant.PASSPORT)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
							}
						}
						return addSubscriberObData(obRequestDTO);
					}
				}

			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			sentryClientExceptions.captureTags(obRequestDTO.getSuID(), null, "reOnboardAddSubscriberObData",
					"SubscriberController");
			sentryClientExceptions.captureExceptions(e);
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			sentryClientExceptions.captureTags(obRequestDTO.getSuID(), null, "reOnboardAddSubscriberObData",
					"SubscriberController");
			sentryClientExceptions.captureExceptions(e);
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse deleteRecord(String mobileNo, String email) {
		try {
			if (!mobileNo.equals("")) {
				Optional<Subscriber> subscriber = Optional
						.ofNullable(subscriberRepoIface.findBymobileNumber("+" + mobileNo));// subscriberRepoIface.findByemailId(email);
				if (subscriber.isPresent()) {
					String suid = subscriber.get().getSubscriberUid();
					int a = subscriberRepoIface.deleteRecordBySubscriberUid(suid);
					if (a == 1) {
						return AppUtil.createApiResponse(true, messageSource.getMessage(
								"api.response.subscriber.record.deleted.successfully", null, Locale.ENGLISH), null);
					} else {
						return AppUtil.createApiResponse(false, messageSource.getMessage(
								"api.error.subscriber.record.not.deleted.successfully", null, Locale.ENGLISH), null);
					}
				}
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);

			} else {
				Optional<Subscriber> subscriber = Optional.ofNullable(subscriberRepoIface.findByemailId(email));// subscriberRepoIface.findByemailId(email);
				if (subscriber.isPresent()) {
					String suid = subscriber.get().getSubscriberUid();
					int a = subscriberRepoIface.deleteRecordBySubscriberUid(suid);
					if (a == 1) {
						return AppUtil.createApiResponse(true, messageSource.getMessage(
								"api.response.subscriber.record.deleted.successfully", null, Locale.ENGLISH), null);
					} else {
						return AppUtil.createApiResponse(false, messageSource.getMessage(
								"api.error.subscriber.record.not.deleted.successfully", null, Locale.ENGLISH), null);
					}
				}
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getDeviceStatus(HttpServletRequest httpServletRequest) {
		try {
			String deviceId = httpServletRequest.getHeader("deviceId");
			if (deviceId == null || deviceId.equals("")) {
				return AppUtil.createApiResponse(true, messageSource
						.getMessage("api.error.deviceid.not.coming.please.send.deviceid", null, Locale.ENGLISH), null);
			}

			// SubscriberDevice subscriberDevice =
			// deviceRepoIface.findBydeviceUid(deviceId);
			Optional<SubscriberDevice> subscriberDevice = Optional
					.ofNullable(deviceRepoIface.findBydeviceUid(deviceId));
			DeviceStatusDto deviceStatusDto = new DeviceStatusDto();
			if (subscriberDevice.isPresent()) {
				SubscriberFcmToken subscriberFcmToken = fcmTokenRepoIface
						.findBysubscriberUid(subscriberDevice.get().getSubscriberUid());
				deviceStatusDto.setFcmToken(subscriberFcmToken.getFcmToken());
				deviceStatusDto.setDeviceStatus(subscriberDevice.get().getDeviceStatus());
				if (subscriberDevice.get().getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {
					deviceStatusDto.setConsentRequired(false);
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.device.status.is.disabled", null, Locale.ENGLISH),
							deviceStatusDto);
				} else {
					SubscriberStatus subscriberStatus = statusRepoIface
							.findBysubscriberUid(subscriberDevice.get().getSubscriberUid());
					if (signRequired) {
						if (subscriberStatus.getSubscriberStatus().equals("ACTIVE")) {
							ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
							if (consentHistory == null) {
								deviceStatusDto.setConsentRequired(false);
							} else {
								SubscriberConsents subscriberConsents = subscriberConsentsRepo
										.findSubscriberConsentBySuidAndConsentId(
												subscriberDevice.get().getSubscriberUid(), consentHistory.getId());
								if (subscriberConsents == null) {
									deviceStatusDto.setConsentRequired(true);
								} else {
									deviceStatusDto.setConsentRequired(false);
								}
							}
						} else {
							deviceStatusDto.setConsentRequired(false);
						}
					} else {
						ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
						if (consentHistory == null) {
							deviceStatusDto.setConsentRequired(false);
						} else {
							SubscriberConsents subscriberConsents = subscriberConsentsRepo
									.findSubscriberConsentBySuidAndConsentId(subscriberDevice.get().getSubscriberUid(),
											consentHistory.getId());
							if (subscriberConsents == null) {
								deviceStatusDto.setConsentRequired(true);
							} else {
								deviceStatusDto.setConsentRequired(false);
							}
						}
					}
					System.out.println(deviceStatusDto.isConsentRequired());
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.device.status", null, Locale.ENGLISH),
							deviceStatusDto);
				}
			} else {
				Optional<SubscriberDeviceHistory> subscriberDeviceHistory = Optional
						.ofNullable(subscriberDeviceHistoryRepoIface.findBydeviceUid(deviceId));
				if (subscriberDeviceHistory.isPresent()) {
					deviceStatusDto.setDeviceStatus(subscriberDeviceHistory.get().getDeviceStatus());
					deviceStatusDto.setConsentRequired(false);
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.device.status.is.disabled", null, Locale.ENGLISH),
							deviceStatusDto);
				} else {
					deviceStatusDto.setConsentRequired(false);
					deviceStatusDto.setDeviceStatus(Constant.NEW_DEVICE);
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.device.status", null, Locale.ENGLISH),
							deviceStatusDto);
				}

			}
//			return AppUtil.createApiResponse(true,
//					messageSource.getMessage("api.response.device.status", null, Locale.ENGLISH), deviceStatusDto);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}

	}

	// @Override
	public ApiResponse reOnboardAddSubscriberObDataOLD(SubscriberObRequestDTO obRequestDTO) throws Exception {
		try {
			SubscriberOnboardingData subscriberOnboardingData = onboardingDataRepoIface
					.findLatestSubscriber(obRequestDTO.getSuID());
			Date d2 = AppUtil.getCurrentDate();
			String oldExpireDate = subscriberOnboardingData.getDateOfExpiry();
			String docDate = subscriberOnboardingData.getCreatedDate();
			Date d1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(docDate);
			Date oldExpiryDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(oldExpireDate);
			long difference_In_Time = d2.getTime() - d1.getTime();
			long difference_In_Days = TimeUnit.MILLISECONDS.toDays(difference_In_Time) % 365;

//			if (difference_In_Days < expiryDays && !obRequestDTO.getSubscriberData().getDocumentNumber().equals(subscriberOnboardingData.getIdDocNumber())) {
//				return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.document.update.restricted", null, Locale.ENGLISH), null);
//			}

			logger.info(CLASS + "reOnboardAddSubscriberObData request {}", obRequestDTO);
			if (obRequestDTO == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.reonboarding.request.cant.be.null", null, Locale.ENGLISH),
						null);
			} else {
				if (obRequestDTO.getSuID() == null) {
					return AppUtil.createApiResponse(false, messageSource
							.getMessage("api.error.subscriber.unique.id.cant.be.null", null, Locale.ENGLISH), null);
				}

				SubscriberView subscriberView = subscriberViewRepoIface.findSubscriberDetails(obRequestDTO.getSuID());
				if (subscriberView == null) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
				}
				SubscriberObData subscriberObData = obRequestDTO.getSubscriberData();

				// check gender must be same
				if (checkGender) {
					if (!subscriberObData.getGender().equals(subscriberView.getGender())) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.gender.must.be.same", null, Locale.ENGLISH), null);
					}
				}

				// check date of birth must be same
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				String dob = sdf.format(subscriberView.getDateOfBirth());
				Date dateDOB = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
						.parse(subscriberObData.getDateOfBirth());
				String reOnboardDOB = sdf.format(dateDOB);
				if (checkDateOfBirth) {
					if (!reOnboardDOB.equals(dob)) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.date.of.birth.must.be.same", null, Locale.ENGLISH),
								null);
					}
				}

				// check id document number must be different
				if (subscriberObData.getDocumentNumber() == null) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.id.document.number.cant.be.null", null, Locale.ENGLISH),
							null);
				}
				System.out.println("Old Expiry date " + oldExpireDate);
				String latest = AppUtil.getDate().toString();
				System.out.println("Latest : " + latest);
				// check expiry date of old id. if expired allow to update
				if (oldExpireDate.compareTo(latest) < 0) {
					List<SubscriberView> subscriber = subscriberViewRepoIface
							.findSubscriberByDocId(subscriberObData.getDocumentNumber());
					// check document number is not present in subscriber table
					// if present then same subscriber can used his document
					if (subscriber.size() != 0) {
						return AppUtil.createApiResponse(false, messageSource.getMessage(
								"api.error.this.document.is.already.onboarded", null, Locale.ENGLISH), null);
					}

					DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

					// new document date of expiry
					String expiryDateTime = subscriberObData.getDateOfExpiry().toString();
					LocalDateTime newExpiryDate = LocalDateTime.parse(expiryDateTime, dateTimeFormatter);

					// new current date and time
					String localDateTime = AppUtil.getDate().toString();
					LocalDateTime currentDateTime = LocalDateTime.parse(localDateTime, dateTimeFormatter);

					if (newExpiryDate.isAfter(currentDateTime)) {
						long daysBetween = Duration.between(currentDateTime, newExpiryDate).toDays();
						System.out.println("Days: " + daysBetween);
						if (daysBetween <= 1) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.cant.do.reonboard.because.your.document.date.of.expiry.is.less.then.days",
									null, Locale.ENGLISH), null);
						}

					} else if (newExpiryDate.isBefore(currentDateTime)) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.the.expiry.date.with.time.is.before.the.current.date.with.time",
										null, Locale.ENGLISH),
								null);
					} else {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.the.expiry.date.with.time.is.the.same.as.the.current.date.with.time",
										null, Locale.ENGLISH),
								null);
					}

					// check LOA level
					String loa = subscriberView.getLoa();

					if (loa.equals(Constant.LOA1)) {
						// if(obRequestDTO.getOnboardingMethod().equals("UNID") ||
						// obRequestDTO.getOnboardingMethod().equals("UNID"))
					} else if (loa.equals(Constant.LOA2)) {
						if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
						}
					} else if (loa.equals(Constant.LOA3)) {
						if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
						}
						if (obRequestDTO.getOnboardingMethod().equals(Constant.PASSPORT)) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
						}
					}
					return addSubscriberObData(obRequestDTO);

				} else {

					if (difference_In_Days < expiryDays && !obRequestDTO.getSubscriberData().getDocumentNumber()
							.equals(subscriberOnboardingData.getIdDocNumber())) {
						return AppUtil.createApiResponse(false,
								"We can't processed. it's seem your last updation of your id document is less than "
										+ expiryDays + " days.",
								null);
					} else {
						List<SubscriberView> subscriber = subscriberViewRepoIface
								.findSubscriberByDocId(subscriberObData.getDocumentNumber());
						if (subscriber.size() != 0) {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.this.document.is.already.onboarded", null, Locale.ENGLISH), null);
						}
						if (checkDocumentNumber) {
							SubscriberView subscriberCertRevoked = subscriberViewRepoIface
									.findSubscriberByDocIdCertRevoked(subscriberObData.getDocumentNumber());
							if (subscriberCertRevoked != null) {
								if (subscriberObData.getDocumentNumber()
										.equals(subscriberCertRevoked.getIdDocNumber())) {
									return AppUtil.createApiResponse(false,
											messageSource.getMessage("api.error.id.document.number.must.be.different",
													null, Locale.ENGLISH),
											null);
								}
							}

						}

						DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

						// new document date of expiry
						String expiryDateTime = subscriberObData.getDateOfExpiry().toString();
						LocalDateTime newExpiryDate = LocalDateTime.parse(expiryDateTime, dateTimeFormatter);

						// new current date and time
						String localDateTime = AppUtil.getDate().toString();
						LocalDateTime currentDateTime = LocalDateTime.parse(localDateTime, dateTimeFormatter);

						if (newExpiryDate.isAfter(currentDateTime)) {
							long daysBetween = Duration.between(currentDateTime, newExpiryDate).toDays();
							System.out.println("Days: " + daysBetween);
							if (daysBetween <= 1) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.cant.do.reonboard.because.your.document.date.of.expiry.is.less.then.days",
										null, Locale.ENGLISH), null);
							}

						} else if (newExpiryDate.isBefore(currentDateTime)) {
							return AppUtil.createApiResponse(false,
									messageSource.getMessage(
											"api.error.the.expiry.date.with.time.is.before.the.current.date.with.time",
											null, Locale.ENGLISH),
									null);
						} else {
							return AppUtil.createApiResponse(false, messageSource.getMessage(
									"api.error.the.expiry.date.with.time.is.the.same.as.the.current.date.with.time",
									null, Locale.ENGLISH), null);
						}

						// check LOA level
						String loa = subscriberView.getLoa();

						if (loa.equals(Constant.LOA1)) {
							// if(obRequestDTO.getOnboardingMethod().equals("UNID") ||
							// obRequestDTO.getOnboardingMethod().equals("UNID"))
						} else if (loa.equals(Constant.LOA2)) {
							if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
							}
						} else if (loa.equals(Constant.LOA3)) {
							if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
							}
							if (obRequestDTO.getOnboardingMethod().equals(Constant.PASSPORT)) {
								return AppUtil.createApiResponse(false, messageSource.getMessage(
										"api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
							}
						}
						return addSubscriberObData(obRequestDTO);
					}
				}
				// List<SubscriberOnboardingData> subscriberOnboardingData =
				// onboardingDataRepoIface.findSubscriberByDocId(subscriberObData.getDocumentNumber());
//                List<SubscriberView> subscriber = subscriberViewRepoIface
//                        .findSubscriberByDocId(subscriberObData.getDocumentNumber());
//                if (subscriber.size() != 0) {
//                    return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.this.document.is.already.onboarded", null, Locale.ENGLISH), null);
//                }
//                if (checkDocumentNumber) {
//                    SubscriberView subscriberCertRevoked =
//                            subscriberViewRepoIface.findSubscriberByDocIdCertRevoked(subscriberObData.getDocumentNumber());
//                    if (subscriberCertRevoked != null) {
//                        if (subscriberObData.getDocumentNumber().equals(subscriberCertRevoked.getIdDocNumber())) {
//                            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.id.document.number.must.be.different", null, Locale.ENGLISH), null);
//                        }
//                    }
//
//                }
//
//
//                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//                // new document date of expiry
//                String expiryDateTime = subscriberObData.getDateOfExpiry().toString();
//                LocalDateTime newExpiryDate = LocalDateTime.parse(expiryDateTime, dateTimeFormatter);
//
//                // new current date and time
//                String localDateTime = AppUtil.getDate().toString();
//                LocalDateTime currentDateTime = LocalDateTime.parse(localDateTime, dateTimeFormatter);
//
//                if (newExpiryDate.isAfter(currentDateTime)) {
//                    long daysBetween = Duration.between(currentDateTime, newExpiryDate).toDays();
//                    System.out.println("Days: " + daysBetween);
//                    if (daysBetween <= 1) {
//                        return AppUtil.createApiResponse(false,
//                                messageSource.getMessage("api.error.you.cant.do.reonboard.because.your.document.date.of.expiry.is.less.then.days", null, Locale.ENGLISH),
//                                null);
//                    }
//
//                } else if (newExpiryDate.isBefore(currentDateTime)) {
//                    return AppUtil.createApiResponse(false,
//                            messageSource.getMessage("api.error.the.expiry.date.with.time.is.before.the.current.date.with.time", null, Locale.ENGLISH), null);
//                } else {
//                    return AppUtil.createApiResponse(false,
//                            messageSource.getMessage("api.error.the.expiry.date.with.time.is.the.same.as.the.current.date.with.time", null, Locale.ENGLISH), null);
//                }
//
//                // check LOA level
//                String loa = subscriberView.getLoa();
//
//                if (loa.equals(Constant.LOA1)) {
//                    // if(obRequestDTO.getOnboardingMethod().equals("UNID") ||
//                    // obRequestDTO.getOnboardingMethod().equals("UNID"))
//                } else if (loa.equals(Constant.LOA2)) {
//                    if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
//                        return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
//                    }
//                } else if (loa.equals(Constant.LOA3)) {
//                    if (obRequestDTO.getOnboardingMethod().equals(Constant.UNID)) {
//                        return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
//                    }
//                    if (obRequestDTO.getOnboardingMethod().equals(Constant.PASSPORT)) {
//                        return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.you.are.using.low.level.of.assurance", null, Locale.ENGLISH), null);
//                    }
//                }
//                return addSubscriberObData(obRequestDTO);
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getSubscriberDetailsBySerachType(String searchType, String searchValue) {
		try {
			logger.info(CLASS + "getSubscriberDetailsBySerachType request searchType and searchValue {},{}", searchType,
					searchValue);
			Subscriber subscriber = null;
			SubscriberDeviceUpdateDto subscriberDeviceUpdateDto = new SubscriberDeviceUpdateDto();
			switch (searchType) {
			case "emailId":
				subscriber = subscriberRepoIface.findByemailId(searchValue);
				break;
			case "mobileNumber":
				subscriber = subscriberRepoIface.findBymobileNumber(searchValue);
				break;
			default:
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.bad.request", null, Locale.ENGLISH), null);
			}
			if (subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.details.not.found", null, Locale.ENGLISH), null);
			} else {

				SubscriberStatus subscriberStatus = statusRepoIface.findBysubscriberUid(subscriber.getSubscriberUid());

				SubscriberDevice subscriberDevice = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());
				subscriberDeviceUpdateDto.setSubscriberUid(subscriber.getSubscriberUid());
				subscriberDeviceUpdateDto.setFullName(subscriber.getFullName());
				subscriberDeviceUpdateDto.setDateOfBirth(subscriber.getDateOfBirth());
				subscriberDeviceUpdateDto.setIdDocType(subscriber.getIdDocType());
				subscriberDeviceUpdateDto.setIdDocNumber(subscriber.getIdDocNumber());
				subscriberDeviceUpdateDto.seteMail(subscriber.getEmailId());
				subscriberDeviceUpdateDto.setMobileNumber(subscriber.getMobileNumber());
				subscriberDeviceUpdateDto.setOsName(subscriber.getOsName());
				subscriberDeviceUpdateDto.setAppVersion(subscriber.getAppVersion());
				subscriberDeviceUpdateDto.setOsVersion(subscriber.getOsVersion());
				subscriberDeviceUpdateDto.setDeviceInfo(subscriber.getDeviceInfo());

				subscriberDeviceUpdateDto.setCreatedDate(subscriber.getCreatedDate());
				subscriberDeviceUpdateDto.setUpdatedDate(subscriber.getUpdatedDate());

				subscriberDeviceUpdateDto.setSubscriberStatus(subscriberStatus.getSubscriberStatus());

				subscriberDeviceUpdateDto.setDeviceUid(subscriberDevice.getDeviceUid());
				subscriberDeviceUpdateDto.setDeviceStatus(subscriberDevice.getDeviceStatus());

				// SusbcriberDetailsView subscriberDetails =
				// susbcriberDetailsRepository.findBysubscriberUid(subscriber.getSubscriberUid());
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.subscriber.details", null, Locale.ENGLISH),
						subscriberDeviceUpdateDto);
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();

			logger.error(CLASS + " getSubscriberDetailsBySerachType Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getSubscriberDetailsBySerachType Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse updateSusbcriberDeviceStatus(String suid) {
		try {
			logger.info(CLASS + "updateSusbcriberDeviceStatus request suid {}", suid);
			if (suid == null || suid == "") {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.unique.id.cant.be.null", null, Locale.ENGLISH),
						null);
			} else {
				SubscriberDevice subscriberDevice = deviceRepoIface.getSubscriberDeviceStatus(suid);
				if (subscriberDevice == null) {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.subscriber.details.not.found", null, Locale.ENGLISH),
							null);
				} else {
					subscriberDevice.setDeviceStatus(Constant.DEVICE_STATUS_DISABLED);
					subscriberDevice.setUpdatedDate(AppUtil.getDate());
					deviceRepoIface.save(subscriberDevice);
					return AppUtil.createApiResponse(true, messageSource
							.getMessage("api.response.subscriber.device.status.updated", null, Locale.ENGLISH), null);
				}
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();

			logger.error(CLASS + " updateSusbcriberDeviceStatus Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " updateSusbcriberDeviceStatus Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getSubscriberListBySerachType(String searchType, String searchValue) {
		try {
			logger.info(CLASS + "getSubscriberDetailsBySerachType request searchType and searchValue {},{}", searchType,
					searchValue);
			List<String> subscriberList = null;
			switch (searchType) {
			case "emailId":
				subscriberList = subscriberRepoIface.getSubscriberListByEmailId(searchValue);
				break;
			case "mobileNumber":
				subscriberList = subscriberRepoIface.getSubscriberListByMobileNo(searchValue);
				break;
			default:
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.bad.request", null, Locale.ENGLISH), null);
			}
			if (subscriberList == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.details.not.found", null, Locale.ENGLISH), null);
			} else {

				String jsonToString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(subscriberList);
				// return jsonToString;
				// SusbcriberDetailsView subscriberDetails =
				// susbcriberDetailsRepository.findBysubscriberUid(subscriber.getSubscriberUid());
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.subscriber.details", null, Locale.ENGLISH),
						jsonToString);
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();

			logger.error(CLASS + " getSubscriberDetailsBySerachType Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + " getSubscriberDetailsBySerachType Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse updateFcmTokenDetails(String suid, String fcmToken) {
		try {
			Date startTime = new Date();
			if (suid != null && suid != "") {
				if (fcmToken != null && fcmToken != "") {
					SubscriberFcmToken subscriberFcmToken = fcmTokenRepoIface.findBysubscriberUid(suid);
					if (subscriberFcmToken != null) {
						String message = "OLD FCMTOKEN | " + subscriberFcmToken.getFcmToken() + " NEW FCMTOKEN |"
								+ fcmToken;
						subscriberFcmToken.setFcmToken(fcmToken);
						fcmTokenRepoIface.save(subscriberFcmToken);
						Date endTime = new Date();
						// double toatlTime = AppUtil.getDifferenceInSeconds(startTime, endTime);
						logModelServiceImpl.setLogModelFCMToken(true, suid, null, "OTHER", null, message, startTime,
								endTime, null);
						return AppUtil.createApiResponse(true, messageSource
								.getMessage("api.response.fcmtoken.updated.successfully", null, Locale.ENGLISH),
								subscriberFcmToken);
					} else {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
					}

				} else {
					return AppUtil.createApiResponse(false,
							messageSource.getMessage("api.error.fcmtoken.cant.be.null.or.empty", null, Locale.ENGLISH),
							null);
				}
			} else {
				return AppUtil.createApiResponse(false, messageSource
						.getMessage("api.error.subscriber.suid.cantbe.null.or.empty", null, Locale.ENGLISH), null);
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getSubDetailsBySerachType(HttpServletRequest httpServletRequest, String searchType,
			String searchValue) {
		try {
			logger.info(CLASS + "getSubscriberDetailsBySerachType request searchType and searchValue {},{}", searchType,
					searchValue);
			// GetSubscriberObDataDTO getSubscriberObDataDTO = new GetSubscriberObDataDTO();
			MobileOTPDto mobileOTPDto = new MobileOTPDto();
			DeviceInfo deviceInfo = new DeviceInfo();
			Subscriber subscriber = null;

			switch (searchType) {
			case "emailId":
				subscriber = subscriberRepoIface.findByemailId(searchValue);
				break;
			case "mobileNumber":
				subscriber = subscriberRepoIface.findBymobileNumber(searchValue);
				break;
			case "idDocNumber":
				subscriber = subscriberRepoIface.findByIdDocNumber(searchValue);
				break;
			case "nationalId":
				subscriber = subscriberRepoIface.findByNationalId(searchValue);
				break;
			default:
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.bad.request", null, Locale.ENGLISH), null);
			}

			if (subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.details.not.found", null, Locale.ENGLISH), null);
			} else {
				// String deviceUid = httpServletRequest.getHeader("deviceId");
				// SubscriberDevice subscriberDeviceDetails =
				// deviceRepoIface.findBydeviceUid(httpServletRequest.getHeader("deviceId"));

				deviceInfo.setDeviceId(httpServletRequest.getHeader("deviceId"));
				deviceInfo.setAppVersion(httpServletRequest.getHeader("appVersion"));
				deviceInfo.setOsVersion(httpServletRequest.getHeader("osVersion"));

				mobileOTPDto.setSubscriberEmail(subscriber.getEmailId());
				mobileOTPDto.setSubscriberMobileNumber(subscriber.getMobileNumber());
				ApiResponse apiResponse = deviceUpdateIface.validateSubscriberAndDevice(deviceInfo, mobileOTPDto);
				if (apiResponse.isSuccess()) {
					return AppUtil.createApiResponse(true,
							messageSource.getMessage("api.response.subscriber.details", null, Locale.ENGLISH),
							apiResponse.getResult());
				} else {
					return AppUtil.createApiResponse(false, apiResponse.getMessage(), null);
				}

			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getSusbcriberDeviceHistory(String suid) {
		try {

			Subscriber subscriber = subscriberRepoIface.findBysubscriberUid(suid);
			if (subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.subscriber.details.not.found", null, Locale.ENGLISH), null);
			} else {
				SubscriberDeviceHistoryDetails subscriberDeviceHistoryDetails = new SubscriberDeviceHistoryDetails();
				SubscriberDevice subscriberDevice = deviceRepoIface.findBysubscriberUid(suid);
				List<SubscriberDeviceHistory> subscriberDeviceHistory = subscriberDeviceHistoryRepoIface
						.findSubscriberDeviceHistory(suid);

				List<HashMap<String, String>> listOfMaps = subscriberDeviceHistory.stream().map(s -> {
					HashMap<String, String> strMap = new HashMap<>();
					strMap.put("device_uid", s.getDeviceUid());
					strMap.put("created_date", s.getCreatedDate());
					return strMap;
				}).collect(Collectors.toList());

				subscriberDeviceHistoryDetails.setSubscriber(subscriber);
				subscriberDeviceHistoryDetails.setSubscriberDevice(subscriberDevice);
				subscriberDeviceHistoryDetails.setSubscriberDeviceHistory(listOfMaps);
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.subscriber.details", null, Locale.ENGLISH),
						subscriberDeviceHistoryDetails);
			}

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	@Override
	public ApiResponse getFCMToken(String subscriberUid) {
		try {
			if(subscriberUid == null || subscriberUid == "") {
				return AppUtil.createApiResponse(false, "Subscriber id can't be null or empty", null);
			}else {
				SubscriberFcmToken subscriberFcmToken = fcmTokenRepoIface.findBysubscriberUid(subscriberUid);
				if(subscriberFcmToken != null) {
					return AppUtil.createApiResponse(true, "Subscriber fcm token found successfully", subscriberFcmToken);
				}else {
					return AppUtil.createApiResponse(false, "Subscriber fcm token not found", null);
				}
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}
}
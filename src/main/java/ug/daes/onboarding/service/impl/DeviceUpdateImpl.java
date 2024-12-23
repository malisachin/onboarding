package ug.daes.onboarding.service.impl;

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
import org.springframework.stereotype.Service;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.constant.Constant;
import ug.daes.onboarding.constant.DeviceUpdatePolicy;
import ug.daes.onboarding.dto.*;
import ug.daes.onboarding.model.*;
import ug.daes.onboarding.repository.*;
import ug.daes.onboarding.service.iface.DeviceUpdateIface;
import ug.daes.onboarding.service.iface.PolicyIface;
import ug.daes.onboarding.service.iface.SubscriberServiceIface;
import ug.daes.onboarding.service.iface.TemplateServiceIface;
import ug.daes.onboarding.util.AppUtil;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static ug.daes.onboarding.service.impl.SubscriberServiceImpl.findLatestOnboardedSub;


@Service
public class DeviceUpdateImpl implements DeviceUpdateIface {

	private static Logger logger = LoggerFactory.getLogger(DeviceUpdateImpl.class);

	final static String CLASS = "DeviceUpdateImpl";

	@Autowired
	SubscriberRepoIface subscriberRepoIface;

	@Autowired
	SubscriberDeviceRepoIface deviceRepoIface;

	@Autowired
	MessageSource messageSource;

	@Autowired
	LogModelServiceImpl logModelServiceImpl;

	@Autowired
	SubscriberOnboardingDataRepoIface onboardingDataRepoIface;
	@Autowired
	TemplateServiceIface templateServiceIface;
	@Autowired
	SubscriberStatusRepoIface statusRepoIface;
	@Autowired
	SubscriberCertificatesRepoIface subscriberCertificatesRepoIface;
	@Autowired
	SubscriberCertPinHistoryRepoIface subscriberCertPinHistoryRepoIface;

	@Autowired
	SubscriberFcmTokenRepoIface fcmTokenRepoIface;

	@Autowired
	SubscriberDeviceHistoryRepoIface subscriberDeviceHistoryRepoIface;

	@Autowired
	PolicyIface policyIface;


	private final SubscriberServiceIface subscriberServiceIface;

	@Autowired
	public DeviceUpdateImpl(@Lazy SubscriberServiceIface subscriberServiceIface) {
		this.subscriberServiceIface = subscriberServiceIface;
	}

	@Autowired
	DevicePolicyRepository devicePolicyRepository;

	@Value("${device.update.min.policy}")
	private long minhour;

	@Value("${device.update.max.policy}")
	private long maxhour;


	@Override
	public void updateSubscriberDeviceAndHistory(SubscriberDevice oldDevice, String newDeviceUid) {
		// save to subscriber device history
		try {
			logger.error(CLASS + "updateSubscriberDeviceAndHistory oldDevice and newDeviceUid {}, {}", oldDevice,
					newDeviceUid);

			SubscriberDeviceHistory subscriberDeviceHistory = new SubscriberDeviceHistory();
			subscriberDeviceHistory.setDeviceUid(oldDevice.getDeviceUid());
			subscriberDeviceHistory.setDeviceStatus(Constant.DEVICE_STATUS_DISABLED);
			subscriberDeviceHistory.setSubscriberUid(oldDevice.getSubscriberUid());
			subscriberDeviceHistory.setCreatedDate(AppUtil.getDate());
			subscriberDeviceHistory.setUpdatedDate(AppUtil.getDate());
			subscriberDeviceHistoryRepoIface.save(subscriberDeviceHistory);

			oldDevice.setDeviceUid(newDeviceUid);
			oldDevice.setDeviceStatus(Constant.DEVICE_STATUS_ACTIVE);
			// oldDevice.setCreatedDate(AppUtil.getDate());
			oldDevice.setUpdatedDate(AppUtil.getDate());
			deviceRepoIface.save(oldDevice);

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "updateSubscriberDeviceAndHistory Exception {}", ex.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + "updateSubscriberDeviceAndHistory Exception {}", e.getMessage());
		}

	}

	@Override
	public ApiResponse activateNewDevice(DeviceInfo deviceInfo, MobileOTPDto mobileOTPDto) {

		try {
			int countDevice = subscriberRepoIface.countSubscriberDevice(deviceInfo.getDeviceId());
			logger.info(CLASS + "checkValidationForSubscriber countDevice {}, DeviceId {} ", countDevice,
					deviceInfo.getDeviceId());
			int countMobile = subscriberRepoIface.countSubscriberMobile(mobileOTPDto.getSubscriberMobileNumber());
			logger.info(CLASS + "checkValidationForSubscriber countMobile {} , SubscriberMobileNumber {} ", countMobile,
					mobileOTPDto.getSubscriberMobileNumber());
			int countEmail = subscriberRepoIface
					.countSubscriberEmailId(mobileOTPDto.getSubscriberEmail().toLowerCase());
			logger.info(CLASS + "checkValidationForSubscriber countEmail {}, SubscriberEmail {} ", countEmail,
					mobileOTPDto.getSubscriberEmail().toLowerCase());
			Subscriber subscriber = subscriberRepoIface.getSubscriberUidByEmailAndMobile(
					mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());

			logger.info(CLASS + " activateNewDevice countDevice and countEmail and countMobile {}, {}, {}", countDevice,
					countEmail, countMobile);
			Date startTime = new Date();
			if (countDevice == 0) {

				if (countEmail == 1 && countMobile == 1) {
					SubscriberDevice device = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());
					System.out.println("device " + device);

					SubscriberStatus subscriberStatus = statusRepoIface
							.findBysubscriberUid(subscriber.getSubscriberUid());

					if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {

						// save subscriber device in history table
						updateSubscriberDeviceAndHistory(device, deviceInfo.getDeviceId()); // save preSubscriberDevice
																							// in

						Subscriber subscriber2 = subscriberRepoIface.findBysubscriberUid(subscriber.getSubscriberUid());

						Date endTime = new Date();
						String device_info = mobileOTPDto.getOsName() + " | " + deviceInfo.getOsVersion() + " | "
								+ deviceInfo.getAppVersion() + " | " + mobileOTPDto.getDeviceInfo();
						String message = "DEVICE_UPDATE | " + deviceInfo.getDeviceId() + "|" + device.getDeviceUid()
								+ "|" + AppUtil.getDate() + "|" + device_info;

						logger.info(CLASS + " activateNewDevice device change mongo {} ", message);

						logModelServiceImpl.setLogModelDTO(true, device.getSubscriberUid(), null, "OTHER", null,
								message, startTime, endTime, null);

						if (subscriber2 != null) {
							subscriber2.setAppVersion(deviceInfo.getAppVersion());
							subscriber2.setOsVersion(deviceInfo.getOsVersion());
							subscriber2.setOsName(mobileOTPDto.getOsName());
							subscriber2.setDeviceInfo(mobileOTPDto.getDeviceInfo());
							subscriberRepoIface.save(subscriber2);
						}
						// update fcm token
						updateFcmToken(subscriber.getSubscriberUid(), mobileOTPDto.getFcmToken());

						return AppUtil.createApiResponse(true,
								messageSource.getMessage(
										"api.response.services.now.accessible.on.this.device.welcome.back", null,
										Locale.ENGLISH),
								null);
						// return AppUtil.createApiResponse(true,"new device has been activated. Please
						// note that services can be consumed after 48 hours.",null);

					} else {
						if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {
							updateSubscriberDeviceAndHistory(device, deviceInfo.getDeviceId());

							Subscriber subscriber2 = subscriberRepoIface
									.findBysubscriberUid(subscriber.getSubscriberUid());

							Date endTime = new Date();
							String device_info = mobileOTPDto.getOsName() + " | " + deviceInfo.getOsVersion() + " | "
									+ deviceInfo.getAppVersion() + " | " + mobileOTPDto.getDeviceInfo();
							String message = "DEVICE_UPDATE | " + deviceInfo.getDeviceId() + "|" + device.getDeviceUid()
									+ "|" + AppUtil.getDate() + "|" + device_info;

							logger.info(CLASS + " activateNewDevice device change mongo {} ", message);

							logModelServiceImpl.setLogModelDTO(true, device.getSubscriberUid(), null, "OTHER", null,
									message, startTime, endTime, null);

							if (subscriber2 != null) {
								subscriber2.setAppVersion(deviceInfo.getAppVersion());
								subscriber2.setOsVersion(deviceInfo.getOsVersion());
								subscriber2.setOsName(mobileOTPDto.getOsName());
								subscriber2.setDeviceInfo(mobileOTPDto.getDeviceInfo());
								subscriberRepoIface.save(subscriber2);
							}
							// update fcm token
							updateFcmToken(subscriber.getSubscriberUid(), mobileOTPDto.getFcmToken());

							return AppUtil.createApiResponse(true,
									messageSource.getMessage(
											"api.response.services.now.accessible.on.this.device.welcome.back", null,
											Locale.ENGLISH),
									null);
						}
					}

				}
			} else {
				if (countEmail == 1 && countMobile == 1) {
					SubscriberDevice device = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());
					System.out.println("device " + device);

					SubscriberStatus subscriberStatus = statusRepoIface
							.findBysubscriberUid(subscriber.getSubscriberUid());

					if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {

						// save subscriber device in history table
						updateSubscriberDeviceAndHistory(device, deviceInfo.getDeviceId()); // save preSubscriberDevice
																							// in

						Subscriber subscriber2 = subscriberRepoIface.findBysubscriberUid(subscriber.getSubscriberUid());

						Date endTime = new Date();
						String device_info = mobileOTPDto.getOsName() + " | " + deviceInfo.getOsVersion() + " | "
								+ deviceInfo.getAppVersion() + " | " + mobileOTPDto.getDeviceInfo();
						String message = "DEVICE_UPDATE | " + deviceInfo.getDeviceId() + "|" + device.getDeviceUid()
								+ "|" + AppUtil.getDate() + "|" + device_info;

						logger.info(CLASS + " activateNewDevice device change mongo {} ", message);

						logModelServiceImpl.setLogModelDTO(true, device.getSubscriberUid(), null, "OTHER", null,
								message, startTime, endTime, null);

						if (subscriber2 != null) {
							subscriber2.setAppVersion(deviceInfo.getAppVersion());
							subscriber2.setOsVersion(deviceInfo.getOsVersion());
							subscriber2.setOsName(mobileOTPDto.getOsName());
							subscriber2.setDeviceInfo(mobileOTPDto.getDeviceInfo());
							subscriberRepoIface.save(subscriber2);
						}
						// update fcm token
						updateFcmToken(subscriber.getSubscriberUid(), mobileOTPDto.getFcmToken());

						return AppUtil.createApiResponse(true,
								messageSource.getMessage(
										"api.response.services.now.accessible.on.this.device.welcome.back", null,
										Locale.ENGLISH),
								null);
						// return AppUtil.createApiResponse(true,"new device has been activated. Please
						// note that services can be consumed after 48 hours.",null);

					} else {
						if (device.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {
							updateSubscriberDeviceAndHistory(device, deviceInfo.getDeviceId());

							Subscriber subscriber2 = subscriberRepoIface
									.findBysubscriberUid(subscriber.getSubscriberUid());

							Date endTime = new Date();
							String device_info = mobileOTPDto.getOsName() + " | " + deviceInfo.getOsVersion() + " | "
									+ deviceInfo.getAppVersion() + " | " + mobileOTPDto.getDeviceInfo();
							String message = "DEVICE_UPDATE | " + deviceInfo.getDeviceId() + "|" + device.getDeviceUid()
									+ "|" + AppUtil.getDate() + "|" + device_info;

							logger.info(CLASS + " activateNewDevice device change mongo {} ", message);

							logModelServiceImpl.setLogModelDTO(true, device.getSubscriberUid(), null, "OTHER", null,
									message, startTime, endTime, null);

							if (subscriber2 != null) {
								subscriber2.setAppVersion(deviceInfo.getAppVersion());
								subscriber2.setOsVersion(deviceInfo.getOsVersion());
								subscriber2.setOsName(mobileOTPDto.getOsName());
								subscriber2.setDeviceInfo(mobileOTPDto.getDeviceInfo());
								subscriberRepoIface.save(subscriber2);
							}
							// update fcm token
							updateFcmToken(subscriber.getSubscriberUid(), mobileOTPDto.getFcmToken());

							return AppUtil.createApiResponse(true,
									messageSource.getMessage(
											"api.response.services.now.accessible.on.this.device.welcome.back", null,
											Locale.ENGLISH),
									null);
						}
					}

				}
			}

			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.the.device.is.already.registered.with.either.same.or.different.email.id.and.mobile.number",
					null, Locale.ENGLISH), null);

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "updateSubscriberDeviceAndHistory Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + "updateSubscriberDeviceAndHistory Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}

	}

	private void updateFcmToken(String suid, String fcmToken) {
		try {
			logger.error(CLASS + "updateFcmToken suid and fcmToken {}, {}", suid, fcmToken);
			SubscriberFcmToken subscriberFcmToken = fcmTokenRepoIface.findBysubscriberUid(suid);
			subscriberFcmToken.setFcmToken(fcmToken);
			subscriberFcmToken.setCreatedDate(AppUtil.getDate());
			fcmTokenRepoIface.save(subscriberFcmToken);
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "updateFcmToken Exception {}", ex.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + "updateFcmToken Exception {}", e.getMessage());
		}

	}

	private NewDeviceDTO setNewDeviceResponse(Subscriber subscriber) {
		NewDeviceDTO newDeviceDTO = new NewDeviceDTO();
		try {
			newDeviceDTO.setNewDevice(true);
			if (subscriber == null)
				return newDeviceDTO;
			SubscriberDetailsReponseDTO responseDTO = new SubscriberDetailsReponseDTO();
			responseDTO.setSuID(subscriber.getSubscriberUid());
			newDeviceDTO.setEmail(subscriber.getEmailId());
			newDeviceDTO.setMobileNumber(subscriber.getMobileNumber());

			SubscriberStatus status = statusRepoIface.findBysubscriberUid(subscriber.getSubscriberUid());
			if (status != null)
				responseDTO.setSubscriberStatus(status.getSubscriberStatus());
			else
				responseDTO.setSubscriberStatus(null);
			List<SubscriberOnboardingData> onboardingDataList = onboardingDataRepoIface
					.getBySubUid(subscriber.getSubscriberUid());
			SubscriberOnboardingData onboardingData = null;
			SubscriberDetails subscriberDetails = new SubscriberDetails();

			if (onboardingDataList != null) {
				if (onboardingDataList.size() > 1) {
					onboardingData = findLatestOnboardedSub(onboardingDataList);

				} else {
					if (onboardingDataList.size() == 1)
						onboardingData = onboardingDataList.get(0);
					else
						onboardingData = null;
				}
			}
			if (onboardingData != null) {
				newDeviceDTO.setIdDocNumber(onboardingData.getIdDocNumber());
				ApiResponse response = subscriberServiceIface.getBase64String(onboardingData.getSelfieUri());
				if (response.isSuccess()) {
					newDeviceDTO.setSelfieUri((String) response.getResult());
				} else {
					newDeviceDTO.setSelfieUri("");
				}
				String method = onboardingData.getOnboardingMethod();
				ApiResponse editTemplateDTORes = templateServiceIface
						.getTemplateLatestById(onboardingData.getTemplateId());

				if (editTemplateDTORes.isSuccess()) {
					EditTemplateDTO editTemplateDTO = (EditTemplateDTO) editTemplateDTORes.getResult();
					String certStatus = subscriberCertificatesRepoIface.getSubscriberCertificateStatus(
							subscriber.getSubscriberUid(), Constant.SIGN, Constant.ACTIVE);

					subscriberDetails.setSubscriberName(subscriber.getFullName());
					subscriberDetails.setOnboardingMethod(method);
					subscriberDetails.setTemplateDetails(editTemplateDTO);
					subscriberDetails.setCertificateStatus(certStatus);
					PinStatus pinStatus = new PinStatus();
					if (certStatus != null) {
						if (certStatus.equals(Constant.ACTIVE)) {
							SubscriberCertificatePinHistory certificatePinHistory = subscriberCertPinHistoryRepoIface
									.findBysubscriberUid(subscriber.getSubscriberUid());
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
				newDeviceDTO.setSubscriberStatusDetails(responseDTO);
			} else {

				newDeviceDTO.setIdDocNumber(null);
				newDeviceDTO.setSelfieUri(null);
				newDeviceDTO.setSubscriberStatusDetails(responseDTO);
			}
			return newDeviceDTO;
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "setNewDeviceResponse Exception {}", ex.getMessage());
			return newDeviceDTO;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + "setNewDeviceResponse Exception {}", e.getMessage());
			return newDeviceDTO;
		}

	}

	@Override
	public ApiResponse validateSubscriberAndDevice(DeviceInfo deviceInfo, MobileOTPDto mobileOTPDto) {
		try {
			int countDevice = subscriberRepoIface.countSubscriberDevice(deviceInfo.getDeviceId());
			int countMobile = subscriberRepoIface.countSubscriberMobile(mobileOTPDto.getSubscriberMobileNumber());
			int countEmail = subscriberRepoIface
					.countSubscriberEmailId(mobileOTPDto.getSubscriberEmail().toLowerCase());

			logger.info(CLASS + " validateSubscriberAndDeviceNew " + deviceInfo);
			System.out.println("countDevice :: " + countDevice + " countMobile :: " + countMobile + " countEmail :: "
					+ countEmail);

			Subscriber subscriber = subscriberRepoIface.getSubscriberDetailsByEmailAndMobile(
					mobileOTPDto.getSubscriberEmail(), mobileOTPDto.getSubscriberMobileNumber());

			NewDeviceDTO newDeviceDTO = setNewDeviceResponse(subscriber);

			String date = null;

			if (countEmail >= 1 && countMobile >= 1 && subscriber == null) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.this.mobile.no.is.already.register.with.different.email.id",
								null, Locale.ENGLISH),
						null);
			} else if (countDevice == 0 && countEmail == 0 && countMobile == 0) {
				newDeviceDTO.setNewDevice(false);
				return AppUtil.createApiResponse(true, messageSource.getMessage(
						"api.response.first.time.registering.onboarding", null, Locale.ENGLISH), newDeviceDTO);

			} else if (countDevice == 0 && countEmail == 1 && countMobile == 0) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage("api.error.this.email.id.is.already.used.with.differenet.mobile.no",
								null, Locale.ENGLISH),
						null);
			} else if (countDevice == 0 && countEmail == 0 && countMobile == 1) {
				return AppUtil.createApiResponse(false,
						messageSource.getMessage(
								"api.error.this.mobile.number.is.already.used.with.differenet.email.id", null,
								Locale.ENGLISH),
						null);
			} else if (countDevice == 0 && countEmail == 1 && countMobile == 1) {
//				if (subscriber == null) {
//					return AppUtil.createApiResponse(false,
//							messageSource.getMessage(
//									"api.error.this.mobile.no.is.already.register.with.different.email.id", null,
//									Locale.ENGLISH),
//							null);
//				} else {
				SubscriberDevice subdevice = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());

				if (subdevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {

					date = Objects.equals(subdevice.getCreatedDate(), subdevice.getUpdatedDate())
							? subdevice.getCreatedDate()
							: subdevice.getUpdatedDate();
					Optional<DevicePolicyModel> devicePolicyModel = Optional
							.ofNullable(devicePolicyRepository.getDevicePolicyHour());
					long devicePolicyHour = 0;
					if (devicePolicyModel.isPresent()) {
						devicePolicyHour = devicePolicyModel.get().getDevicePolicyHour();
						if (devicePolicyHour <= minhour) {
							devicePolicyHour = minhour;
						} else if (devicePolicyHour >= maxhour) {
							devicePolicyHour = maxhour;
						}
					} else {
						devicePolicyHour = minhour;
					}
					ApiResponse policyResponse = policyIface.checkPolicyRange(date, DeviceUpdatePolicy.PATTERN,
							devicePolicyHour);

					if (!policyResponse.isSuccess()) {

						return AppUtil.createApiResponse(false,
								"Due to recent registration on another device, access here is on hold for now. You'll regain access to this device in just "
										+ devicePolicyHour + " hours. We appreciate your patience.",
								null);
					}
					return AppUtil.createApiResponse(true, messageSource.getMessage(
							"api.response.new.device.is.ready.to.be.used", null, Locale.ENGLISH), newDeviceDTO);
				}
				// newDeviceDTO.setNewDevice(true);
				newDeviceDTO.setNewDevice(false);
				return AppUtil.createApiResponse(true,
						messageSource.getMessage("api.response.new.device.is.ready.to.be.used", null, Locale.ENGLISH),
						newDeviceDTO);

				// }
			} else {
				SubscriberDevice subDevice = deviceRepoIface.findBydeviceUidAndStatus(deviceInfo.getDeviceId(),
						Constant.DEVICE_STATUS_ACTIVE);

				if (subDevice == null && subscriber == null) {
					newDeviceDTO.setNewDevice(false);
					return AppUtil.createApiResponse(true, messageSource.getMessage(
							"api.response.first.time.registering.onboarding", null, Locale.ENGLISH), newDeviceDTO);
				} else if (subDevice != null) {
					if (subDevice != null && subscriber == null) {
						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.this.mobile.number.is.already.used.with.differenet.email.id", null,
										Locale.ENGLISH),
								newDeviceDTO);
					} else if (subDevice.getDeviceStatus().equalsIgnoreCase(Constant.DEVICE_STATUS_ACTIVE)
							&& !subDevice.getSubscriberUid().equals(subscriber.getSubscriberUid())) {

						return AppUtil.createApiResponse(false,
								messageSource.getMessage(
										"api.error.this.mobile.number.is.already.used.with.differenet.email.id", null,
										Locale.ENGLISH),
								null);
					} else {
						SubscriberDevice subscriberDevice = deviceRepoIface
								.getSubscriber(subscriber.getSubscriberUid());
						if (subscriber.getSubscriberUid().equals(subDevice.getSubscriberUid())) {
							newDeviceDTO.setNewDevice(false);
							return AppUtil.createApiResponse(true,
									messageSource.getMessage("api.response.app.is.re.installed", null, Locale.ENGLISH),
									newDeviceDTO);
						}

						if (subscriberDevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {

							date = Objects.equals(subscriberDevice.getCreatedDate(), subscriberDevice.getUpdatedDate())
									? subscriberDevice.getCreatedDate()
									: subscriberDevice.getUpdatedDate();
							Optional<DevicePolicyModel> devicePolicyModel = Optional
									.ofNullable(devicePolicyRepository.getDevicePolicyHour());
							long devicePolicyHour = 0;
							if (devicePolicyModel.isPresent()) {
								devicePolicyHour = devicePolicyModel.get().getDevicePolicyHour();
								if (devicePolicyHour <= minhour) {
									devicePolicyHour = minhour;
								} else if (devicePolicyHour >= maxhour) {
									devicePolicyHour = maxhour;
								}
							} else {
								devicePolicyHour = minhour;
							}

							ApiResponse policyResponse = policyIface.checkPolicyRange(date, DeviceUpdatePolicy.PATTERN,
									devicePolicyHour);
							if (!policyResponse.isSuccess()) {

								return AppUtil.createApiResponse(false,
										"Due to recent registration on another device, access here is on hold for now. You'll regain access to this device in just "
												+ devicePolicyHour + " hours. We appreciate your patience.",
										null);
							}

							return AppUtil.createApiResponse(true, messageSource.getMessage(
									"api.response.new.device.is.ready.to.be.used", null, Locale.ENGLISH), newDeviceDTO);

						} else {

							if (!subscriberDevice.getDeviceUid().equalsIgnoreCase(deviceInfo.getDeviceId())) {

								updateSubscriberDeviceHistory(subscriberDevice, deviceInfo.getDeviceId());

								newDeviceDTO.setNewDevice(false);
								return AppUtil.createApiResponse(true,
										messageSource.getMessage("api.response.new.device.is.ready.to.be.used", null,
												Locale.ENGLISH),
										newDeviceDTO);
							} else {
								newDeviceDTO.setNewDevice(false);
								return AppUtil.createApiResponse(true,
										messageSource.getMessage("api.response.new.device.is.ready.to.be.used", null,
												Locale.ENGLISH),
										newDeviceDTO);
							}

						}
					}
				} else {
					SubscriberDevice subscriberDevice = deviceRepoIface.getSubscriber(subscriber.getSubscriberUid());

					if (subscriberDevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {

						date = Objects.equals(subscriberDevice.getCreatedDate(), subscriberDevice.getUpdatedDate())
								? subscriberDevice.getCreatedDate()
								: subscriberDevice.getUpdatedDate();
						Optional<DevicePolicyModel> devicePolicyModel = Optional
								.ofNullable(devicePolicyRepository.getDevicePolicyHour());
						long devicePolicyHour = 0;
						if (devicePolicyModel.isPresent()) {
							devicePolicyHour = devicePolicyModel.get().getDevicePolicyHour();
							if (devicePolicyHour <= minhour) {
								devicePolicyHour = minhour;
							} else if (devicePolicyHour >= maxhour) {
								devicePolicyHour = maxhour;
							}
						} else {
							devicePolicyHour = minhour;
						}

						ApiResponse policyResponse = policyIface.checkPolicyRange(date, DeviceUpdatePolicy.PATTERN,
								devicePolicyHour);
						if (!policyResponse.isSuccess()) {

							return AppUtil.createApiResponse(false,
									"Due to recent registration on another device, access here is on hold for now. You'll regain access to this device in just "
											+ devicePolicyHour + " hours. We appreciate your patience.",
									null);
						}

						return AppUtil.createApiResponse(true, messageSource.getMessage(
								"api.response.new.device.is.ready.to.be.used", null, Locale.ENGLISH), newDeviceDTO);

					} else {

						if (!subscriberDevice.getDeviceUid().equalsIgnoreCase(deviceInfo.getDeviceId())) {

							updateSubscriberDeviceHistory(subscriberDevice, deviceInfo.getDeviceId());

							newDeviceDTO.setNewDevice(false);
							return AppUtil.createApiResponse(true, messageSource.getMessage(
									"api.response.new.device.is.ready.to.be.used", null, Locale.ENGLISH), newDeviceDTO);
						} else {
							newDeviceDTO.setNewDevice(false);
							return AppUtil.createApiResponse(true, messageSource.getMessage(
									"api.response.new.device.is.ready.to.be.used", null, Locale.ENGLISH), newDeviceDTO);
						}

					}

				}
			}
		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "validateSubscriberAndDevice Exception {}", ex.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + "validateSubscriberAndDevice Exception {}", e.getMessage());
			return AppUtil.createApiResponse(false, messageSource.getMessage(
					"api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
		}
	}

	public void updateSubscriberDeviceHistory(SubscriberDevice oldDevice, String newDeviceUid) {
		// save to subscriber device history
		try {
			logger.error(CLASS + "updateSubscriberDeviceAndHistory oldDevice and newDeviceUid {}, {}", oldDevice,
					newDeviceUid);

			SubscriberDeviceHistory subscriberDeviceHistory = new SubscriberDeviceHistory();
			subscriberDeviceHistory.setDeviceUid(oldDevice.getDeviceUid());
			subscriberDeviceHistory.setDeviceStatus(Constant.DEVICE_STATUS_DISABLED);
			subscriberDeviceHistory.setSubscriberUid(oldDevice.getSubscriberUid());
			subscriberDeviceHistory.setCreatedDate(AppUtil.getDate());
			subscriberDeviceHistory.setUpdatedDate(AppUtil.getDate());
			subscriberDeviceHistoryRepoIface.save(subscriberDeviceHistory);

		} catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
				| PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
			ex.printStackTrace();
			logger.error(CLASS + "updateSubscriberDeviceAndHistory Exception {}", ex.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(CLASS + "updateSubscriberDeviceAndHistory Exception {}", e.getMessage());
		}

	}
}

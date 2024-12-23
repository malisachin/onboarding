package ug.daes.onboarding.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hibernate.PessimisticLockException;

import org.hibernate.QueryTimeoutException;
import org.hibernate.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ug.daes.DAESService;
import ug.daes.PKICoreServiceException;
import ug.daes.Result;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.constant.ByteArrayToMultiPart;
import ug.daes.onboarding.constant.Constant;
import ug.daes.onboarding.dto.*;
import ug.daes.onboarding.enums.LogMessageType;
import ug.daes.onboarding.enums.ServiceNames;
import ug.daes.onboarding.enums.TransactionType;
import ug.daes.onboarding.exceptions.OnBoardingServiceException;
import ug.daes.onboarding.model.*;
import ug.daes.onboarding.repository.*;
import ug.daes.onboarding.response.OnBoardingServiceResponse;
import ug.daes.onboarding.service.iface.ProposedFlowIface;
import ug.daes.onboarding.service.iface.SubscriberServiceIface;
import ug.daes.onboarding.service.iface.TemplateServiceIface;
import ug.daes.onboarding.util.AppUtil;

import javax.sql.rowset.serial.SerialBlob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Blob;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ProposedFlowImpl implements ProposedFlowIface {

    private static Logger logger = LoggerFactory.getLogger(ProposedFlowImpl.class);

    /**
     * The Constant CLASS.
     */
    final static String CLASS = "PraposedFlowImpl";

    @Value("${edms.localurl}")
    private String baselocalUrl;

    @Value("${edms.downloadurl}")
    private String edmsDwonlodUrl;

    @Value("${verify.photo}")
    private Boolean verifyPhoto;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    OnboardingLivelinessRepository onboardingLivelinessRepository;

    private static Path testFile;

    @Autowired
    MessageSource messageSource;

    @Autowired
    SubscriberRepoIface subscriberRepoIface;

    @Autowired
    SubscriberDeviceRepoIface subscriberDeviceRepoIface;

    @Autowired
    SubscriberDeviceRepoIface deviceRepoIface;

    @Autowired
    SubscriberStatusRepoIface statusRepoIface;

    @Autowired
    SubscriberFcmTokenRepoIface fcmTokenRepoIface;

    @Autowired
    ConsentHistoryRepo consentHistoryRepo;

    @Autowired
    SubscriberConsentsRepo subscriberConsentsRepo;

    @Autowired
    SubscriberOnboardingDataRepoIface onboardingDataRepoIface;

    @Autowired
    TemplateServiceIface templateServiceIface;

    @Autowired
    SubscriberCertificatesRepoIface subscriberCertificatesRepoIface;

    @Autowired
    SubscriberCertPinHistoryRepoIface subscriberCertPinHistoryRepoIface;

    @Autowired
    SubscriberCompleteDetailRepoIface subscriberCompleteDetailRepoIface;

    @Autowired
    SubscriberFcmTokenRepoIface subscriberFcmTokenRepoIface;
    @Autowired
    SubscriberStatusRepoIface subscriberStatusRepoIface;

    @Autowired
    TemporaryTableRepo temporaryTableRepo;


    @Autowired
    SubscriberDeviceHistoryRepoIface subscriberDeviceHistoryRepoIface;

    @Autowired
    LogModelServiceImpl logModelServiceImpl;

    @Autowired
    OnBoardingMethodRepoIface onBoardingMethodRepoIface;


    @Autowired
    OnboardingStepDetailsRepoIface onboardingStepsRepoIface;

    @Autowired
    EdmsServiceImpl edmsService;

    @Autowired
    SubscriberRaDataRepoIface raRepoIface;

    @Autowired
    RabbitMQSender mqSender;

    @Autowired
    SubscriberOnboardingDataRepoIface subscriberOnboardingDataRepoIface;

    @Autowired
    PhotoFeaturesRepo photoFeaturesRepo;

    ObjectMapper objectMapper = new ObjectMapper();


    @Value("${signed.required.by.user}")
    private boolean signRequired;

    @Value(value = "${ind.api.sms}")
    private String indApiSMS;

    @Value("${au.log.url}")
    private String auditLogUrl;


    @Value("${extract.features}")
    private String exractFeatures;

    @Value("${find.details}")
    private String findDetails;

    @Autowired
    OnBoardingServiceResponse onBoardingServiceResponse;

    @Autowired
    OnBoardingServiceException onBoardingServiceException;

    private final SubscriberServiceIface subscriberServiceIface;

    @Autowired
    public ProposedFlowImpl(@Lazy SubscriberServiceIface subscriberServiceIface) {
        this.subscriberServiceIface = subscriberServiceIface;
    }

    @Override
    public ApiResponse saveDataTemporyTable(TemporaryTableDTO temporaryTableDTO) {
        try {
            if (temporaryTableDTO == null) {
                logger.info(CLASS + " saveDataTemporyTable() temporaryTableDTO cannot be null");
                return AppUtil.createApiResponse(false, "temporaryTableDTO cannot be null", null);
            }
            if (temporaryTableDTO.getIdDocNumber() == null || temporaryTableDTO.getIdDocNumber().isEmpty()) {
                logger.info(CLASS + " saveDataTemporyTable() id doc number cannot be null");
                return AppUtil.createApiResponse(false, "id doc number cannot be null", null);
            }

            if (temporaryTableDTO.getStep() == 1) {
                logger.info(CLASS + "saveDataTemporyTable step ::" + temporaryTableDTO.getStep());
                ApiResponse response = flag1method(temporaryTableDTO);
                if (!response.isSuccess()) {
                    return AppUtil.createApiResponse(false, response.getMessage(), response.getResult());

                }
                return AppUtil.createApiResponse(true, response.getMessage(), response.getResult());

            }
//            else if(temporaryTableDTO.getStep() == 2){
//                ApiResponse response =flag2method(temporaryTableDTO,livelinessVideo,selfie);
//                if(!response.isSuccess()){
//                    return AppUtil.createApiResponse(false,response.getMessage(),response.getResult());
//
//                }
//                return AppUtil.createApiResponse(true,response.getMessage(),response.getResult());
//            }
            else if (temporaryTableDTO.getStep() == 3) {
                logger.info(CLASS + "saveDataTemporyTable step ::" + temporaryTableDTO.getStep());
                ApiResponse response = flag3method(temporaryTableDTO);
                if (!response.isSuccess()) {
                    return AppUtil.createApiResponse(false, response.getMessage(), response.getResult());

                }
                return AppUtil.createApiResponse(true, response.getMessage(), response.getResult());

            } else if (temporaryTableDTO.getStep() == 4) {
                logger.info(CLASS + "saveDataTemporyTable step ::" + temporaryTableDTO.getStep());
                ApiResponse response = flag4method(temporaryTableDTO);
                if (!response.isSuccess()) {
                    return AppUtil.createApiResponse(false, response.getMessage(), response.getResult());

                }
                return AppUtil.createApiResponse(true, response.getMessage(), response.getResult());
            } else {
                return AppUtil.createApiResponse(false, "Step not found", null);
            }


        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "saveDataTemporyTable Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " saveDataTemporyTable Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }

    }


    public ApiResponse flag1method(TemporaryTableDTO temporaryTableDTO) {
        try {
            if (temporaryTableDTO.getDeviceId() == null || temporaryTableDTO.getDeviceId().isEmpty()) {
                logger.info(CLASS + "flag1method() req idDocNumber :: " + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Device Cannot be null", null);
            }
            if (temporaryTableDTO.getSubscriberObDataDTO() == null) {
                logger.info(CLASS + "flag1method() req Subscriber Ob data cannot be null :: " + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Subscriber Ob data cannot be null", null);
            }
            if (temporaryTableDTO.getSubscriberDeviceInfoDto() == null) {
                logger.info(CLASS + "flag1method() req Subscriber Device info cannot be null :: " + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Subscriber Device info cannot be null", null);
            }
            Subscriber subscriber = subscriberRepoIface.findbyDocumentNumber(temporaryTableDTO.getIdDocNumber());

            if (subscriber != null) {
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                logger.info(CLASS + "flag1method() details of onboarded subscriber :: " + subscriber);
                SubscriberOnboardingData subscriberOnboardingData = subscriberOnboardingDataRepoIface.findLatestSubscriber(subscriber.getSubscriberUid());
                if (subscriberOnboardingData == null) {
                    return AppUtil.createApiResponse(false, "Subscriber onboarding data cannot be null", false);
                }
                try {
                    HttpHeaders head = new HttpHeaders();
                    HttpEntity<Object> request = new HttpEntity<>(head);
                    ResponseEntity<byte[]> resp = restTemplate.exchange(subscriberOnboardingData.getSelfieUri(), HttpMethod.GET, request, byte[].class);
                    if (resp.getStatusCodeValue() == 200) {
                        System.out.println("generateVisitorCard() get selfie url called successfully");
                    }
                    String base64 = AppUtil.getBase64FromByteArr(resp.getBody());
                    temporaryResponseDto.setSelfieImage(base64);

                } catch (Exception e) {
                    e.printStackTrace();
                }


                temporaryResponseDto.setSubscriber(subscriber);
                temporaryResponseDto.setExistingSubscriber(true);

                return AppUtil.createApiResponse(true, "It seems you're already have an UGPass account. Kindly log in to access your account.", temporaryResponseDto);

            }

            SubscriberDevice subscriberDevice = subscriberDeviceRepoIface.findDeviceDetailsById(temporaryTableDTO.getDeviceId());
            if (subscriberDevice != null && subscriberDevice.getDeviceStatus().equals("ACTIVE")) {
                logger.info(CLASS + "flag1method() onboarded subscriber device details :: " + subscriberDevice);
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setExistingSubscriberDevice(true);
                return AppUtil.createApiResponse(false, "Device is already registered with onboarded user", null);
            }
            TemporaryTable temporaryTable = temporaryTableRepo.getbyidDocNumber(temporaryTableDTO.getIdDocNumber());

            TemporaryTable temporaryTableDevice = temporaryTableRepo.getByDevice(temporaryTableDTO.getDeviceId());
            List<OnboardingStepDetails> onboardingStepDetailslist = onboardingStepsRepoIface.getAllSteps();

            ObjectMapper objectMapper = new ObjectMapper();

            // Convert DTO to JSON string
            String documentDetailsJson = objectMapper.writeValueAsString(temporaryTableDTO.getSubscriberObDataDTO());
            String deviceDetailsJson = objectMapper.writeValueAsString(temporaryTableDTO.getSubscriberDeviceInfoDto());


            if (temporaryTable != null) {
                if (temporaryTableDTO.getIdDocNumber().equals(temporaryTable.getIdDocNumber()) && temporaryTableDTO.getDeviceId().equals(temporaryTable.getDeviceId())) {
                    logger.info(CLASS + "flag1method() data already exist in temporary table id doc number :: " + temporaryTable.getIdDocNumber());
                    TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                    temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
                    temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
                    temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());


                    // Convert JSON string to DeviceInfoDTO
                    SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
                    temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
                    temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());

//                    temporaryResponseDto.setStep2Details(temporaryTable.getStep2Data());
                    temporaryResponseDto.setStep2Status(temporaryTable.getStep2Status());
                    temporaryResponseDto.setMobileNumber(temporaryTable.getStep3Data());
                    temporaryResponseDto.setStep3Status(temporaryTable.getStep3Status());
                    temporaryResponseDto.setEmailId(temporaryTable.getStep4Data());
                    temporaryResponseDto.setStep4Status(temporaryTable.getStep4Status());
                    temporaryResponseDto.setStep5Details(temporaryTable.getStep5Data());
                    temporaryResponseDto.setStep5Status(temporaryTable.getStep5Status());
                    temporaryResponseDto.setStepCompleted(temporaryTable.getStepCompleted());
                    temporaryResponseDto.setNextStep(temporaryTable.getNextStep());
                    temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);
                    temporaryResponseDto.setSelfieImage(temporaryTable.getSelfie());
                    temporaryResponseDto.setDataInTemporaryTable(true);

                    return AppUtil.createApiResponse(true, "Details found", temporaryResponseDto);
                } else if (temporaryTableDevice == null && temporaryTableDTO.getIdDocNumber().equals(temporaryTable.getIdDocNumber()) && !temporaryTableDTO.getDeviceId().equals(temporaryTable.getDeviceId())) {
                    TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                    temporaryResponseDto.setNewDevice(true);
                    logger.info(CLASS + "flag1method() data already exist in temporary table coming with new device.  req id doc number :: " + temporaryTable.getIdDocNumber());
                    return AppUtil.createApiResponse(true, "Do you want to continue on this new device", temporaryResponseDto);
                }


            }

            if (temporaryTableDevice != null && !temporaryTableDevice.getIdDocNumber().equals(temporaryTableDTO.getIdDocNumber())) {
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setUsedDevice(true);
                //              logger.info(CLASS + "flag1method() data already exist in temporary table This device belongs to other onboarding user. req id doc number :: "+temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(true, "This device is already registered with different details. Use the same document to proceed, or delete the existing data and try again.", temporaryResponseDto);
            }


            ObjectMapper objectMapper1 = new ObjectMapper();
            JsonNode jsonNode1 = objectMapper1.readTree(documentDetailsJson);


//            ObjectMapper objectMapper1 = new ObjectMapper();
//            JsonNode jsonNode1 = objectMapper1.readTree(String.valueOf(jsonNode.get("document_details")));
            String subscriberType = jsonNode1.get("subscriberType").asText();
            if (subscriberType.equals("null") || subscriberType.isEmpty()) {
                return AppUtil.createApiResponse(false, "SubscriberType no can not be null or empty", null);
            }


            if (!subscriberType.equals(Constant.RESIDENT)
            ) {

                int countOptional = temporaryTableRepo.getCountOfOptionalData(temporaryTableDTO.getOptionalData1());
                System.out.println("OptionalData::::" + countOptional);
                if (countOptional > 0) {
                    logger.info(CLASS + "addSubscriberObData isOptionData1Present Onboarding can not be processed because the same national id already exists {}",
                            countOptional);
                    return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.onboarding.can.not.be.processed.because.the.same.national.id.already.exists",
                            null, Locale.ENGLISH), null);

                }

                if (temporaryTableDTO.getOptionalData1() != null
                        && !temporaryTableDTO.getOptionalData1().isEmpty()) {
                    int count = isOptionData1Present(temporaryTableDTO.getOptionalData1());
                    if (count == 1) {


                        logger.info(CLASS
                                        + "addSubscriberObData isOptionData1Present Onboarding can not be processed because the same national id already exists {}",
                                count);
                        return AppUtil.createApiResponse(false, messageSource.getMessage(
                                "api.error.onboarding.can.not.be.processed.because.the.same.national.id.already.exists",
                                null, Locale.ENGLISH), null);

                    }
                } else {
                    return AppUtil.createApiResponse(false,
                            messageSource.getMessage("api.error.optional.data.is.empty", null, Locale.ENGLISH), null);
                }
            }


            TemporaryTable temporaryTable1 = new TemporaryTable();
            TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();

//            temporaryTable1.setStep1Data(temporaryTableDTO.getStep1Details());

//            temporaryResponseDto.setStep1Details(temporaryTable1.getStep1Data());

            temporaryResponseDto.setSubscriberObDetails(temporaryTableDTO.getSubscriberObDataDTO());
            temporaryTable1.setDeviceInfo(deviceDetailsJson);
//            temporaryResponseDto.setDeviceInfo(temporaryTable1.getDeviceInfo());
            temporaryResponseDto.setSubscriberDeviceInfoDto(temporaryTableDTO.getSubscriberDeviceInfoDto());

            temporaryTable1.setStep1Status("COMPLETED");
            temporaryResponseDto.setStep1Status("COMPLETED");

            temporaryTable1.setIdDocNumber(temporaryTableDTO.getIdDocNumber());
            temporaryResponseDto.setIdDocNumber(temporaryTable1.getIdDocNumber());


            if (temporaryTableDTO.getOptionalData1().equals("0")) {
                temporaryTable1.setOptionalData1(null);
                temporaryResponseDto.setOptionalData1(null);
                ObjectNode documentDetailsJsonNode = (ObjectNode) objectMapper.readTree(documentDetailsJson);
                documentDetailsJsonNode.putNull("optionalData1");
                String updatedDocumentDetailsJson = objectMapper.writeValueAsString(documentDetailsJsonNode);
                temporaryTable1.setStep1Data(updatedDocumentDetailsJson);
            } else {
                temporaryTable1.setStep1Data(documentDetailsJson);

                temporaryTable1.setOptionalData1(temporaryTableDTO.getOptionalData1());
                temporaryResponseDto.setOptionalData1(temporaryTable1.getOptionalData1());
            }

            temporaryTable1.setDeviceId(temporaryTableDTO.getDeviceId());
            temporaryResponseDto.setDeviceId(temporaryTable1.getDeviceId());

            temporaryTable1.setStepCompleted(temporaryTableDTO.getStep());
            temporaryResponseDto.setStepCompleted(temporaryTable1.getStepCompleted());

            ApiResponse res = nextStepDetails(temporaryTableDTO.getStep());
            String Response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getResult());
            OnboardingStepDetails responseDto = objectMapper.readValue(Response, OnboardingStepDetails.class);

            if (!res.isSuccess()) {
                temporaryTable1.setNextStep(temporaryTableDTO.getStep());
                temporaryResponseDto.setNextStep(temporaryTable1.getNextStep());

            }
            temporaryTable1.setNextStep(responseDto.getStepId());
            temporaryResponseDto.setNextStep(temporaryTable1.getNextStep());

            temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);


            temporaryTable1.setCreatedOn(AppUtil.getDate());
            temporaryTable1.setUpdatedOn(AppUtil.getDate());


            System.out.println("TemporaryTable:::" + temporaryTable1);

            temporaryTableRepo.save(temporaryTable1);


            return AppUtil.createApiResponse(true, "Details of step1 saved successfully in temporary table", temporaryResponseDto);

        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "flag1method() Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " flag1method() Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }

    public ApiResponse flag2method(TemporaryTableDTO temporaryTableDTO, MultipartFile livelinessVideo, String selfie) {
        try {
            if (temporaryTableDTO == null) {
                logger.info(CLASS + "flag2method() Temporary Table dto cannot be null :: " + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Temporary Table dto cannot be null", null);

            }
            if (livelinessVideo == null) {
                logger.info(CLASS + "flag2method() livelinessVideo  caanot be null of id doc number" + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Liveliness Video cannot be null", null);
            }

            if (selfie == null) {
                logger.info(CLASS + "flag2method() selfie  caanot be null of id doc number" + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Selfie cannot be null", null);
            }
            TemporaryTable temporaryTable = temporaryTableRepo.getbyidDocNumber(temporaryTableDTO.getIdDocNumber());
            List<OnboardingStepDetails> onboardingStepDetailslist = onboardingStepsRepoIface.getAllSteps();
            if (onboardingStepDetailslist == null) {
                logger.info(CLASS + "flag2method() Onboarding Steps  caanot be null of id doc number" + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Onboarding Steps cannot be null or empty", null);
            }
            if (temporaryTable == null) {
                logger.info(CLASS + "flag2method() Document details not found for id doc number" + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Document details not found", null);
            }
            if (temporaryTable.getStepCompleted() == 2 || temporaryTable.getSelfie()!=null ) {
                logger.info(CLASS + "flag2method() data already exist in temporary table id doc number :: " + temporaryTableDTO.getIdDocNumber());
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
                temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
                temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());
//                temporaryResponseDto.setStep1Details(temporaryTable.getStep1Data());
                ObjectMapper obje = new ObjectMapper();

                // Convert JSON string to DeviceInfoDTO
                SubscriberObDetails subscriberObDetails = obje.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
                temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
                temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());
                VideoDetailsDto videoDetailsDto = objectMapper.readValue(temporaryTable.getStep2Data(), VideoDetailsDto.class);
                temporaryResponseDto.setVideoDetailsDto(videoDetailsDto);
//                temporaryResponseDto.setStep2Details(temporaryTable.getStep2Data());
                temporaryResponseDto.setStep2Status(temporaryTable.getStep2Status());

                temporaryResponseDto.setMobileNumber(temporaryTable.getStep3Data());
                temporaryResponseDto.setStep3Status(temporaryTable.getStep3Status());

                temporaryResponseDto.setEmailId(temporaryTable.getStep4Data());
                temporaryResponseDto.setStep4Status(temporaryTable.getStep4Status());

                temporaryResponseDto.setStep5Details(temporaryTable.getStep5Data());
                temporaryResponseDto.setStep5Status(temporaryTable.getStep5Status());

                temporaryResponseDto.setStepCompleted(temporaryTable.getStepCompleted());
                temporaryResponseDto.setNextStep(temporaryTable.getNextStep());
                temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);
                temporaryResponseDto.setDataInTemporaryTable(true);
                temporaryResponseDto.setSelfieImage(temporaryTable.getSelfie());
//                temporaryResponseDto.setDeviceInfo(temporaryTable.getDeviceInfo());

                return AppUtil.createApiResponse(true, "Details Found", temporaryResponseDto);
            }
            
           


            TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();


            String videoDetailsString = objectMapper.writeValueAsString(temporaryTableDTO.getVideoDetailsDto());

            temporaryTable.setStep2Status("COMPLETED");
            temporaryTable.setStep2Data(videoDetailsString);
            temporaryResponseDto.setStep2Status("COMPLETED");
            temporaryTable.setLivelinessVideo(livelinessVideo.getBytes());
            temporaryTable.setSelfie(selfie);

            temporaryTable.setUpdatedOn(AppUtil.getDate());

            ObjectMapper objectMapper = new ObjectMapper();
            ApiResponse res = nextStepDetails(temporaryTableDTO.getStep());
            String Response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getResult());
            OnboardingStepDetails responseDto = objectMapper.readValue(Response, OnboardingStepDetails.class);

            if (!res.isSuccess()) {
                temporaryTable.setNextStep(temporaryTableDTO.getStep());
                temporaryResponseDto.setNextStep(temporaryTable.getNextStep());

            }
            temporaryTable.setNextStep(responseDto.getStepId());
            temporaryResponseDto.setNextStep(temporaryTable.getNextStep());

            temporaryTable.setStepCompleted(temporaryTableDTO.getStep());
            temporaryResponseDto.setStepCompleted(temporaryTable.getStepCompleted());

            temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);

            temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
//            temporaryResponseDto.setStep1Details(temporaryTable.getStep1Data());
            SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
            temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
            temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());
            temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
//            temporaryResponseDto.setDeviceInfo(temporaryTable.getDeviceInfo());
            temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());
            temporaryResponseDto.setCreatedOn(temporaryTable.getCreatedOn());
            temporaryResponseDto.setCreatedOn(temporaryTable.getCreatedOn());
            temporaryResponseDto.setUpdatedOn(temporaryTable.getUpdatedOn());

            temporaryTableRepo.save(temporaryTable);

            return AppUtil.createApiResponse(true, "Details of step2 saved successfully in temporary table", temporaryResponseDto);

        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "flag2method() Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " flag2method() Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }

    }

    public ApiResponse flag3method(TemporaryTableDTO temporaryTableDTO) {
        try {
            Subscriber subscriber = subscriberRepoIface.findBymobileNumber(temporaryTableDTO.getMobileNumber());
            TemporaryTable temporaryTable = temporaryTableRepo.getbyidDocNumber(temporaryTableDTO.getIdDocNumber());
            TemporaryTable temporaryTableMobile = temporaryTableRepo.getByMobNumber(temporaryTableDTO.getMobileNumber());
            List<OnboardingStepDetails> onboardingStepDetailslist = onboardingStepsRepoIface.getAllSteps();
            if (subscriber != null) {
                logger.info(CLASS + "flag3method() details of onboarded subscriber :: " + subscriber);
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setSubscriber(subscriber);
                temporaryResponseDto.setExistingSubscriber(true);
                return AppUtil.createApiResponse(false, "This Mobile Number belongs to onboard user", null);
            }
            if (temporaryTable == null) {
                logger.info(CLASS + "flag3method() Details not found with this Id Document Number in temporary table :: " + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Details not found with this Id Document Number in temporary table", null);
            }
            if (temporaryTable != null) {
                if (temporaryTableDTO.getIdDocNumber().equals(temporaryTable.getIdDocNumber()) && temporaryTableDTO.getMobileNumber().equals(temporaryTable.getStep3Data())) {
                    logger.info(CLASS + "flag3method() data already exist in temporary table id doc number :: " + temporaryTableDTO.getIdDocNumber());
                    TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                    temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
                    temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
                    temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());
                    ObjectMapper objectMapper = new ObjectMapper();

                    SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
                    temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
                    temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());
//                    temporaryResponseDto.setStep2Details(temporaryTable.getStep2Data());
                    temporaryResponseDto.setStep2Status(temporaryTable.getStep2Status());
                    temporaryResponseDto.setMobileNumber(temporaryTable.getStep3Data());
                    temporaryResponseDto.setStep3Status(temporaryTable.getStep3Status());
                    temporaryResponseDto.setEmailId(temporaryTable.getStep4Data());
                    temporaryResponseDto.setStep4Status(temporaryTable.getStep4Status());
                    temporaryResponseDto.setStep5Details(temporaryTable.getStep5Data());
                    temporaryResponseDto.setStep4Status(temporaryTable.getStep5Status());
                    temporaryResponseDto.setStepCompleted(temporaryTable.getStepCompleted());
                    temporaryResponseDto.setNextStep(temporaryTable.getNextStep());
                    temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);
                    temporaryResponseDto.setDataInTemporaryTable(true);
                    temporaryResponseDto.setSelfieImage(temporaryTable.getSelfie());

                    return AppUtil.createApiResponse(true, "Details found", temporaryResponseDto);

                }

            }
            if (temporaryTableMobile == null && temporaryTable.getIdDocNumber().equals(temporaryTableDTO.getIdDocNumber()) && (temporaryTable.getStep3Data() != null && !temporaryTable.getStep3Data().equals(temporaryTableDTO.getMobileNumber()))) {
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setNewMobileNumber(true);
                logger.info(CLASS + "flag3method() Do you want to continue with this new Mobile number :: " + temporaryTable.getIdDocNumber());
                return AppUtil.createApiResponse(true, "Do you want to continue with this new Mobile number", temporaryResponseDto);
            }

            if (temporaryTableMobile != null && !temporaryTableMobile.getIdDocNumber().equals(temporaryTableDTO.getIdDocNumber())) {
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setUsedMobNumber(true);
                logger.info(CLASS + "flag3method() This Mobile number belongs to other onboarding user :: " + temporaryTable.getIdDocNumber());
                return AppUtil.createApiResponse(true, "This Mobile number belongs to other onboarding user", temporaryResponseDto);
            }

//            else if(temporaryTableMobile!=null){
//                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
//                temporaryResponseDto.setDataInTemporaryTable(true);
//
//                return AppUtil.createApiResponse(false,"Duplicate Mobile Number Found",temporaryResponseDto);
//            }


            TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
            temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
            ObjectMapper objectmp = new ObjectMapper();

            SubscriberObDetails subscriberObDetails = objectmp.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
            temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
            temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());
//            temporaryResponseDto.setStep2Details(temporaryTable.getStep2Data());
            temporaryResponseDto.setStep2Status(temporaryTable.getStep2Status());
            temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
//            temporaryResponseDto.setDeviceInfo(temporaryTable.getDeviceInfo());
            temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());
            temporaryResponseDto.setCreatedOn(temporaryTable.getCreatedOn());
            temporaryResponseDto.setUpdatedOn(temporaryTable.getUpdatedOn());

            temporaryTable.setStep3Data(temporaryTableDTO.getMobileNumber());
            temporaryResponseDto.setMobileNumber(temporaryTableDTO.getMobileNumber());

            temporaryTable.setStepCompleted(temporaryTableDTO.getStep());
            temporaryResponseDto.setStepCompleted(temporaryTableDTO.getStep());

            temporaryTable.setStep3Status("COMPLETED");
            temporaryResponseDto.setStep3Status("COMPLETED");
//            temporaryTable.setNextStep(nextStepDetails(temporaryTableDTO.getStep()));

            ObjectMapper objectMapper = new ObjectMapper();
            ApiResponse res = nextStepDetails(temporaryTableDTO.getStep());
            String Response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getResult());
            OnboardingStepDetails responseDto = objectMapper.readValue(Response, OnboardingStepDetails.class);

            if (!res.isSuccess()) {
                temporaryTable.setNextStep(temporaryTableDTO.getStep());
                temporaryResponseDto.setNextStep(temporaryTableDTO.getStep());
            }
            temporaryTable.setNextStep(responseDto.getStepId());
            temporaryResponseDto.setNextStep(responseDto.getStepId());
            temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);


            temporaryTableRepo.save(temporaryTable);
            return AppUtil.createApiResponse(true, "Details of step3 saved successfully in temporary table", temporaryResponseDto);
        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "flag3method() Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " flag3method() Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }

    }

    public ApiResponse flag4method(TemporaryTableDTO temporaryTableDTO) {
        try {
            Subscriber subscriber = subscriberRepoIface.findByemailId(temporaryTableDTO.getEmailId());
            TemporaryTable temporaryTableEmail = temporaryTableRepo.getByEmail(temporaryTableDTO.getEmailId());
            TemporaryTable temporaryTable = temporaryTableRepo.getbyidDocNumber(temporaryTableDTO.getIdDocNumber());
            List<OnboardingStepDetails> onboardingStepDetailslist = onboardingStepsRepoIface.getAllSteps();
            if (subscriber != null) {
                logger.info(CLASS + "flag4method() details of onboarded subscriber :: " + subscriber);
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setSubscriber(subscriber);
                temporaryResponseDto.setExistingSubscriber(true);
                return AppUtil.createApiResponse(false, "This Email ID belongs to onboard user", null);
            }
            if (temporaryTable == null) {
                logger.info(CLASS + "flag4method() Details not found with this Id Document Number in temporary table :: " + temporaryTableDTO.getIdDocNumber());
                return AppUtil.createApiResponse(false, "Details not found with this Id Document Number in temporary table", null);
            }
            if(temporaryTable.getStep3Data() == null || temporaryTable.getStep3Data().isEmpty()){
                AppUtil.createApiResponse(false,"Mobile number not found",null);

            }
            if (temporaryTable != null) {
                if (temporaryTableDTO.getIdDocNumber().equals(temporaryTable.getIdDocNumber()) && temporaryTableDTO.getEmailId().equals(temporaryTable.getStep4Data())) {
                    logger.info(CLASS + "flag4method() data already exist in temporary table id doc number :: " + temporaryTableDTO.getIdDocNumber());
                    TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                    temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
                    temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
                    temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());
                    ObjectMapper objectMapper = new ObjectMapper();

                    SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
                    temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
//                    temporaryResponseDto.setStep1Details(temporaryTable.getStep1Data());
                    temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());
//                    temporaryResponseDto.setStep2Details(temporaryTable.getStep2Data());
                    temporaryResponseDto.setStep2Status(temporaryTable.getStep2Status());
                    temporaryResponseDto.setMobileNumber(temporaryTable.getStep3Data());
                    temporaryResponseDto.setStep3Status(temporaryTable.getStep3Status());
                    temporaryResponseDto.setEmailId(temporaryTable.getStep4Data());
                    temporaryResponseDto.setStep4Status(temporaryTable.getStep4Status());
                    temporaryResponseDto.setStep5Details(temporaryTable.getStep5Data());
                    temporaryResponseDto.setStep4Status(temporaryTable.getStep5Status());
                    temporaryResponseDto.setStepCompleted(temporaryTable.getStepCompleted());
                    temporaryResponseDto.setNextStep(temporaryTable.getNextStep());
                    temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);
                    temporaryResponseDto.setSelfieImage(temporaryTable.getSelfie());
                    temporaryResponseDto.setDataInTemporaryTable(true);

                    return AppUtil.createApiResponse(true, "Details found", temporaryResponseDto);

                }

            }
            if (temporaryTableEmail == null && temporaryTable.getIdDocNumber().equals(temporaryTableDTO.getIdDocNumber()) && (temporaryTable.getStep4Data() != null && !temporaryTable.getStep4Data().equals(temporaryTableDTO.getEmailId()))) {
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setNewMobileNumber(true);
                logger.info(CLASS + "flag4method() Do you want to continue with this new Mobile number :: " + temporaryTable.getIdDocNumber());
                return AppUtil.createApiResponse(true, "Do you want to continue with this  new email id", temporaryResponseDto);
            }

            if (temporaryTableEmail != null && !temporaryTableEmail.getIdDocNumber().equals(temporaryTableDTO.getIdDocNumber())) {
                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setUsedEmail(true);
                logger.info(CLASS + "flag4method() This Mobile number belongs to other onboarding user :: " + temporaryTable.getIdDocNumber());
                return AppUtil.createApiResponse(true, "This email id is belongs to already to other onboarding user ", temporaryResponseDto);
            }

//            if(temporaryTable!=null){
//                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
//                temporaryResponseDto.setDataInTemporaryTable(true);
//
//                return AppUtil.createApiResponse(false,"Duplicate Email found",temporaryResponseDto);
//            }
            TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
            temporaryResponseDto.setIdDocNumber(temporaryTable.getIdDocNumber());
            ObjectMapper object = new ObjectMapper();

            SubscriberObDetails subscriberObDetails = object.readValue(temporaryTable.getStep1Data(), SubscriberObDetails.class);
            temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
//            temporaryResponseDto.setStep1Details(temporaryTable.getStep1Data());
            temporaryResponseDto.setStep1Status(temporaryTable.getStep1Status());
//            temporaryResponseDto.setStep2Details(temporaryTable.getStep2Data());
            temporaryResponseDto.setStep2Status(temporaryTable.getStep2Status());
            temporaryResponseDto.setDeviceId(temporaryTable.getDeviceId());
//            temporaryResponseDto.setDeviceInfo(temporaryTable.getDeviceInfo());
            temporaryResponseDto.setOptionalData1(temporaryTable.getOptionalData1());
            temporaryResponseDto.setCreatedOn(temporaryTable.getCreatedOn());
            temporaryResponseDto.setCreatedOn(temporaryTable.getCreatedOn());
            temporaryResponseDto.setUpdatedOn(temporaryTable.getUpdatedOn());
            temporaryResponseDto.setMobileNumber(temporaryTable.getStep3Data());
            temporaryResponseDto.setStep3Status(temporaryTable.getStep3Status());

            temporaryTable.setStep4Data(temporaryTableDTO.getEmailId());
            temporaryResponseDto.setEmailId(temporaryTable.getStep4Data());

            temporaryTable.setStep4Status("COMPLETED");
            temporaryResponseDto.setStep4Status("COMPLETED");

            temporaryTable.setStepCompleted(temporaryTableDTO.getStep());
            temporaryResponseDto.setStepCompleted(temporaryTable.getStepCompleted());
//            temporaryTable1.setNextStep(nextStepDetails(temporaryTableDTO.getStep()));
            ObjectMapper objectMapper = new ObjectMapper();
            ApiResponse res = nextStepDetails(temporaryTableDTO.getStep());
            if (!res.isSuccess()) {
                temporaryTable.setNextStep(temporaryTableDTO.getStep());
                temporaryResponseDto.setNextStep(temporaryTableDTO.getStep());
            } else {
                String Response = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(res.getResult());
                OnboardingStepDetails responseDto = objectMapper.readValue(Response, OnboardingStepDetails.class);

                temporaryTable.setNextStep(responseDto.getStepId());
                temporaryResponseDto.setNextStep(temporaryTable.getNextStep());
            }

            temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailslist);
            temporaryTableRepo.save(temporaryTable);
            return AppUtil.createApiResponse(true, "Details of step4 saved successfully in temporary table", temporaryResponseDto);
        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "flag4method() Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " flag4method() Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }

    }


    public ApiResponse checkValidationForSubscriber(MobileOTPDto mobileOTPDto) {
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
                    previousSuid = subscriberRepoIface.getSubscriberDetailsByEmailAndMobile(mobileOTPDto.getSubscriberEmail().toLowerCase(), mobileOTPDto.getSubscriberMobileNumber());
                    if (previousSuid == null) {
                        return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.this.mobile.no.is.already.register.with.different.email.id", null, Locale.ENGLISH),
                                null);
                    } else {
                        SubscriberDevice subscriberDevice = deviceRepoIface.getSubscriber(previousSuid.getSubscriberUid());
                        deviceDetails = deviceRepoIface.findBydeviceUidAndStatus(mobileOTPDto.getDeviceId(), "ACTIVE");
                        if (subscriberDevice.getDeviceUid().equals(mobileOTPDto.getDeviceId())) {


                            //if(deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)){
                            if (deviceDetails != null) {
                                if (!subscriberDevice.getSubscriberUid().equals(deviceDetails.getSubscriberUid())) {
                                    return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.this.device.is.already.register.with.differet.email.or.mobile.no", null, Locale.ENGLISH),
                                            null);
                                } else {
                                    countDevice = 1;
                                }
                            } else {
                                countDevice = 1;
                            }

                        } else if (deviceDetails != null) {
                            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.this.device.is.already.register.with.differet.email.or.mobile.no", null, Locale.ENGLISH),
                                    null);
                        } else {
                            subscriberDeviceDetails = deviceRepoIface.findDeviceDetailsById(mobileOTPDto.getDeviceId());
                            SubscriberStatus subscriberStatus = statusRepoIface.findBysubscriberUid(subscriberDeviceDetails.getSubscriberUid());
                            if (subscriberDeviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
                                return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.this.device.is.already.register.with.differet.email.or.mobile.no", null, Locale.ENGLISH),
                                        null);
                            } else {
                                countDevice = 1;
                            } //if(subscriberDeviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)) {
                            //countDevice = subscriberRepoIface.countSubscriberDevice(mobileOTPDto.getDeviceId());
                            //}
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
                    //deviceDetails = deviceRepoIface.findBydeviceUid(mobileOTPDto.getDeviceId());
                    deviceDetails = deviceRepoIface.getSubscriber(previousSuid.getSubscriberUid());

                    if (deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)
                            || deviceDetails.getDeviceStatus() == Constant.DEVICE_STATUS_DISABLED) {
                        System.out.println("checkValidationForSubscriber This Device is disabled ");
                        //return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.this.device.is.disabled", null, Locale.ENGLISH),null);
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
                            SubscriberConsents subscriberConsents = subscriberConsentsRepo.findSubscriberConsentBySuidAndConsentId(deviceDetails.getSubscriberUid(), consentHistory.getId());
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
                        SubscriberConsents subscriberConsents = subscriberConsentsRepo.findSubscriberConsentBySuidAndConsentId(deviceDetails.getSubscriberUid(), consentHistory.getId());
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
                    //deviceDetails = deviceRepoIface.findBydeviceDetails(mobileOTPDto.getDeviceId());
                    deviceDetails = deviceRepoIface.findBydeviceUidAndStatus(mobileOTPDto.getDeviceId(), "ACTIVE");
                    logger.info(CLASS + " checkValidationForSubscriber deviceDetails : {}", deviceDetails);
                    //SubscriberStatus subscriberStatus = statusRepoIface.findBysubscriberUid(deviceDetails.getSubscriberUid());

                    //logger.info(CLASS + " checkValidationForSubscriber subscriberStatus : {}", subscriberStatus);
                    //if(deviceDetails != null) {
                    //return AppUtil.createApiResponse(false,messageSource.getMessage("api.error.this.device.is.already.register.with.differet.email.or.mobile.no", null, Locale.ENGLISH),
                    //		null);
                    //}else {

                    //}
                    System.out.println(" deviceDetails " + deviceDetails);
                    //if (deviceDetails.getDeviceStatus().equals(Constant.DEVICE_STATUS_ACTIVE)) {
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


    @Override
    public ApiResponse submitObData(String idDocumentNumber) {
        try {
            if (idDocumentNumber.isEmpty() || idDocumentNumber.equals("null")) {
                return AppUtil.createApiResponse(true, "ID Document Number Cannot be null", null);
            }

            TemporaryTable temporaryTable = temporaryTableRepo.getbyidDocNumber(idDocumentNumber);
            int countOfValues = onboardingStepsRepoIface.getNoOfOnboardingSteps();
            if (temporaryTable == null) {
                return AppUtil.createApiResponse(false, "No data found for given id doc number", null);

            } else if (temporaryTable.getStepCompleted() != countOfValues) {
                return AppUtil.createApiResponse(false, "Please complete all steps", null);
            }

            String featuresBase64 = null;


            if(verifyPhoto) {
               ApiResponse responseOfFaceVerification = verifyFaceFeatures(temporaryTable.getSelfie());
                if (!responseOfFaceVerification.isSuccess()) {
                    return AppUtil.createApiResponse(false, responseOfFaceVerification.getMessage(), responseOfFaceVerification.getResult());
                }
                featuresBase64 = responseOfFaceVerification.getResult().toString();
            }

            String step1Json = temporaryTable.getStep1Data();

            String deviceInfo = temporaryTable.getDeviceInfo();

            MobileOTPDto mobileOTPDto = new MobileOTPDto();
            JsonNode documentDetailsJson = objectMapper.readTree(step1Json);
            JsonNode deviceDetailsJson = objectMapper.readTree(deviceInfo);

            mobileOTPDto.setSubscriberName(documentDetailsJson.get("subscriberName").asText());
            mobileOTPDto.setDeviceId(temporaryTable.getDeviceId());
            mobileOTPDto.setSubscriberMobileNumber(temporaryTable.getStep3Data());
            mobileOTPDto.setSubscriberEmail(temporaryTable.getStep4Data());
            mobileOTPDto.setFcmToken(deviceDetailsJson.get("fcmToken").asText());
            mobileOTPDto.setOtpStatus(true);
            mobileOTPDto.setOsName(deviceDetailsJson.get("osName").asText());
            mobileOTPDto.setOsVersion(deviceDetailsJson.get("osVersion").asText());
            mobileOTPDto.setAppVersion(deviceDetailsJson.get("appVersion").asText());
            mobileOTPDto.setDeviceInfo(deviceDetailsJson.get("deviceInfo").asText());
            mobileOTPDto.setIdDocNumber(idDocumentNumber);

            ApiResponse response = saveSubscribersData(mobileOTPDto);
            System.out.println("response::::" + response);
            if (!response.isSuccess()) {
                return AppUtil.createApiResponse(false, response.getMessage(), response.getResult());
            }

            SubscriberRegisterResponseDTO responseDTO = (SubscriberRegisterResponseDTO) response.getResult();

            // Access the suID field
            String suID = responseDTO.getSuID();

            //saving data into photo features
            
            if(verifyPhoto) {

//                String featuresBase64 = responseOfFaceVerification.getResult().toString();

                byte[] featuresByte = featuresBase64.getBytes();

                byte[] decodedData = Base64.getDecoder().decode(featuresBase64);

                Blob blob = new SerialBlob(decodedData);

                PhotoFeatures photoFeatures = new PhotoFeatures();
                photoFeatures.setPhotoFeatures(blob);
                photoFeatures.setSuid(suID);
                photoFeatures.setCreatedOn(AppUtil.getDate());
                photoFeatures.setUpdatedOn(AppUtil.getDate());
                photoFeaturesRepo.save(photoFeatures);


            }

            String step2Json = temporaryTable.getStep2Data();

            // for saving video in EDMS
            FileUploadDTO fileUploadDTO = populateFileUploadDTO(step2Json,suID);
            MultipartFile multipartFile = convertToMultipartFile(temporaryTable.getLivelinessVideo(), "file", "filename.mp4", "video/mp4");
            CompletableFuture<ApiResponse> videoResponse = edmsService.saveFileToEdms(multipartFile,"video",fileUploadDTO);
            System.out.println("videoResponse: "+ videoResponse);
            ApiResponse apiResponse = videoResponse.get();
            if (!apiResponse.isSuccess()) {
                return AppUtil.createApiResponse(false, response.getMessage(), response.getResult());
            }

            SubscriberObRequestDTO subscriberObRequestDTO = createSubscriberObRequestDTO
                    (suID, documentDetailsJson, temporaryTable, idDocumentNumber);
            ApiResponse res = subscriberServiceIface.addSubscriberObData(subscriberObRequestDTO);



            if (!res.isSuccess()) {
            	System.out.println(" dt stging ");
                return AppUtil.createApiResponse(false, res.getMessage(), res.getResult());
            }

            int deleteValue = temporaryTableRepo.deleteRecordByIdDocumentNumber(idDocumentNumber);
            if (deleteValue != 1) {
                return AppUtil.createApiResponse(false, "Record not deleted from temporary table", null);
            }
            return AppUtil.createApiResponse(true, res.getMessage(), res.getResult());
        }catch (Exception e){
            e.printStackTrace();
            return onBoardingServiceException.handleExceptionWithStaticMessage(e);
        }
    }

    public SubscriberObRequestDTO createSubscriberObRequestDTO(String suID, JsonNode documentDetailsJson, TemporaryTable temporaryTable, String idDocumentNumber) {
        SubscriberObRequestDTO subscriberObRequestDTO = new SubscriberObRequestDTO();
        subscriberObRequestDTO.setSuID(suID);
        subscriberObRequestDTO.setOnboardingMethod(documentDetailsJson.get("onboardingMethod").asText());
        subscriberObRequestDTO.setTemplateId(documentDetailsJson.get("templateID").asInt());
        subscriberObRequestDTO.setSubscriberType(documentDetailsJson.get("subscriberType").asText());

        SubscriberObData subscriberObData = new SubscriberObData();
        subscriberObData.setDateOfBirth(documentDetailsJson.get("dateOfBirth").asText());
        subscriberObData.setDateOfExpiry(documentDetailsJson.get("dateOfExpiry").asText());
        subscriberObData.setNationality(documentDetailsJson.get("nationality").asText());
        subscriberObData.setGender(documentDetailsJson.get("gender").asText());
        subscriberObData.setPrimaryIdentifier(documentDetailsJson.get("primaryIdentifier").asText());
        subscriberObData.setSecondaryIdentifier(documentDetailsJson.get("secondaryIdentifier").asText());
        subscriberObData.setDocumentType(documentDetailsJson.get("documentType").asText());
        subscriberObData.setOptionalData1(documentDetailsJson.get("optionalData1").asText());
        subscriberObData.setOptionalData2(documentDetailsJson.get("optionalData2").asText());
        subscriberObData.setDocumentNumber(idDocumentNumber);
        subscriberObData.setIssuingState(documentDetailsJson.get("issuingState").asText());
        subscriberObData.setSubscriberSelfie(temporaryTable.getSelfie());
        subscriberObData.setGeoLocation(documentDetailsJson.get("geoLocation").asText());
        subscriberObData.setRemarks(documentDetailsJson.get("remarks").asText());
        subscriberObData.setSubscriberUniqueId(suID);
        subscriberObRequestDTO.setSubscriberData(subscriberObData);
        return subscriberObRequestDTO;
    }


    public FileUploadDTO populateFileUploadDTO(String step2Json, String suID) throws IOException {
        FileUploadDTO videoUploadReq = objectMapper.readValue(step2Json, FileUploadDTO.class);

        FileUploadDTO fileUploadDTO = new FileUploadDTO();
        fileUploadDTO.setSubscriberUid(suID);
        fileUploadDTO.setRecordedTime(videoUploadReq.getRecordedTime());
        fileUploadDTO.setRecordedGeoLocation(videoUploadReq.getRecordedGeoLocation());
        fileUploadDTO.setVerificationFirst(videoUploadReq.getVerificationFirst());
        fileUploadDTO.setVerificationSecond(videoUploadReq.getVerificationSecond());
        fileUploadDTO.setVerificationThird(videoUploadReq.getVerificationThird());
        fileUploadDTO.setTypeOfService(videoUploadReq.getTypeOfService());
        return fileUploadDTO;
    }

    private MultipartFile convertToMultipartFile(byte[] byteArray, String paramName, String fileName, String contentType) {
        return new ByteArrayToMultiPart(
                byteArray,   // byte array of file contents
                paramName,   // parameter name for the file
                fileName,    // original filename
                contentType  // content type of the file
        );
    }


    public ApiResponse saveSubscribersData(MobileOTPDto subscriberDTO) throws ParseException {
        if (subscriberDTO.getOsName() == null || subscriberDTO.getAppVersion() == null
                || subscriberDTO.getOsVersion() == null || subscriberDTO.getDeviceInfo() == null) {
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage("api.error.application.info.not.found", null, Locale.ENGLISH), null);
        }
        Date startTime = new Date();
//        String OtpReqTime = AppUtil.getTimeStamping();
//        String URI = auditLogUrl;
//        logger.info(CLASS + " saveSubscriberData auditLogUrl {} ", URI);
//        OtpDTO dto = new OtpDTO();
//        String email = encryptedString(subscriberDTO.getSubscriberEmail());
//        dto.setIdentifier(email);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<OtpDTO> requestEntity = new HttpEntity<>(dto, headers);
//        ResponseEntity<String> correlation = restTemplate.exchange(URI, HttpMethod.POST, requestEntity, String.class);
//        ObjectMapper objectMapper = new ObjectMapper();
//        AuditDTO auditDTO = null;
//        String correlationId = null;
//        try {
//            if (correlation.getStatusCode() != HttpStatus.NO_CONTENT || correlation != null) {
//                auditDTO = objectMapper.readValue(correlation.getBody(), AuditDTO.class);
//            }
//        } catch (Exception e) {
//            logger.error(CLASS + " saveSubscriberData Exception in AuditLogUrl {} ", e.getMessage());
//            System.out.println(e.getMessage());
//        }
//        if (auditDTO != null) {
//            correlationId = auditDTO.getCorrelationID();
//        }
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
        //ApiResponse response = checkValidationForSubscriberNew(subscriberDTO);
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

//                    logModelServiceImpl.setLogModel(true, subscriber.getSubscriberUid(), null,
//                            "SUBSCRIBER_REGISTRATION", subscriber.getSubscriberUid(), String.valueOf(toatlTime), startTime, endTime,
//                            null);
                    logger.info(CLASS + " saveSubscriberData Subscriber Detail saved {}", responseDTO);
                    if (!signRequired) {
                        SubscriberConsents subscriberConsents = new SubscriberConsents();
                        String consentData = "I agreed to above Terms and conditions and Data privacy terms";
                        ConsentHistory consentHistory = consentHistoryRepo.findLatestConsent();
                        if (subscriberConsentsRepo.findSubscriberConsentBySuidAndConsentId(responseDTO.getSuID(), consentHistory.getId()) == null) {
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
//                    logModelServiceImpl.setLogModel(false, subscriber.getSubscriberUid(), null,
//                            "SUBSCRIBER_REGISTRATION", subscriber.getSubscriberUid(), null, null, null, null);
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
            logger.error(CLASS + "saveSubscriberData Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(CLASS + "saveSubscriberData Exception {}", e.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }


    public ApiResponse addSubscriberObDataOLD(SubscriberObRequestDTO obRequestDTO) throws Exception {
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
        logger.info(CLASS + " addSubscriberObData request {}", obRequestDTO);
        try {
            subscriber = subscriberRepoIface.findBysubscriberUid(obRequestDTO.getSuID());
            if (subscriber == null) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
            }
        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
                 | PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException e) {
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(
                    CLASS + "addSubscriberObData Exception Something went Wrong, Onboarding Failed " + e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage("api.error.something.went.wrong.onboarding.failed", null, Locale.ENGLISH),
                    null);
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
                                        + "addSubscriberObData isOptionData1Present Onboarding can not be processed because the same national id already exists {}",
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

//		subscriber
//				.setFullName(subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier());
//		subscriber.setDateOfBirth(subscriberObData.getDateOfBirth());
//		subscriber.setIdDocType(subscriberObData.getDocumentType());
//		subscriber.setIdDocNumber(subscriberObData.getDocumentNumber());
//		subscriber.setUpdatedDate(AppUtil.getDate());
//		subscriber.setSubscriberUid(obRequestDTO.getSuID());
//
//		onboardingData.setCreatedDate(AppUtil.getDate());
//		onboardingData.setIdDocType(subscriberObData.getDocumentType());
//		onboardingData.setIdDocNumber(subscriberObData.getDocumentNumber());
//		onboardingData.setOnboardingMethod(obRequestDTO.getOnboardingMethod());
//		onboardingData.setSubscriberUid(obRequestDTO.getSuID());
//		onboardingData.setTemplateId(obRequestDTO.getTemplateId());
//		onboardingData.setOnboardingMethod(obRequestDTO.getOnboardingMethod());
//		onboardingData.setSubscriberType(obRequestDTO.getSubscriberType());
//		onboardingData.setIdDocCode(subscriberObData.getDocumentCode());
//		onboardingData.setGender(subscriberObData.getGender());
//		onboardingData.setGeolocation(subscriberObData.getGeoLocation());
//		onboardingData.setOptionalData1(subscriberObData.getOptionalData1());
        try {
            System.out.println("hashcode :: " + subscriberRepoIface.hashCode());
            subscriberStatus = subscriberRepoIface.getSubscriberStatus(obRequestDTO.getSuID());
            logger.info(CLASS + "addSubscriberObData req for subscriberStatus {}", subscriberStatus);
            if (subscriberStatus == null) {
                return AppUtil.createApiResponse(false,
                        messageSource.getMessage("api.error.subscriber.not.found", null, Locale.ENGLISH), null);
            }

            subscriberDevice = deviceRepoIface.getSubscriber(obRequestDTO.getSuID());
            if (subscriberDevice.getDeviceStatus().equals(Constant.DEVICE_STATUS_DISABLED)
                    || subscriberDevice.getDeviceStatus() == Constant.DEVICE_STATUS_DISABLED) {
                logger.info(CLASS + " addSubscriberObData req for subscriberDevice {}", subscriberDevice);
                // System.out.println("addSubscriberObData This Device is disabled");
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
        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
                 | PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "addSubscriberObData Exception 2 {}", ex.getMessage());
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage("api.error.something.went.wrong.onboarding.failed", null, Locale.ENGLISH),
                    null);
        } catch (Exception e) {
            logger.error(CLASS + "addSubscriberObData Exception 2 {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage("api.error.something.went.wrong.onboarding.failed", null, Locale.ENGLISH),
                    null);
        }

        subscriber
                .setFullName(subscriberObData.getSecondaryIdentifier() + " " + subscriberObData.getPrimaryIdentifier());
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

//		if (obRequestDTO.getOnboardingMethod().equals("UNID")) {
//			onboardingData.setLevelOfAssurance("LOA1");
//		} else if (obRequestDTO.getOnboardingMethod().equals("NIN")) {
//			onboardingData.setLevelOfAssurance("LOA3");
//		} else if (obRequestDTO.getOnboardingMethod().equals("E-PASSPORT")) {
//			onboardingData.setLevelOfAssurance("LOA3");
//		} else if (obRequestDTO.getOnboardingMethod().equals("PASSPORT")) {
//			onboardingData.setLevelOfAssurance("LOA2");
//		}

        // if (obRequestDTO.getOnboardingMethod().equals("EMIRATES_ID")) {
        // onboardingData.setLevelOfAssurance("LOA1");
        // }else if (obRequestDTO.getOnboardingMethod().equals("EMIRATES_ID_ICA")) {
        // onboardingData.setLevelOfAssurance("LOA3");
        // }else if (obRequestDTO.getOnboardingMethod().equals("E-PASSPORT")) {
        // onboardingData.setLevelOfAssurance("LOA3");
        // }else if (obRequestDTO.getOnboardingMethod().equals("PASSPORT")){
        // onboardingData.setLevelOfAssurance("LOA2");
        // }else if(obRequestDTO.getOnboardingMethod().equals("EMIRATES_ID_NFC")) {
        // onboardingData.setLevelOfAssurance("LOA3");
        // }

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

//		String uriIssueCert = raBaseUrl + "/RegistrationAuthority/api/post/service/certificate/request";
//		String uriCheckStatus = raBaseUrl + "/RegistrationAuthority/api/get/service/status";
        try {

            // ResponseEntity<ApiResponse> resStatus =
            // restTemplate.getForEntity(uriCheckStatus, ApiResponse.class);
            // ApiResponse response = resStatus.getBody();

            ObjectMapper objectMapper = new ObjectMapper();
            Selfie selfie = new Selfie();
            selfie.setSubscriberSelfie(obRequestDTO.getSubscriberData().getSubscriberSelfie());
            selfie.setSubscriberUniqueId(obRequestDTO.getSuID());
            logger.info(CLASS + "addSubscriberObData req for saveSelfieToEdms {}", selfie);
            ApiResponse selfieResponse = edmsService.saveSelfieToEdms(selfie);
            if (selfieResponse.isSuccess()) {
                logger.info(CLASS + " addSubscriberObData res for saveSelfieToEdms {}", selfieResponse);
                String selfieURI = (String) selfieResponse.getResult();
                onboardingData.setSelfieUri(selfieURI);
            } else {
                logger.info(CLASS + "addSubscriberObData res in false for saveSelfieToEdms {}",
                        selfieResponse.getMessage());
                return AppUtil.createApiResponse(false, selfieResponse.getMessage(), null);
            }
            logger.info(CLASS + "addSubscriberObData req for createThumlbnailOfSelfie");
            ApiResponse selfieThumbnail = edmsService.createThumlbnailOfSelfie(selfie);
            if (selfieThumbnail.isSuccess()) {
                logger.info(CLASS + "addSubscriberObData res for createThumlbnailOfSelfie {}",
                        selfieThumbnail.isSuccess());
                onboardingData.setSelfieThumbnailUri(selfieThumbnail.getResult().toString());
            } else {
                return AppUtil.createApiResponse(false, selfieThumbnail.getMessage(), null);
            }

            savedSubscriber = subscriberRepoIface.save(subscriber);

            additionalFile = subscriberObData;
            additionalFile.setSubscriberSelfie(null);
            additionalFile.setSubscriberUniqueId(onboardingData.getSubscriberUid());
            String additionalFieldSaved = objectMapper.writeValueAsString(additionalFile);
            onboardingData.setOnboardingDataFieldsJson(additionalFieldSaved);
            onboardingData.setRemarks(obRequestDTO.getSubscriberData().getRemarks());

            onboardingData = onboardingDataRepoIface.save(onboardingData);
            status = statusRepoIface.findBysubscriberUid(onboardingData.getSubscriberUid());

//			HttpHeaders headers = new HttpHeaders();
//			headers.setContentType(MediaType.APPLICATION_JSON);
//			HttpEntity<Object> requestEntity = new HttpEntity<>(issueCertDTO, headers);

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
//					ResponseEntity<String> res = restTemplate.exchange(uriIssueCert, HttpMethod.POST, requestEntity,
//							String.class);
                }
            } else {
                status.setSubscriberStatus(Constant.ONBOARDED);
                status.setSubscriberStatusDescription(Constant.ONBOARDED_SUCESSFULLY);
                status.setUpdatedDate(AppUtil.getDate());
                status = statusRepoIface.save(status);
                raData = raRepoIface.save(raData);
//				ResponseEntity<String> res = restTemplate.exchange(uriIssueCert, HttpMethod.POST, requestEntity,
//						String.class);
            }

            if (savedSubscriber != null) {

                Date endTime = new Date();
                double toatlTime = AppUtil.getDifferenceInSeconds(startTime, endTime);
                logModelServiceImpl.setLogModel(true, savedSubscriber.getSubscriberUid(),
                        onboardingData.getGeolocation(), Constant.SUBSCRIBER_ONBOARDED,
                        savedSubscriber.getSubscriberUid(), String.valueOf(toatlTime), startTime, endTime, null);
                logger.info(CLASS + " addSubscriberObData Subscriber OnBoarding Data Saved {}", savedSubscriber);
                return AppUtil.createApiResponse(true, messageSource
                                .getMessage("api.response.ugPass.application.submitted.successfully", null, Locale.ENGLISH),
                        savedSubscriber);
            } else {
                logModelServiceImpl.setLogModel(false, savedSubscriber.getSubscriberUid(), null,
                        Constant.SUBSCRIBER_ONBOARDED, savedSubscriber.getSubscriberUid(), null, null, null, null);
                return AppUtil.createApiResponse(false, messageSource.getMessage(
                        "api.error.ugPass.application.submission.failed", null, Locale.ENGLISH), subscriber);
            }
        } catch (HttpClientErrorException e) {
            logger.error(CLASS + "addSubscriberObData  HttpClientErrorException {}", e.getMessage());
            e.printStackTrace();
            setLogModel(false, subscriber, onboardingData.getGeolocation());
            return AppUtil.createApiResponse(false,
                    messageSource.getMessage("api.error.something.went.wrong.onboarding.failed", null, Locale.ENGLISH),
                    null);
        } catch (ResourceAccessException e) {
            logger.error(CLASS + "addSubscriberObData ResourceAccessException {}", e.getMessage());
            e.printStackTrace();
            setLogModel(false, subscriber, onboardingData.getGeolocation());
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.service.is.unavailable.please.try.after.sometime", null, Locale.ENGLISH), null);
        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException
                 | PessimisticLockException | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "addSubscriberObData  Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + "addSubscriberObData  Exception {}", e.getMessage());
            e.printStackTrace();
            setLogModel(false, subscriber, onboardingData.getGeolocation());
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }

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


    int isOptionData1Present(String optionalData1) {

        int optionalDataCount = onboardingDataRepoIface.getOptionalData1(optionalData1);

        return optionalDataCount;
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

    private String getTimeStampString() throws ParseException {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return f.format(new Date());
    }


//    public int nextStepDetails(int currentStepId){
//
//        try{
//
//            int countNoOfSteps = onboardingStepsRepoIface.getNoOfOnboardingSteps();
//
//            if(countNoOfSteps == currentStepId){
//                return currentStepId;
//            }
//
//            while (currentStepId < countNoOfSteps){
//
//                  OnboardingStepDetails onboardingSteps =   onboardingStepsRepoIface.getStepDetails(currentStepId+1);
//                  if(onboardingSteps.getStatus().equals("ACTIVE")){
//
//                      return onboardingSteps.getStepId();
//
//                  }
//
//                  currentStepId +=1;
//            }
//
//            return currentStepId;
//
//        }catch (Exception e){
//            return -1;
//        }
//
//
//    }


    public ApiResponse nextStepDetails(int currentStepId) {

        try {

            int countNoOfSteps = onboardingStepsRepoIface.getNoOfOnboardingSteps();

            if (countNoOfSteps == currentStepId) {
                return AppUtil.createApiResponse(false, "Last Step", countNoOfSteps);
            }


            OnboardingStepDetails onboardingSteps = onboardingStepsRepoIface.getStepDetails(currentStepId + 1);

            return AppUtil.createApiResponse(true, "Next Step Details", onboardingSteps);


        } catch (Exception e) {
            return AppUtil.createApiResponse(false, "something went wrong", null);
        }


    }


    @Override
    public ApiResponse updateRecord(UpdateTemporaryTableDto updateTemporaryTableDto) {
        try {
            if (updateTemporaryTableDto.getIdDocNumber() == null || updateTemporaryTableDto.getIdDocNumber().isEmpty()) {
                return AppUtil.createApiResponse(false, "id doc number cannot be null or empty", null);
            }
            List<OnboardingStepDetails> onboardingStepDetailsList = onboardingStepsRepoIface.getAllSteps();
            if (updateTemporaryTableDto.getSubscriberDeviceInfoDto() != null) {
                TemporaryTable temporaryTable1 = temporaryTableRepo.getbyidDocNumber(updateTemporaryTableDto.getIdDocNumber());

                if (temporaryTable1 == null) {
                    return AppUtil.createApiResponse(false, "no record found for given document id number", null);

                }

                String deviceDetailsJson = objectMapper.writeValueAsString(updateTemporaryTableDto.getSubscriberDeviceInfoDto());
                JsonNode jsonNode = objectMapper.readTree(deviceDetailsJson);
                String deviceId = jsonNode.get("deviceId").asText();

                temporaryTable1.setDeviceId(deviceId);
                temporaryTable1.setDeviceInfo(deviceDetailsJson);

                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setIdDocNumber(temporaryTable1.getIdDocNumber());
//                temporaryResponseDto.setStep1Details(temporaryTable1.getStep1Data());


                SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable1.getStep1Data(), SubscriberObDetails.class);
                temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
                temporaryResponseDto.setStep1Status(temporaryTable1.getStep1Status());
//                temporaryResponseDto.setStep2Details(temporaryTable1.getStep2Data());
                temporaryResponseDto.setStep2Status(temporaryTable1.getStep2Status());
                temporaryResponseDto.setDeviceId(temporaryTable1.getDeviceId());
                SubscriberDeviceInfoDto subscriberDeviceInfoDto = objectMapper.readValue(temporaryTable1.getDeviceInfo(), SubscriberDeviceInfoDto.class);
                temporaryResponseDto.setSubscriberDeviceInfoDto(subscriberDeviceInfoDto);
                temporaryResponseDto.setOptionalData1(temporaryTable1.getOptionalData1());
                temporaryResponseDto.setCreatedOn(temporaryTable1.getCreatedOn());
                temporaryResponseDto.setCreatedOn(temporaryTable1.getCreatedOn());
                temporaryResponseDto.setUpdatedOn(temporaryTable1.getUpdatedOn());
                temporaryResponseDto.setMobileNumber(temporaryTable1.getStep3Data());
                temporaryResponseDto.setStep3Status(temporaryTable1.getStep3Status());

                temporaryResponseDto.setStepCompleted(temporaryTable1.getStepCompleted());
                temporaryResponseDto.setNextStep(temporaryTable1.getNextStep());
                temporaryResponseDto.setStep4Status(temporaryTable1.getStep4Status());
                temporaryResponseDto.setEmailId(temporaryTable1.getStep4Data());
                temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailsList);


                temporaryTableRepo.save(temporaryTable1);
                return AppUtil.createApiResponse(true, "device updated successfully", temporaryResponseDto);
            } else if (updateTemporaryTableDto.getMobileNumber() != null || !updateTemporaryTableDto.getMobileNumber().isEmpty()) {
                TemporaryTable temporaryTable1 = temporaryTableRepo.getbyidDocNumber(updateTemporaryTableDto.getIdDocNumber());

                if (temporaryTable1 == null) {
                    return AppUtil.createApiResponse(false, "no record found for given document id number", null);

                }
                temporaryTable1.setStep3Data(updateTemporaryTableDto.getMobileNumber());

                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setIdDocNumber(temporaryTable1.getIdDocNumber());
//                temporaryResponseDto.setStep1Details(temporaryTable1.getStep1Data());
                SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable1.getStep1Data(), SubscriberObDetails.class);
                temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
                temporaryResponseDto.setStep1Status(temporaryTable1.getStep1Status());
//                temporaryResponseDto.setStep2Details(temporaryTable1.getStep2Data());
                temporaryResponseDto.setStep2Status(temporaryTable1.getStep2Status());
                temporaryResponseDto.setDeviceId(temporaryTable1.getDeviceId());
//                temporaryResponseDto.setDeviceInfo(temporaryTable1.getDeviceInfo());
                SubscriberDeviceInfoDto subscriberDeviceInfoDto = objectMapper.readValue(temporaryTable1.getDeviceInfo(), SubscriberDeviceInfoDto.class);
                temporaryResponseDto.setSubscriberDeviceInfoDto(subscriberDeviceInfoDto);
                temporaryResponseDto.setOptionalData1(temporaryTable1.getOptionalData1());
                temporaryResponseDto.setCreatedOn(temporaryTable1.getCreatedOn());
                temporaryResponseDto.setCreatedOn(temporaryTable1.getCreatedOn());
                temporaryResponseDto.setUpdatedOn(temporaryTable1.getUpdatedOn());
                temporaryResponseDto.setMobileNumber(temporaryTable1.getStep3Data());
                temporaryResponseDto.setStep3Status(temporaryTable1.getStep3Status());

                temporaryResponseDto.setStepCompleted(temporaryTable1.getStepCompleted());
                temporaryResponseDto.setStep4Status(temporaryTable1.getStep4Status());
                temporaryResponseDto.setEmailId(temporaryTable1.getStep4Data());
                temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailsList);

                temporaryTableRepo.save(temporaryTable1);
                return AppUtil.createApiResponse(true, "Mobile Number  updated successfully", temporaryResponseDto);
            } else if (updateTemporaryTableDto.getEmailId() != null || !updateTemporaryTableDto.getEmailId().isEmpty()) {
                TemporaryTable temporaryTable1 = temporaryTableRepo.getbyidDocNumber(updateTemporaryTableDto.getIdDocNumber());

                if (temporaryTable1 == null) {
                    return AppUtil.createApiResponse(false, "no record found for given document id number", null);

                }
                temporaryTable1.setStep4Data(updateTemporaryTableDto.getEmailId());

                TemporaryResponseDto temporaryResponseDto = new TemporaryResponseDto();
                temporaryResponseDto.setIdDocNumber(temporaryTable1.getIdDocNumber());
//                temporaryResponseDto.setStep1Details(temporaryTable1.getStep1Data());
                SubscriberObDetails subscriberObDetails = objectMapper.readValue(temporaryTable1.getStep1Data(), SubscriberObDetails.class);
                temporaryResponseDto.setSubscriberObDetails(subscriberObDetails);
                temporaryResponseDto.setStep1Status(temporaryTable1.getStep1Status());
//                temporaryResponseDto.setStep2Details(temporaryTable1.getStep2Data());
                temporaryResponseDto.setStep2Status(temporaryTable1.getStep2Status());
                temporaryResponseDto.setDeviceId(temporaryTable1.getDeviceId());
//                temporaryResponseDto.setDeviceInfo(temporaryTable1.getDeviceInfo());
                SubscriberDeviceInfoDto subscriberDeviceInfoDto = objectMapper.readValue(temporaryTable1.getDeviceInfo(), SubscriberDeviceInfoDto.class);
                temporaryResponseDto.setSubscriberDeviceInfoDto(subscriberDeviceInfoDto);
                temporaryResponseDto.setOptionalData1(temporaryTable1.getOptionalData1());
                temporaryResponseDto.setCreatedOn(temporaryTable1.getCreatedOn());
                temporaryResponseDto.setCreatedOn(temporaryTable1.getCreatedOn());
                temporaryResponseDto.setUpdatedOn(temporaryTable1.getUpdatedOn());
                temporaryResponseDto.setMobileNumber(temporaryTable1.getStep3Data());
                temporaryResponseDto.setStep3Status(temporaryTable1.getStep3Status());

                temporaryResponseDto.setStepCompleted(temporaryTable1.getStepCompleted());
                temporaryResponseDto.setStep4Status(temporaryTable1.getStep4Status());
                temporaryResponseDto.setEmailId(temporaryTable1.getStep4Data());
                temporaryResponseDto.setOnboardingStepDetails(onboardingStepDetailsList);

                temporaryTableRepo.save(temporaryTable1);
                return AppUtil.createApiResponse(true, "Email ID  updated successfully", temporaryResponseDto);
            }

            return null;


        } catch (Exception e) {
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);

        }
    }

//    @Override
//    public ApiResponse deleteRecord(String anyValue) {
//        try{
//            if(temporaryTableRepo.getByMobNumber(anyValue) != null){
//                int a = temporaryTableRepo.deleteRecord(anyValue,null,null);
//                if(a!=1){
//                    return AppUtil.createApiResponse(false,"temporary table record is not deleted by mobile number",null);
//                }
//            }
//            if(temporaryTableRepo.getByEmail(anyValue) != null) {
//                int a = temporaryTableRepo.deleteRecord(null, anyValue, null);
//                if(a!=1){
//                    return AppUtil.createApiResponse(false,"temporary table record is not deleted by using email id",null);
//                }
//            }
//            if(temporaryTableRepo.getByDevice(anyValue) != null){
//                int a = temporaryTableRepo.deleteRecord(null, null, anyValue);
//                if(a!=1){
//                    return AppUtil.createApiResponse(false,"temporary table record is not deleted by using device id",null);
//                }
//            }
//
//            return AppUtil.createApiResponse(true,"temporary table record deleted suceessfully",null);
//
//
//        }catch (Exception e) {
//            e.printStackTrace();
//            return AppUtil.createApiResponse(false, messageSource.getMessage(
//                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
//
//        }
//    }

    @Override
    public ApiResponse deleteRecord(UpdateTemporaryTableDto updateTemporaryTableDto) {
        try {
            if (temporaryTableRepo.getByMobNumber(updateTemporaryTableDto.getMobileNumber()) != null) {
                Optional<TemporaryTable> temporaryTable = Optional.ofNullable(temporaryTableRepo.getByMobNumber(updateTemporaryTableDto.getMobileNumber()));
                if (!temporaryTable.isPresent()) {
                    return AppUtil.createApiResponse(false, "There is no record with this Mobile Number", null);
                }

                int a = temporaryTableRepo.deleteRecord(updateTemporaryTableDto.getMobileNumber(), null, null);
                if (a != 1) {
                    return AppUtil.createApiResponse(false, "temporary table record is not deleted by mobile number", null);
                }
                return AppUtil.createApiResponse(true, "temporary table record deleted suceessfully", null);

            }
            if (temporaryTableRepo.getByEmail(updateTemporaryTableDto.getEmailId()) != null) {
                Optional<TemporaryTable> temporaryTable = Optional.ofNullable(temporaryTableRepo.getByEmail(updateTemporaryTableDto.getMobileNumber()));
                if (!temporaryTable.isPresent()) {
                    return AppUtil.createApiResponse(false, "There is no record with this Email id", null);
                }
                int a = temporaryTableRepo.deleteRecord(null, updateTemporaryTableDto.getEmailId(), null);
                if (a != 1) {
                    return AppUtil.createApiResponse(false, "temporary table record is not deleted by using email id", null);
                }
                return AppUtil.createApiResponse(true, "temporary table record deleted suceessfully", null);
            }

            if (temporaryTableRepo.getByDevice(updateTemporaryTableDto.getDeviceId()) != null) {
                Optional<TemporaryTable> temporaryTable = Optional.ofNullable(temporaryTableRepo.getByDevice(updateTemporaryTableDto.getDeviceId()));
                if (!temporaryTable.isPresent()) {
                    return AppUtil.createApiResponse(false, "There is no record with this Device id", null);
                }
                int a = temporaryTableRepo.deleteRecord(null, null, updateTemporaryTableDto.getDeviceId());
                if (a != 1) {
                    return AppUtil.createApiResponse(false, "temporary table record is not deleted by using device id", null);
                }
                return AppUtil.createApiResponse(true, "temporary table record deleted suceessfully", null);
            }

            return AppUtil.createApiResponse(false, "something went wrong", null);


        } catch (Exception e) {
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage(
                    "api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);

        }
    }

    @Async
    public CompletableFuture<ApiResponse> saveVideoToEdms(MultipartFile file, FileUploadDTO fileupload) {
        try {
            logger.info(CLASS + " saveVideoToEdms req fileupload {} and File {} ", fileupload, file.getOriginalFilename());

            // Check for empty file
            if (file.isEmpty()) {
                return CompletableFuture.completedFuture(onBoardingServiceResponse.createApiResponse(false,"api.error.subscriber.video.cant.be.null.or.empty", null));
            }

            String contentType = file.getContentType();
            logger.info("file.getContentType() :: " + contentType);

            // Validate content type
            if (contentType == null || !contentType.startsWith("video/")) {
                return CompletableFuture.completedFuture(onBoardingServiceResponse.createApiResponse(true,"api.error.video.content.type.is.not.mp4", null));
            }

            // Get Document ID asynchronously
            return fetchDocumentIdAsync().thenCompose(documentId -> {
                String docIdAndFileUrl = baselocalUrl + "/documents/" + documentId.getId() + "/files";

                // Upload file asynchronously
                return uploadFileAsync(docIdAndFileUrl, file, fileupload).thenApply(result -> {
                    if (result.getStatusCodeValue() == 202) {
                        String downloadUrl = edmsDwonlodUrl + documentId.getId() + "/files/downloads";
                        saveOnboardingLiveliness(fileupload, downloadUrl);
                        return onBoardingServiceResponse.createApiResponse(true,"api.response.video.uploaded.successfully",null);
                    } else {
                        return onBoardingServiceException.handleErrorRestTemplateResponse(result.getStatusCodeValue());
                    }
                });
            });
        } catch (Exception e) {
            logger.error(CLASS + " saveVideoToEdms Exception: ", e.getMessage());
            e.printStackTrace();
            return CompletableFuture.completedFuture(onBoardingServiceException.handleExceptionWithStaticMessage(e));
        }
    }

// Async Helper Methods

    @Async
    public CompletableFuture<DocumentResponse> fetchDocumentIdAsync() {
        String docIdUrl = baselocalUrl + "/documents";
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(new LinkedMultiValueMap<>(), new HttpHeaders());
        return CompletableFuture.supplyAsync(() -> restTemplate.exchange(docIdUrl, HttpMethod.POST, requestEntity, DocumentResponse.class).getBody());
    }

    @Async
    public CompletableFuture<ResponseEntity<ApiResponse>> uploadFileAsync(String docIdAndFileUrl, MultipartFile file, FileUploadDTO fileupload) {
        MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("file_new", new FileSystemResource(convert(file)));
        bodyMap.add("model", fileupload.getSubscriberUid() + " _Video " + AppUtil.getDate());
        bodyMap.add("action", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);
        return CompletableFuture.supplyAsync(() -> restTemplate.exchange(docIdAndFileUrl, HttpMethod.POST, requestEntity, ApiResponse.class));
    }

     public void saveOnboardingLiveliness(FileUploadDTO fileUploadDTO, String downloadUrl) {
        OnboardingLiveliness onboardingLiveliness = new OnboardingLiveliness();
        onboardingLiveliness.setSubscriberUid(fileUploadDTO.getSubscriberUid());
        onboardingLiveliness.setRecordedTime(fileUploadDTO.getRecordedTime());
        onboardingLiveliness.setRecordedGeoLocation(fileUploadDTO.getRecordedGeoLocation());
        onboardingLiveliness.setVerificationFirst(fileUploadDTO.getVerificationFirst().name());
        onboardingLiveliness.setVerificationSecond(fileUploadDTO.getVerificationSecond().name());
        onboardingLiveliness.setVerificationThird(fileUploadDTO.getVerificationThird().name());
        onboardingLiveliness.setTypeOfService(fileUploadDTO.getTypeOfService().name());
        onboardingLiveliness.setUrl(downloadUrl);
        onboardingLivelinessRepository.save(onboardingLiveliness);
    }


    public static File convert(MultipartFile file) {
        System.out.println("TOMCAT_HOME_PATH ::" + System.getProperty("catalina.home"));
        String tomcatBasePath = System.getProperty("catalina.home");
        // Create a File object representing the folder
        File folder = new File(tomcatBasePath, "ObTempFiles");
        File convFile = new File(folder.getAbsolutePath() + File.separator + file.getOriginalFilename());
        // Check if the folder already exists
        if (folder.exists()) {
            System.out.println("Folder already exists. PATH ::" + folder.getAbsolutePath());
            try {
                convFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(convFile);
                fos.write(file.getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return convFile;
        } else {
            // Create the folder
            boolean created = folder.mkdir();
            // Check if the folder creation was successful
            if (created) {
                System.out.println("Folder created successfully. PATH ::" + folder.getAbsolutePath());
            } else {
                System.out.println("Failed to create the folder.");
            }
            try {
                convFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(convFile);
                fos.write(file.getBytes());
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return convFile;
        }
    }


    @Override
    public ApiResponse saveStep2Details(TemporaryTableDTO temporaryTableDTO, MultipartFile livelinessVideo, String selfie) {
        try {
            if (temporaryTableDTO == null) {
                return AppUtil.createApiResponse(false, "temporaryTableDTO cannot be null", null);
            }
            if (temporaryTableDTO.getIdDocNumber() == null || temporaryTableDTO.getIdDocNumber().isEmpty()) {
                return AppUtil.createApiResponse(false, "id doc number cannot be null", null);
            }

            if (temporaryTableDTO.getStep() == -1) {

            }

            if (temporaryTableDTO.getStep() == 2) {
                ApiResponse response = flag2method(temporaryTableDTO, livelinessVideo, selfie);
                if (!response.isSuccess()) {
                    return AppUtil.createApiResponse(false, response.getMessage(), response.getResult());

                }
                return AppUtil.createApiResponse(true, response.getMessage(), response.getResult());
            } else {
                return AppUtil.createApiResponse(false, "Something went wrong", null);
            }

        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "getOnBoardingSteps Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " getOnBoardingSteps Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Override
    public void deleteOldRecords() {


        List<TemporaryTable> records = temporaryTableRepo.findAll();


        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime now = LocalDateTime.now();

        for (TemporaryTable record : records) {
            if (record.getUpdatedOn() != null) {

                LocalDateTime updatedOn = LocalDateTime.parse(record.getUpdatedOn(), formatter);

                LocalDateTime threshold = now.minusHours(24);


                if (updatedOn.isBefore(threshold) || updatedOn.isEqual(threshold)) {

                    temporaryTableRepo.deleteRecordByIdDocumentNumber(record.getIdDocNumber());


                }
            }
        }


    }

    // Api to save face features into database in photo_features table
    @Override
    public ApiResponse getAllSubscriberExtractFeatures() {
        try {
            List<SubscriberOnboardingData> subscriberOnboardingDataList = subscriberOnboardingDataRepoIface.getAllSelfies();

            for (SubscriberOnboardingData subscriberOnboardingData : subscriberOnboardingDataList) {
                try {
                    ApiResponse response = externalEdmsApi(subscriberOnboardingData.getSelfieUri());

//                    if(!response.isSuccess()){
//                        return AppUtil.createApiResponse(false,response.getMessage(),response.getResult());
//                    }


                        ApiResponse response1 = extractFeatchersPython(response.getResult().toString());
                        if(!response1.isSuccess()){
                            return AppUtil.createApiResponse(false,"response from facefeature python api is negative",null);
                        }
                        String featuresBase64 = response1.getResult().toString();

                    byte[] decodedData = Base64.getDecoder().decode(featuresBase64);

                    Blob blob = new SerialBlob(decodedData);

//                        byte[] featuresByte = featuresBase64.getBytes();

                        PhotoFeatures photoFeatures = new PhotoFeatures();

                        photoFeatures.setSuid(subscriberOnboardingData.getSubscriberUid());
                        photoFeatures.setPhotoFeatures(blob);
                        photoFeatures.setCreatedOn(AppUtil.getDate());
                        photoFeatures.setUpdatedOn(AppUtil.getDate());
                        photoFeaturesRepo.save(photoFeatures);


                } catch (JDBCConnectionException | ConstraintViolationException | DataException |
                         LockAcquisitionException |
                         PessimisticLockException
                         | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
                    ex.printStackTrace();
                    logger.error(CLASS + "getAllSubscriberExtractFeatures Exception {}", ex.getMessage());
                    return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
                } catch (Exception e) {
                    logger.error(CLASS + " getAllSubscriberExtractFeatures Exception {}", e.getMessage());
                    e.printStackTrace();
                    return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
                }
            }
            return AppUtil.createApiResponse(true, "All features are extracted and saved in db", null);
        } catch (JDBCConnectionException | ConstraintViolationException | DataException |
                 LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "getAllSubscriberExtractFeatures Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " getAllSubscriberExtractFeatures Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }

    // Python api called to fetch face featues
    public ApiResponse extractFeatchersPython(String subscriberPhoto) {
        try {
            HttpHeaders headers = new HttpHeaders();

            extractFeatureInputDto extractFeatureInputDto = new extractFeatureInputDto();
            extractFeatureInputDto.setSubscriberPhoto(subscriberPhoto);
            HttpEntity<Object> request = new HttpEntity<>(extractFeatureInputDto, headers);

            ResponseEntity<ApiResponse> response = restTemplate.exchange(exractFeatures,
                    HttpMethod.POST, request, ApiResponse.class);

            if (!response.getBody().isSuccess()) {
                return AppUtil.createApiResponse(false, "Extract feature python api failed", null);
            }
            return AppUtil.createApiResponse(true, "features extracted successfully", response.getBody().getResult());

        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "extractFeatchersPython Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " extractFeatchersPython Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }

    }

//api to fetch subscriber selfie base 64 from edms
    public ApiResponse externalEdmsApi(String edmsUrl) {
        try {
            RestTemplate restTemplate = new RestTemplate();
                try {
                    HttpHeaders head = new HttpHeaders();
                    HttpEntity<Object> request = new HttpEntity<>(head);
                    ResponseEntity<byte[]> resp = restTemplate.exchange(edmsUrl, HttpMethod.GET, request, byte[].class);
                    if (resp.getStatusCodeValue() == 200) {
                        System.out.println("printing response " + resp);
                        String selfieBase64 = AppUtil.getBase64FromByteArr(resp.getBody());
                        return AppUtil.createApiResponse(true,"edms selfie",selfieBase64);
                    }else{
                        return AppUtil.createApiResponse(true, "edms selfie fetchednot fetched", null);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    return AppUtil.createApiResponse(false,"edms url failed",null);

                }

        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "externalEdmsApi Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " externalEdmsApi Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }

    public ApiResponse verifyFaceFeatures(String selfieBase64){
        try{

            ApiResponse response1 = findDetails(selfieBase64);
            if(!response1.isSuccess()){
                return AppUtil.createApiResponse(false,response1.getMessage(),response1.getResult());
            }

            return AppUtil.createApiResponse(true,response1.getMessage(),response1.getResult());

        }catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                PessimisticLockException
                | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "externalEdmsApi Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " externalEdmsApi Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }
    }


    public ApiResponse findDetails(String subscriberPhoto) {
        try {
            HttpHeaders headers = new HttpHeaders();

            extractFeatureInputDto extractFeatureInputDto = new extractFeatureInputDto();
            extractFeatureInputDto.setImage(subscriberPhoto);
            HttpEntity<Object> request = new HttpEntity<>(extractFeatureInputDto, headers);

            ResponseEntity<ApiResponse> response = restTemplate.exchange(findDetails,
                    HttpMethod.POST, request, ApiResponse.class);

            if (!response.getBody().isSuccess()) {
                return AppUtil.createApiResponse(false, response.getBody().getMessage(), response.getBody().getResult());
            }
            return AppUtil.createApiResponse(true, response.getBody().getMessage(), response.getBody().getResult());

        } catch (JDBCConnectionException | ConstraintViolationException | DataException | LockAcquisitionException |
                 PessimisticLockException
                 | QueryTimeoutException | SQLGrammarException | GenericJDBCException ex) {
            ex.printStackTrace();
            logger.error(CLASS + "extractFeatchersPython Exception {}", ex.getMessage());
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.registration.failed", null, Locale.ENGLISH), null);
        } catch (Exception e) {
            logger.error(CLASS + " extractFeatchersPython Exception {}", e.getMessage());
            e.printStackTrace();
            return AppUtil.createApiResponse(false, messageSource.getMessage("api.error.something.went.wrong.please.try.after.sometime", null, Locale.ENGLISH), null);
        }

    }
}






package ug.daes.onboarding.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.dto.TemporaryTableDTO;
import ug.daes.onboarding.dto.UpdateTemporaryTableDto;
import ug.daes.onboarding.model.TemporaryTable;
import ug.daes.onboarding.repository.TemporaryTableRepo;
import ug.daes.onboarding.service.iface.ProposedFlowIface;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class ProposedFlowController {

    private static Logger logger = LoggerFactory.getLogger(ProposedFlowController.class);
    /** The Constant CLASS. */
    final static String CLASS = "ProposedFlowColtroller";

    @Autowired
    ProposedFlowIface proposedFlowIface;

    @Autowired

    TemporaryTableRepo temporaryTableRepo;

    ObjectMapper mapper = new ObjectMapper();

    @PostMapping(value = "api/save/temporary-data")
    public ApiResponse saveDataTemporaryTable( @RequestBody TemporaryTableDTO model) throws JsonProcessingException {


//        TemporaryTableDTO temporaryTableDTO = mapper.readValue(model,TemporaryTableDTO.class);
        logger.info(CLASS + "saveDataTemporaryTable req for given id doc number :: "+model.getIdDocNumber());
        return proposedFlowIface.saveDataTemporyTable(model);
    }


    @PostMapping(value = "api/save/video-selfie/details/temporary-data")
    public ApiResponse saveStep2DataTemporaryTable(@RequestParam(value = "file", required = false) MultipartFile file,
                                            @RequestParam(value ="selfie",required = false) String selfie ,
                                            @RequestParam(value = "model") String model) throws JsonProcessingException {


        TemporaryTableDTO temporaryTableDTO = mapper.readValue(model,TemporaryTableDTO.class);
        logger.info(CLASS + "saveStep2DataTemporaryTable() req  for given id doc number:: "+temporaryTableDTO.getIdDocNumber());

        return proposedFlowIface.saveStep2Details(temporaryTableDTO,file,selfie);
    }

    @PostMapping (value = "api/submit/ob-data/{idDocumentNumber}")
    public ApiResponse submitData(@PathVariable String idDocumentNumber){

        logger.info(CLASS + "submitData() req :: "+idDocumentNumber);
        return proposedFlowIface.submitObData(idDocumentNumber);
    }

//    @PostMapping(value = "api/update/device/{idDocumentNumber}")
//    public ApiResponse updateDevice(@PathVariable String idDocumentNumber,@RequestParam(value = "deviceInfo") String deviceInfo){
//        return praposedFlowIface.updateDevice(idDocumentNumber,deviceInfo);
//    }

    @PostMapping(value = "api/update/temporaryTable")
    public ApiResponse updateRecordByDeviceIdOrMobileOrEmail(@RequestBody UpdateTemporaryTableDto updateTemporaryTableDto){
        logger.info(CLASS + "updateRecordByDeviceIdOrMobileOrEmail()  req for given id doc number:: "+updateTemporaryTableDto.getIdDocNumber());
        return proposedFlowIface.updateRecord(updateTemporaryTableDto);
    }

//    @PostMapping(value = "api/delete/record/{anyValue}")
//    public ApiResponse deleteRecordbyDeviceIdorMobNoOrEmail(@PathVariable String anyValue){
//
//        return proposedFlowIface.deleteRecord(anyValue);
//    }

    @PostMapping(value = "api/delete-record/temporaryTable")
    public ApiResponse deleteRecordbyDeviceIdorMobNoOrEmail(@RequestBody UpdateTemporaryTableDto updateTemporaryTableDto){
        logger.info(CLASS + " inside deleteRecordbyDeviceIdorMobNoOrEmail()");
        return proposedFlowIface.deleteRecord(updateTemporaryTableDto);
    }


//    @Scheduled(cron = "0 0 0 * * ?")
//    public void deleteOldRecords() {
//        // Get the current time
//        LocalDateTime now = LocalDateTime.now();
//
//        // Fetch all records from the repository
//        List<TemporaryTable> records = temporaryTableRepo.findAll();
//
//        for (TemporaryTable record : records) {
//            // Check if the updated time is older than or equal to 24 hours
//            if (record.getUpdatedOn().isBefore(now.minusHours(24))) {
//                myEntityRepository.delete(record);
//                System.out.println("Deleted record with ID: " + record.getId());
//            }
//        }

    @PostMapping(value = "api/extract/features")
    public ApiResponse getAllSubscriberExtractFeatures(){
        logger.info(CLASS + " inside getAllSubscriberExtractFeatures()");
        return proposedFlowIface.getAllSubscriberExtractFeatures();
    }






}

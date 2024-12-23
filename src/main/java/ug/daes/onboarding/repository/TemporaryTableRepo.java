package ug.daes.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ug.daes.onboarding.model.SusbcriberDetailsView;
import ug.daes.onboarding.model.TemporaryTable;

import javax.transaction.Transactional;

@Repository
public interface TemporaryTableRepo  extends JpaRepository<TemporaryTable, Integer> {

    @Query(value = "select * from temporary_table where id_doc_number =?1",nativeQuery = true)
    TemporaryTable getbyidDocNumber(String idDocNumber);

    @Query(value = "select count(*) from temporary_table where optional_data1 =?1",nativeQuery = true)
    int getCountOfOptionalData(String optionalData1);

    @Query(value = "select * from temporary_table where step_3_data=?1",nativeQuery = true)
    TemporaryTable getByMobNumber(String mob);


    @Query(value = "select * from temporary_table where device_id=?1",nativeQuery = true)
    TemporaryTable getByDevice(String deviceId);

    @Query(value = "select * from temporary_table where step_4_data=?1",nativeQuery = true)
    TemporaryTable getByEmail(String email);


    @Modifying
    @Transactional
    @Query(value = "call ra_0_2.delete_subscriber_temporary_mob_or_email_deviceId (?1,?2,?3)", nativeQuery = true)
    int deleteRecord(String anyValue1,String anyValue2,String anyValue3);


    @Modifying
    @Transactional
    @Query(value = "call ra_0_2.delete_onboarding_user(?1)", nativeQuery = true)
    int deleteRecordByIdDocumentNumber(String idDocNumber);


}

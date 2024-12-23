package ug.daes.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ug.daes.onboarding.model.SusbcriberDetailsView;

@Repository
public interface SusbcriberDetailsViewRepo extends JpaRepository<SusbcriberDetailsView, Integer>{
	
	SusbcriberDetailsView findBysubscriberUid(String subscriberUniqueId);
	
	@Query(value = "select s.subscriber_uid from susbcriber_details s where s.mobile_number=?1", nativeQuery = true)
	String findBymobileNumber(String mobileNumber);
	
	@Query(value = "select s.subscriber_uid from susbcriber_details s where s.email_id=?1", nativeQuery = true)
	String findByemailId(String emailId);

}

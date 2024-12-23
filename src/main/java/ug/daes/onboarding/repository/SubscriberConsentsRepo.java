package ug.daes.onboarding.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ug.daes.onboarding.model.SubscriberConsents;

@Repository
public interface SubscriberConsentsRepo extends JpaRepository<SubscriberConsents,Integer> {
    @Query(value = "select * from subscriber_consents where subscriber_uid=?1 and consent_id=?2",nativeQuery = true)
    SubscriberConsents findSubscriberConsentBySuidAndConsentId(String suid,int ConsentId);
}

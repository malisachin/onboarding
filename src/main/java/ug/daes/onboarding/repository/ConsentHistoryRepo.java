package ug.daes.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ug.daes.onboarding.model.ConsentHistory;

import javax.persistence.criteria.CriteriaBuilder;

@Repository
public interface ConsentHistoryRepo extends JpaRepository<ConsentHistory, Integer> {

    @Query(value = "select * from consent_history where consent_required=1 order by created_On DESC LIMIT 1",nativeQuery = true)
    ConsentHistory findLatestConsent();


    @Query(value = "select * from consent_history where consent_id=?1 order by created_on DESC LIMIT 1",nativeQuery = true)
    ConsentHistory LatestConsent(int id);
}

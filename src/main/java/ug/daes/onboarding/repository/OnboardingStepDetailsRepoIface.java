package ug.daes.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ug.daes.onboarding.model.OnboardingStepDetails;

import java.util.List;

@Repository

public interface OnboardingStepDetailsRepoIface extends JpaRepository <OnboardingStepDetails,Integer>{

    @Query(value = "select count(step_id) from onboarding_step_details ", nativeQuery = true)
    int getNoOfOnboardingSteps();

    @Query(value = "select * from onboarding_step_details where step_id =?1", nativeQuery = true)
    OnboardingStepDetails getStepDetails(int stepId);

    @Query(value = "select * from onboarding_step_details",nativeQuery = true)
    List<OnboardingStepDetails> getAllSteps();

}

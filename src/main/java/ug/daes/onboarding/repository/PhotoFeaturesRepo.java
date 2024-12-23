package ug.daes.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ug.daes.onboarding.model.PhotoFeatures;

@Repository
public interface PhotoFeaturesRepo extends JpaRepository<PhotoFeatures,Integer> {

}

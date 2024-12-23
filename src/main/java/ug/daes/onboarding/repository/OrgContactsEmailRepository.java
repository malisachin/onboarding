package ug.daes.onboarding.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import ug.daes.onboarding.model.OrgContactsEmail;

@Repository
public interface OrgContactsEmailRepository extends JpaRepository<OrgContactsEmail, Integer> {

	@Query(value = "select count(*) from org_subscriber_email ose where employee_email =?1 and not ugpass_email =?2",nativeQuery =  true)
	int findByOrgEmailAndNotUgPassEmail(String organizationEmail, String ugpassEmail);
	
//	@Query(value = "select count(*) from org_subscriber_email ose where employee_email =?1",nativeQuery =  true)
//	OrgContactsEmail findByOrganizationEmail(String organizationEmail);
	
	@Query(value = "select count(*) from org_subscriber_email ose where employee_email =?1 and not mobile_number =?2",nativeQuery =  true)
	int findByOrgEmailAndNotMobile(String organizationEmail, String mobileNumber);

	@Query(value = "select count(*) from org_subscriber_email ose where employee_email =?1 and not passport_number =?2",nativeQuery =  true)
	int findByOrgEmailAndNotPassport(String organizationEmail, String passportNumber);

	@Query(value = "select count(*) from org_subscriber_email ose where employee_email =?1 and not national_id_number =?2",nativeQuery =  true)
	int findByOrgEmailAndNotNin(String organizationEmail, String nationalIdNumber);

	
}

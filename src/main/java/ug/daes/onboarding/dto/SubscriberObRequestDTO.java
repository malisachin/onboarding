/**
 * 
 */
package ug.daes.onboarding.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Raxit Dubey
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscriberObRequestDTO implements Serializable{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String suID;
    
    private String onboardingMethod;
    
    private String mobileNo;
	private String emailId;
    
    private SubscriberObData subscriberData;
    
    private int templateId;
    
    private int consentId;
    
    private String acknowledgement;
    
    private String subscriberType;
    
    private String onboardingApprovalStatus;
    
    private String certStatus;
    
    private String onboardingPaymentStatus;
    
    private String levelOfAssurance;

    private List<SubscriberDocuments> subscriberDocuments;
    
    private CertificateDetailDto certificateDetailDto; 
	public String getSuID() {
		return suID;
	}

	public void setSuID(String suID) {
		this.suID = suID;
	}

	public String getOnboardingMethod() {
		return onboardingMethod;
	}

	public void setOnboardingMethod(String onboardingMethod) {
		this.onboardingMethod = onboardingMethod;
	}

	public SubscriberObData getSubscriberData() {
		return subscriberData;
	}

	public void setSubscriberData(SubscriberObData subscriberData) {
		this.subscriberData = subscriberData;
	}

	public List<SubscriberDocuments> getSubscriberDocuments() {
		return subscriberDocuments;
	}

	public void setSubscriberDocuments(List<SubscriberDocuments> subscriberDocuments) {
		this.subscriberDocuments = subscriberDocuments;
	}

	public int getTemplateId() {
		return templateId;
	}

	public void setTemplateId(int templateId) {
		this.templateId = templateId;
	}

	public int getConsentId() {
		return consentId;
	}

	public void setConsentId(int consentId) {
		this.consentId = consentId;
	}

	public String getAcknowledgement() {
		return acknowledgement;
	}

	public void setAcknowledgement(String acknowledgement) {
		this.acknowledgement = acknowledgement;
	}

	public String getSubscriberType() {
		return subscriberType;
	}

	public void setSubscriberType(String subscriberType) {
		this.subscriberType = subscriberType;
	}

	public String getOnboardingApprovalStatus() {
		return onboardingApprovalStatus;
	}

	public void setOnboardingApprovalStatus(String onboardingApprovalStatus) {
		this.onboardingApprovalStatus = onboardingApprovalStatus;
	}

	public String getLevelOfAssurance() {
		return levelOfAssurance;
	}

	public void setLevelOfAssurance(String levelOfAssurance) {
		this.levelOfAssurance = levelOfAssurance;
	}

	public String getCertStatus() {
		return certStatus;
	}

	public void setCertStatus(String certStatus) {
		this.certStatus = certStatus;
	}

	public String getOnboardingPaymentStatus() {
		return onboardingPaymentStatus;
	}

	public void setOnboardingPaymentStatus(String onboardingPaymentStatus) {
		this.onboardingPaymentStatus = onboardingPaymentStatus;
	}

	public CertificateDetailDto getCertificateDetailDto() {
		return certificateDetailDto;
	}

	public void setCertificateDetailDto(CertificateDetailDto certificateDetailDto) {
		this.certificateDetailDto = certificateDetailDto;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public String getEmailId() {
		return emailId;
	}

	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}

	@Override
	public String toString() {
		return "SubscriberObRequestDTO [suID=" + suID + ", onboardingMethod=" + onboardingMethod + ", mobileNo="
				+ mobileNo + ", emailId=" + emailId + ", subscriberData=" + subscriberData + ", templateId="
				+ templateId + ", consentId=" + consentId + ", acknowledgement=" + acknowledgement + ", subscriberType="
				+ subscriberType + ", onboardingApprovalStatus=" + onboardingApprovalStatus + ", certStatus="
				+ certStatus + ", onboardingPaymentStatus=" + onboardingPaymentStatus + ", levelOfAssurance="
				+ levelOfAssurance + ", subscriberDocuments=" + subscriberDocuments + ", certificateDetailDto="
				+ certificateDetailDto + "]";
	}
}

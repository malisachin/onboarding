package ug.daes.onboarding.dto;

import java.util.Date;


public class ConsentHistoryDto {

    private int id;


    private int consentId;


    private String consent;


    private Date createdOn;


    private String termsAndConditionsBase64;


    private String dataPrivacyBase64;


    private String consentType;


    private boolean consentRequired;


    private String privacyConsent;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConsentId() {
        return consentId;
    }

    public void setConsentId(int consentId) {
        this.consentId = consentId;
    }

    public String getConsent() {
        return consent;
    }

    public void setConsent(String consent) {
        this.consent = consent;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public String getTermsAndConditionsBase64() {
        return termsAndConditionsBase64;
    }

    public void setTermsAndConditionsBase64(String termsAndConditionsBase64) {
        this.termsAndConditionsBase64 = termsAndConditionsBase64;
    }

    public String getDataPrivacyBase64() {
        return dataPrivacyBase64;
    }

    public void setDataPrivacyBase64(String dataPrivacyBase64) {
        this.dataPrivacyBase64 = dataPrivacyBase64;
    }

    public String getConsentType() {
        return consentType;
    }

    public void setConsentType(String consentType) {
        this.consentType = consentType;
    }

    public boolean isConsentRequired() {
        return consentRequired;
    }

    public void setConsentRequired(boolean consentRequired) {
        this.consentRequired = consentRequired;
    }

    public String getPrivacyConsent() {
        return privacyConsent;
    }

    public void setPrivacyConsent(String privacyConsent) {
        this.privacyConsent = privacyConsent;
    }

    @Override
    public String toString() {
        return "ConsentHistory{" +
                "id=" + id +
                ", consentId=" + consentId +
                ", consent='" + consent + '\'' +
                ", createdOn=" + createdOn +
                ", termsAndConditionsBase64='" + termsAndConditionsBase64 + '\'' +
                ", dataPrivacyBase64='" + dataPrivacyBase64 + '\'' +
                ", consentType='" + consentType + '\'' +
                ", consentRequired=" + consentRequired +
                ", privacyConsent='" + privacyConsent + '\'' +
                '}';
    }
}

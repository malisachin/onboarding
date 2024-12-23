package ug.daes.onboarding.dto;


public class SubscriberConsentsDto {

    private String id;


    private String suid;


    private String consentData;


    private String signedConsentData;


    private int consentId;


    private String createdOn;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSuid() {
        return suid;
    }

    public void setSuid(String suid) {
        this.suid = suid;
    }

    public String getConsentData() {
        return consentData;
    }

    public void setConsentData(String consentData) {
        this.consentData = consentData;
    }

    public String getSignedConsentData() {
        return signedConsentData;
    }

    public void setSignedConsentData(String signedConsentData) {
        this.signedConsentData = signedConsentData;
    }

    public int getConsentId() {
        return consentId;
    }

    public void setConsentId(int consentId) {
        this.consentId = consentId;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public String toString() {
        return "SubscriberConsentsDto{" +
                "id='" + id + '\'' +
                ", suid='" + suid + '\'' +
                ", consentData='" + consentData + '\'' +
                ", signedConsentData='" + signedConsentData + '\'' +
                ", consentId=" + consentId +
                ", createdOn='" + createdOn + '\'' +
                '}';
    }
}

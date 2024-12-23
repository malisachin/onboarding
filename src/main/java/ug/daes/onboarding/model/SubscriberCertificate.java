package ug.daes.onboarding.model;

import java.io.Serializable;
import javax.persistence.*;
import java.util.Date;


/**
 * The persistent class for the subscriber_certificates database table.
 * 
 */
@Entity
@Table(name="subscriber_certificates")
@NamedQuery(name="SubscriberCertificate.findAll", query="SELECT s FROM SubscriberCertificate s")
public class SubscriberCertificate implements Serializable {
	private static final long serialVersionUID = 1L;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="cerificate_expiry_date")
	private Date cerificateExpiryDate;

	@Column(name="certificate_data")
	private String certificateData;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="certificate_issue_date")
	private Date certificateIssueDate;

	@Id
	@Column(name="certificate_serial_number",nullable = false, unique = true)
	private String certificateSerialNumber;

	@Column(name="certificate_status")
	private String certificateStatus;

	@Column(name="certificate_type")
	private String certificateType;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="created_date")
	private Date createdDate;

	@Column(name="pki_key_id")
	private String pkiKeyId;

	private String remarks;

	@Column(name="subscriber_uid")
	private String subscriberUid;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="updated_date")
	private Date updatedDate;

	public SubscriberCertificate() {
	}

	public Date getCerificateExpiryDate() {
		return this.cerificateExpiryDate;
	}

	public void setCerificateExpiryDate(Date cerificateExpiryDate) {
		this.cerificateExpiryDate = cerificateExpiryDate;
	}

	public String getCertificateData() {
		return this.certificateData;
	}

	public void setCertificateData(String certificateData) {
		this.certificateData = certificateData;
	}

	public Date getCertificateIssueDate() {
		return this.certificateIssueDate;
	}

	public void setCertificateIssueDate(Date certificateIssueDate) {
		this.certificateIssueDate = certificateIssueDate;
	}

	public String getCertificateSerialNumber() {
		return this.certificateSerialNumber;
	}

	public void setCertificateSerialNumber(String certificateSerialNumber) {
		this.certificateSerialNumber = certificateSerialNumber;
	}

	public String getCertificateStatus() {
		return this.certificateStatus;
	}

	public void setCertificateStatus(String certificateStatus) {
		this.certificateStatus = certificateStatus;
	}

	public String getCertificateType() {
		return this.certificateType;
	}

	public void setCertificateType(String certificateType) {
		this.certificateType = certificateType;
	}

	public Date getCreatedDate() {
		return this.createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getPkiKeyId() {
		return this.pkiKeyId;
	}

	public void setPkiKeyId(String pkiKeyId) {
		this.pkiKeyId = pkiKeyId;
	}

	public String getRemarks() {
		return this.remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public String getSubscriberUid() {
		return this.subscriberUid;
	}

	public void setSubscriberUid(String subscriberUid) {
		this.subscriberUid = subscriberUid;
	}

	public Date getUpdatedDate() {
		return this.updatedDate;
	}

	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}

}
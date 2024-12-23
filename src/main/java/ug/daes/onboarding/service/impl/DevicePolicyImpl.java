package ug.daes.onboarding.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.model.DevicePolicyModel;
import ug.daes.onboarding.repository.DevicePolicyRepository;
import ug.daes.onboarding.service.iface.DevicePolicyIface;
import ug.daes.onboarding.util.AppUtil;

@Service
public class DevicePolicyImpl implements DevicePolicyIface{
	
	private static Logger logger = LoggerFactory.getLogger(DevicePolicyImpl.class);
	
	/** The Constant CLASS. */
	final static String CLASS = "DevicePolicyImpl";
	
	@Value(value = "${device.update.min.policy}")
	private int minHour;

	@Value(value = "${device.update.max.policy}")
	private int maxHour;
	
	@Autowired
	DevicePolicyRepository devicePolicyRepository; 


	@Override
	public ApiResponse devicePolicyHour(int hour) {
		try {
			logger.info(CLASS+" request hour {}",hour);
			
			if(hour < minHour || hour > maxHour) {
				return AppUtil.createApiResponse(false,"Please enter value between "+minHour+" and "+maxHour, null);
			}
			//Optional<DevicePolicyModel> devicePolicy= Optional.ofNullable(devicePolicyRepository.getDevicePolicyHour());
			DevicePolicyModel devicePolicy= devicePolicyRepository.getDevicePolicyHour();
			if(devicePolicy == null) {
				DevicePolicyModel devicePolicyModel = new DevicePolicyModel();
				devicePolicyModel.setDevicePolicyHour(hour);
				devicePolicyRepository.save(devicePolicyModel);
				return AppUtil.createApiResponse(true," Device Policy updated successfully ", null);
			}else {
				//DevicePolicyModel devicePolicyModel = new DevicePolicyModel();
				devicePolicy.setDevicePolicyHour(hour);
				devicePolicyRepository.save(devicePolicy);
				return AppUtil.createApiResponse(true," Device Policy updated successfully ", null);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return AppUtil.createApiResponse(false," Something went wrong plase try after sometime ", null);
		}
	}

}

package ug.daes.onboarding.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.service.iface.DevicePolicyIface;

@RestController
public class DevicePolicyController {
	
	@Autowired
	DevicePolicyIface devicePolicyIface;
	
	@PostMapping("/api/post/devicepolicy/{hour}")
	public ApiResponse addDevicePolicyHour(@PathVariable int hour) {
		return  devicePolicyIface.devicePolicyHour(hour);
	}

}

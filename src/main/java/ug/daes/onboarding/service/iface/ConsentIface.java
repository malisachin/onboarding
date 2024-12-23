package ug.daes.onboarding.service.iface;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import ug.daes.onboarding.constant.ApiResponse;
import ug.daes.onboarding.dto.ConsentDTO;
import ug.daes.onboarding.model.Consent;

public interface ConsentIface {



    ApiResponse signData(HttpHeaders httpHeaders);
}

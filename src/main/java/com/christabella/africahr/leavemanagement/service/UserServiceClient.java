package com.christabella.africahr.leavemanagement.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.base-url}")
    private String authServiceUrl;

    public String getUserEmail(String userId) {
        return restTemplate.getForObject(authServiceUrl + "/api/v1/users/" + userId + "/email", String.class);
    }

    public String getUserFullName(String userId) {
        return restTemplate.getForObject(authServiceUrl + "/api/v1/users/" + userId + "/fullname", String.class);
    }

    public String getUserAvatar(String userId) {
        String url = authServiceUrl + "/api/v1/auth/users/" + userId + "/avatar";
        return restTemplate.getForObject(url, String.class);
    }

    public String getUserDepartment(String userId) {
        return restTemplate.getForObject(authServiceUrl + "/api/v1/users/" + userId + "/department", String.class);
    }


}


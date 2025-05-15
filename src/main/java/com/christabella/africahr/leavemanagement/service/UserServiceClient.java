package com.christabella.africahr.leavemanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${auth.service.base-url}")
    private String authServiceUrl;


    public String getUserFullName(String userId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + userId + "/fullname";
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching user full name: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserAvatar(String userId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + userId + "/avatar";
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching user avatar: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserDepartment(String userId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + userId + "/department";
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching user department: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<String> getManagersByDepartment(String department) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/role/MANAGER?department=" + department;
            List<String> managers = restTemplate.getForObject(url, List.class);
            return managers != null ? managers : List.of();
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching managers: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<String> getAdmins() {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/role/ADMIN";
            List<String> admins = restTemplate.getForObject(url, List.class);
            return admins != null ? admins : List.of();
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching admins: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<String> getAllManagers() {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/role/MANAGER";
            List<String> managers = restTemplate.getForObject(url, List.class);
            return managers != null ? managers : List.of();
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching all managers: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public List<String> getUsersByRole(String role) {
        try {
            String cleanRole = role.trim().toUpperCase().replace("ROLE_", "");
            String url = authServiceUrl + "/api/v1/auth/users/role/" + cleanRole;
            List<String> users = restTemplate.getForObject(url, List.class);
            return users != null ? users : List.of();
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error fetching users with role {}: {}", role, e.getMessage(), e);
            return List.of();
        }
    }

    public String getUserRole(String userId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + userId + "/role";
            String role = restTemplate.getForObject(url, String.class);
            return role != null ? role.trim().toUpperCase().replace("ROLE_", "") : null;
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching user role: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserEmail(String userId) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/" + userId + "/email";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? (String) response.get("data") : null;
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching user email: {}", e.getMessage(), e);
            return null;
        }
    }

    public String getUserIdByEmail(String email) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/id?email=" + email;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? (String) response.get("data") : null;
        } catch (RestClientException e) {
            log.error("Failed to connect to auth service: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching user ID by email: {}", e.getMessage(), e);
            return null;
        }
    }
}

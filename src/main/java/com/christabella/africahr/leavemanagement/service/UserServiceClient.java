package com.christabella.africahr.leavemanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.util.Collections;

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
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            return response != null ? (String) response.get("data") : null;
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

    /**
     * Get emails of all managers in the system
     */
    public List<String> getManagerEmails() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = authServiceUrl + "/api/v1/auth/users/role/MANAGER/emails";
            
            ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<String>>() {}
            );
            
            List<String> emails = response.getBody();
            if (emails == null || emails.isEmpty()) {
                log.warn("No manager emails found");
                return Collections.emptyList();
            }
            
            return emails;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No managers found in the system");
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            log.error("Client error while fetching manager emails: {}", e.getMessage());
            return Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("Connection error while fetching manager emails: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error while fetching manager emails: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get emails of all admins in the system
     */
    public List<String> getAdminEmails() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            String url = authServiceUrl + "/api/v1/auth/users/role/ADMIN/emails";
            
            ResponseEntity<List<String>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<String>>() {}
            );
            
            List<String> emails = response.getBody();
            if (emails == null || emails.isEmpty()) {
                log.warn("No admin emails found");
                return Collections.emptyList();
            }
            
            return emails;
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("No admins found in the system");
            return Collections.emptyList();
        } catch (HttpClientErrorException e) {
            log.error("Client error while fetching admin emails: {}", e.getMessage());
            return Collections.emptyList();
        } catch (ResourceAccessException e) {
            log.error("Connection error while fetching admin emails: {}", e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error while fetching admin emails: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public String getUserFullNameByEmail(String email) {
        try {
            String url = authServiceUrl + "/api/v1/auth/users/email/" + email;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("data") instanceof Map dataMap) {
                Object nameObj = dataMap.get("name");
                return nameObj != null ? nameObj.toString() : "Approver";
            }
            return "Approver";
        } catch (Exception e) {
            log.error("Failed to fetch full name for email {}: {}", email, e.getMessage());
            return "Approver";
        }
    }
}

package com.connecthub.auth_service.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class MediaServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.media.url:http://localhost:8084}")
    private String mediaServiceUrl;

    public MediaServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String uploadAvatar(UUID userId, MultipartFile file) {
        try {
            HttpHeaders fileHeaders = new HttpHeaders();
            fileHeaders.setContentType(MediaType.parseMediaType(
                    file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE));
            fileHeaders.setContentDispositionFormData("file", file.getOriginalFilename());

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new HttpEntity<Resource>(fileResource, fileHeaders));
            body.add("uploaderId", userId.toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    mediaServiceUrl + "/media/upload/image",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                return null;
            }

            Object url = responseBody.get("url");
            if (url == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Media service did not return an avatar URL");
            }
            return url.toString();
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to upload avatar to media service", e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid avatar file", e);
        }
    }
}


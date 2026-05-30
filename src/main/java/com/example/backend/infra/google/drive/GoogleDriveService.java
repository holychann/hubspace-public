package com.example.backend.infra.google.drive;

import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import com.example.backend.infra.google.dto.GoogleFormResponseDto;
import com.google.auth.oauth2.AccessToken;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public interface GoogleDriveService {
    String getValidAccessToken(String username) throws IOException;
    AccessToken refreshAccessToken(String refreshToken, String username);
    GoogleFormCreateResponseDto createFormInDrive(String username, String formName, List<String> searchColumns, String refreshToken);
    List<GoogleFormResponseDto> getFormResponses(String formId, String accessToken, List<String> searchColumnIds, LocalDateTime lastResponseTime);
    Boolean deleteFormInDrive(String formId, String username);
}

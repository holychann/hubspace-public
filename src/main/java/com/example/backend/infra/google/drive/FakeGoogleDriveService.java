package com.example.backend.infra.google.drive;

import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import com.example.backend.infra.google.dto.GoogleFormQuestionsIdsResponseDto;
import com.example.backend.infra.google.dto.GoogleFormResponseDto;
import com.google.auth.oauth2.AccessToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@Profile("test-mock")
public class FakeGoogleDriveService implements GoogleDriveService {

    @Override
    public String getValidAccessToken(String username) {

        // 토큰 무효
        if (username.contains("INVALID")) {
            refreshAccessToken("INVALID", "username");
        }

        // API 에러
        if (username.contains("API_ERROR")) {
            refreshAccessToken("API_ERROR", "username");
        }

        // 성공
        return refreshAccessToken("VALID", "username").getTokenValue();
    }

    @Override
    public AccessToken refreshAccessToken(String refreshToken, String username) {

        // 실패 시나리오 1: 토큰 무효
        if ("INVALID".equals(refreshToken)) {
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
        }

        // 실패 시나리오 2: API 에러
        if ("API_ERROR".equals(refreshToken)) {
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }

        // 성공 시나리오
        return new AccessToken(
                "FAKE_REFRESHED_ACCESS_TOKEN",
                Date.from(
                        LocalDateTime.now()
                                .plusHours(1)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                )
        );
    }


    @Override
    public GoogleFormCreateResponseDto createFormInDrive(
            String username,
            String formTitle,
            List<String> searchColumns,
            String refreshToken
    ) {
        // 시나리오 제어
        if ("INVALID".equals(refreshToken)) {
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
        }

        if (formTitle.contains("SECURITY")) {
            throw new BusinessException(ErrorCode.GOOGLE_SECURITY_ERROR);
        }

        if (formTitle.contains("API_ERROR")) {
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }

        // 정상 성공 응답
        String fakeFormId = UUID.randomUUID().toString();
        String fakeFormUrl = "https://fake.google.form/" + fakeFormId;

        Map<String, String> columnIdMap = new HashMap<>();
        for (String col : searchColumns) {
            columnIdMap.put(col, "Q_" + col);
        }

        return GoogleFormCreateResponseDto.of(
                fakeFormId,
                fakeFormUrl,
                GoogleFormQuestionsIdsResponseDto.of(columnIdMap)
        );
    }

    @Override
    public List<GoogleFormResponseDto> getFormResponses(
            String formId,
            String accessToken,
            List<String> searchColumnIds,
            LocalDateTime lastResponseTime
    ) {
        // 단위 테스트에서는 “응답이 있다/없다”만 있으면 충분
        return List.of(
                GoogleFormResponseDto.of(
                        "RESPONSE_1",
                        LocalDateTime.now().toString(),
                        Map.of(searchColumnIds.get(0), "FAKE_VALUE")
                )
        );
    }

    @Override
    public Boolean deleteFormInDrive(String formId, String username) {
        if (formId.contains("ERROR")) {
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }
        return true;
    }
}

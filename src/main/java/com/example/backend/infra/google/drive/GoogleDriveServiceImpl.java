package com.example.backend.infra.google.drive;

import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import com.example.backend.infra.google.dto.GoogleFormQuestionsIdsResponseDto;
import com.example.backend.infra.google.dto.GoogleFormResponseDto;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.model.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleDriveServiceImpl implements GoogleDriveService{

    @Value("${google.application-name}")
    private String APPLICATION_NAME;

    private final UserService userService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    /**
     * 유효한 Access Token 을 반환합니다.
     * @param username
     * @return
     * @throws IOException
     */
    @Override
    public String getValidAccessToken(String username) throws IOException {

        UserEntity user = userService.findUserByUsername(username);

        AccessToken accessToken = null;
        LocalDateTime expiresAt = null;

        try {
            if(user.getGoogleAccessToken() != null &&
                    user.getAccessTokenExpiresAt().isAfter(LocalDateTime.now().plusMinutes(5))){

                return user.getGoogleAccessToken();
            }

            accessToken = refreshAccessToken(user.getGoogleRefreshToken(), username);
            Date expirationTime = accessToken.getExpirationTime();
            expiresAt = LocalDateTime.ofInstant(
                    expirationTime.toInstant(),
                    ZoneId.systemDefault()
            );
        } catch (BusinessException e) {
            log.error("❌ Google API Error | Message: 이런 줸장 {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Google API Error | Message: 이런 줸장 {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT, e.getMessage());
        }

        if(accessToken != null && accessToken.getTokenValue() != null){
            userService.updateAccessToken(username, accessToken.getTokenValue(), expiresAt);

            return accessToken.getTokenValue();
        }

        throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
    }

    /**
     * Access Token 만료 시 재발급
     * @param refreshToken
     * @return
     * @throws IOException
     */
    @Override
    public AccessToken refreshAccessToken(String refreshToken, String username) {

        log.info("🌐 [GOOGLE][ACCESS TOKEN][START] 발급 시작 | Username: {}", username);

        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            credentials.refreshIfExpired();

            AccessToken accessToken = credentials.getAccessToken();

            if (accessToken == null || accessToken.getTokenValue() == null) {
                log.debug("❌ [GOOGLE][ACCESS_TOKEN][ERROR] 액세스 토큰이 비어있음");
                throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
            }

            log.info("🌐 [GOOGLE][ACCESS_TOKEN][END] 발급 완료 | Username: {}", username);

            return accessToken;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }


    }

    /**
     * Google Form 파일 생성
     * @param username 사용자 이름(예시: GOOGLE_1235235131)
     * @param formTitle 파일명
     * @param searchColumns 검색할 컬럼명 목록
     * @param refreshToken Refresh Token
     * @return Google Form 파일 생성 결과(url, id)
     */
    @Override
    public GoogleFormCreateResponseDto createFormInDrive(String username, String formTitle, List<String> searchColumns, String refreshToken) {

        try {
            log.info("📋 [GOOGLE][FORM][START] Google Form 파일 생성 시작 | username: {}, formName: {}", username, formTitle);
            String accessToken = getValidAccessToken(username);

            Drive driveService = createDriveServiceInstance(accessToken);

            // List[0]: 파일 ID, List[1]: 파일 URL
            List<String> formdata = createGoogleFormFile(driveService, formTitle);
            String formId = formdata.get(0);
            String formUrl = formdata.get(1);
            log.info("📋 [GOOGLE][FORM][END] Google Form 파일 생성 완료 | formId: {}", formId);

            // 질문 추가
            GoogleFormQuestionsIdsResponseDto googleFormQuestionsIdsResponseDto = addQuestionsToForm(formId, searchColumns, accessToken);

            // 응답 목록 반환
            return GoogleFormCreateResponseDto.of(formId, formUrl, googleFormQuestionsIdsResponseDto);
        } catch (IOException e){
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
        } catch (GeneralSecurityException e){
            throw new BusinessException(ErrorCode.GOOGLE_SECURITY_ERROR);
        } catch (Exception e){
            log.error("🔥 Google API error", e);
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }
    }

    /**
     * Drive Service 인스턴스 생성
     * @param accessToken Valid Access Token
     * @return Drive Service 인스턴스
     */
    private Drive createDriveServiceInstance(String accessToken) throws GeneralSecurityException, IOException {

        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Google Form 파일 생성
     * @param drive Drive Service 인스턴스
     * @param formTitle 파일명
     * @return List[파일 ID, 파일 URL]
     */
    private List<String> createGoogleFormFile(Drive drive, String formTitle) throws Exception {
        File fileMetadata = new File();
        fileMetadata.setName(formTitle);
        fileMetadata.setMimeType("application/vnd.google-apps.form");

        File file = drive.files().create(fileMetadata)
                .setFields("id, webViewLink")
                .execute();

        List<String> metadata = new ArrayList<>();
        metadata.add(file.getId());
        metadata.add(file.getWebViewLink());

        return metadata;
    }

    /**
     * Google Form Service 인스턴스 생성
     * @param accessToken Valid Access Token
     * @return Forms Service 인스턴스
     */
    private Forms createFormsService(String accessToken) throws GeneralSecurityException, IOException {
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null));

        return new Forms.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Google Form에 질문 추가
     * @param formId Google Form ID
     * @param searchColumns 검색용 컬럼명 목록
     * @param accessToken Valid Access Token
     */
    private GoogleFormQuestionsIdsResponseDto addQuestionsToForm(String formId, List<String> searchColumns, String accessToken) throws Exception {

        Forms formsService = createFormsService(accessToken);

        List<Request> requests = new ArrayList<>();

        // 배열 순서대로 질문 추가 요청 만들기
        for (int i = 0; i < searchColumns.size(); i++) {
            String colName = searchColumns.get(i);

            // "주관식 단답형(TextQuestion)" 질문 생성
            Request request = new Request()
                    .setCreateItem(new CreateItemRequest()
                            .setItem(new Item()
                                    .setTitle(colName) // 질문 제목 (예: 이름, 학번)
                                    .setQuestionItem(new QuestionItem()
                                            .setQuestion(new Question()
                                                    .setRequired(true) // 필수 응답 여부
                                                    .setTextQuestion(new TextQuestion())) // 텍스트 입력형
                                    )
                            )
                            .setLocation(new Location().setIndex(i)) // 순서 지정
                    );
            requests.add(request);
        }

        BatchUpdateFormRequest batchRequest = new BatchUpdateFormRequest().setRequests(requests);
        BatchUpdateFormResponse response = formsService.forms().batchUpdate(formId, batchRequest).execute();

        Map<String, String> columnIdMap = new HashMap<>();
        List<Response> replies = response.getReplies();

        if (replies != null) {
            for (int i = 0; i < searchColumns.size(); i++) {
                // 요청했던 컬럼명
                String colName = searchColumns.get(i);

                // 그에 해당하는 응답 (순서가 보장됨)
                Response reply = replies.get(i);

                // 계층 구조를 타고 내려가서 ID 추출
                // Response -> CreateItemResponse -> Item -> QuestionItem -> Question -> QuestionId
                CreateItemResponse createItemResponse = reply.getCreateItem();

                List<String> questionIds = createItemResponse.getQuestionId();
                if (questionIds != null && !questionIds.isEmpty()) {
                    String questionId = questionIds.get(0);
                    columnIdMap.put(colName, questionId);
                }
            }
        }

        return GoogleFormQuestionsIdsResponseDto.of(columnIdMap);

    }

    /**
     * Google Form에 답변을 저장한 응답 목록을 반환합니다.
     * @param formId Google Form ID
     * @param accessToken Valid Access Token
     * @param searchColumnIds 검색용 컬럼 ID 목록
     * @return 응답 목록 List
     */
    public List<GoogleFormResponseDto> getFormResponses(String formId, String accessToken, List<String> searchColumnIds, LocalDateTime lastResponseTime) {

        log.info("🌐 [GOOGLE][FROM_RESPONSES][START] 최신 응답 목록 조회 시작 | formId: {}", formId);

        try {
            Forms formsService = createFormsService(accessToken);

            // 응답 목록 조회 요청
            ListFormResponsesResponse rawResponses = formsService.forms().responses().list(formId)
                    .setPageSize(500)
                    .execute();

            List<FormResponse> responses = rawResponses.getResponses();
            List<GoogleFormResponseDto> responseDtoList = new ArrayList<>();

            if (rawResponses == null || rawResponses.isEmpty()) {
                log.info("⚠️ 아직 응답이 없습니다. | formId: {}", formId);

                // 비어있다면 깡통 List 반환
                return responseDtoList;
            }

            for (FormResponse raw : responses) {
                LocalDateTime responseTime = OffsetDateTime.parse(raw.getCreateTime()).toLocalDateTime();


                // 최근 응답 이전의 응답은 무시
                if(!responseTime.isAfter(lastResponseTime)) {
                    continue;
                }

                Map<String, String> parsedAnswers = new LinkedHashMap<>();

                // 답변이 있는 경우에만 처리
                if (raw.getAnswers() != null) {
                    for (String searchColumnId : searchColumnIds) {
                        Answer answerObj = raw.getAnswers().get(searchColumnId);
                        if (answerObj == null) {
                            continue;
                        }

                        String textValue = extractTextValue(answerObj);
                        parsedAnswers.put(searchColumnId, textValue);
                    }
                }

                responseDtoList.add(GoogleFormResponseDto.of(
                        raw.getResponseId(),
                        raw.getCreateTime(),
                        parsedAnswers
                ));
            }

            log.info("🌐 [GOOGLE][FORM_RESPONSES][END] 최신 응답 목록 조회 완료 | responseCount: {}, formId: {}", responseDtoList.size(), formId);

            return responseDtoList;

        } catch (IOException e) {
            log.error("❌ [GOOGLE][FORM_RESPONSES][ERROR] Google API 통신 오류 | 메시지: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
        } catch (GeneralSecurityException e) {
            log.error("❌ [GOOGLE][FORM_RESPONSES][ERROR] Google API SSL/보안 설정 초기화 실패 | 메시지: {}", e.getMessage());
            throw new BusinessException(ErrorCode.GOOGLE_SECURITY_ERROR);
        } catch (Exception e) {
            log.error("❌ [GOOGLE][FORM_RESPONSES][ERROR] 예상치 못한 오류, e: {}", e);
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }

    }

    /**
     * Answer 객체에서 텍스트 값만 추출하는 헬퍼 메소드
     * @param answer Answer 객체
     * @return 추출된 텍스트 값
     */
    private String extractTextValue(Answer answer) {
        // 텍스트 답변이 있는 경우 (주관식, 객관식 등)
        if (answer.getTextAnswers() != null && answer.getTextAnswers().getAnswers() != null) {
            List<String> values = new ArrayList<>();
            for (TextAnswer textAnswer : answer.getTextAnswers().getAnswers()) {
                values.add(textAnswer.getValue());
            }
            // 값이 여러 개면 콤마(,)로 연결, 하나면 그냥 반환
            return String.join(", ", values);
        }
        // TODO: 그리드형, 날짜형 등 다른 타입의 답변 처리 로직 추가 가능
        return "";
    }

    @Override
    public Boolean deleteFormInDrive(String formId, String username){

        log.info("🌐[GOOGLE][DELETE_FORM][START] 구글 폼 삭제 시도 | formId: {}", formId);

        try {

            String accessToken = getValidAccessToken(username);

            Drive driveService = createDriveServiceInstance(accessToken);
            driveService.files().delete(formId).execute();

        } catch(IOException e){
            throw new BusinessException(ErrorCode.GOOGLE_INVALID_GRANT);
        } catch (GeneralSecurityException e){
            throw new BusinessException(ErrorCode.GOOGLE_SECURITY_ERROR);
        } catch (Exception e){
            throw new BusinessException(ErrorCode.GOOGLE_API_ERROR);
        }

        log.info("🌐 [GOOGLE][DELETE_FORM][END] 구글 폼 삭제 완료");

        return true;
    }
}

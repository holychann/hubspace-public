package com.example.backend.application.facade;

import com.example.backend.domain.event.converter.EventResponseConverter;
import com.example.backend.domain.event.dto.EventDetail;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.event.service.command.EventCommandService;
import com.example.backend.domain.event.service.command.file.EventFileCommandService;
import com.example.backend.domain.event.service.query.EventQueryService;
import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.domain.response.dto.ResponseSaveDto;
import com.example.backend.domain.response.service.command.ResponseCommandService;
import com.example.backend.domain.response.service.query.ResponseQueryService;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.domain.user.service.UserService;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.google.drive.GoogleDriveService;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.example.backend.domain.event.dto.EventResponseDto.*;

/**
 * 이벤트 Facade 구현 클래스
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventFacadeImpl implements EventFacade{

    private final int MAX_RESPONSE_COUNT = 10;

    private final EventQueryService eventQueryService;
    private final UserService userService;
    private final GoogleDriveService googleDriveService;
    private final EventCommandService eventCommandService;
    private final EventFileCommandService eventFileCommandService;
    private final ResponseCommandService responseCommandService;
    private final EventResponseConverter eventResponseConverter;
    private final ResponseQueryService responseQueryService;


    /**
     * 이벤트 리스트 조회
     *
     * @param username : 사용자 username
     * @return SearchList | 이벤트가 없을 경우 null 
     */
    @Override
    public SearchList getEventList(String username) {

        // 최적화를 위해 제거 @AuthenticationPrincipal 로 대체
//        UserEntity userEntity = userService.findUserByUsername(username);

        return eventQueryService.getEventList(username);
    }

    /**
     * 이벤트 상세 정보 조회
     *
     * @param username : 사용자 username
     * @param eventId : 이벤트 ID
     * @return EventDetail
     */
    @Override
    public EventDetail getEventDetail(String username, Long eventId) {

        // 최적화를 위해 제거 @AuthenticationPrincipal 로 대체
//        UserEntity userEntity = userService.findUserByUsername(username);

        EventDetail eventDetail = eventQueryService.getEventDetail(username, eventId);

        if(eventDetail.getEventType() == EventType.FILE){
            List<ResponseDto> responses = responseQueryService.getResponses(eventId);

            EventDetail fileDetailController = eventResponseConverter.toFileDetailController(eventDetail, responses);

            return fileDetailController;
        }

        return eventDetail;
    }

    /**
     * Google Form 이벤트 생성
     *
     * @param username : 사용자 username
     * @param eventRequestDto : Google Form 생성 요청 DTO
     * @return CreatedFormEvent
     */
    @Override
    public CreatedFormEvent createFormEvent(String username, EventRequestDto.FormEvent eventRequestDto) {

        UserEntity userEntity = null;
        GoogleFormCreateResponseDto googleFormResponse = null;
        CreatedFormEvent formEvent = null;
        try{
             userEntity = userService.findUserByUsername(username);

             googleFormResponse = googleDriveService.createFormInDrive(
                    userEntity.getUsername(),
                    eventRequestDto.getEventTitle(),
                    eventRequestDto.getSearchColumns(),
                    userEntity.getGoogleRefreshToken());

             formEvent = eventCommandService.createFormEvent(userEntity, eventRequestDto, googleFormResponse);

            return formEvent;
        } catch (Exception e){
            log.error(
                    "❌[EVENT][CREATE][FAIL] 이벤트 생성 실패 | username: {}, eventTitle: {}",
                    username,
                    eventRequestDto.getEventTitle(),
                    e
            );
            // 이벤트 생성 후 에러가 발생하였다면, DB 에서 이벤트 정보 삭제
            if(formEvent != null){
                log.warn(
                        "⚠️[EVENT][COMPENSATION][DB] 이벤트 롤백 수행 | eventId: {}, username: {}",
                        formEvent.getEventId(),
                        username
                );
                eventCommandService.deleteEvent(username, formEvent.getEventId());
            }

            // 구글 폼 생성이 완료되었는데 에러가 발생하였다면, 구글 폼 삭제
            if(googleFormResponse != null){
                log.warn(
                        "⚠️[EVENT][COMPENSATION][GOOGLE_FORM] 구글 폼 삭제 수행 | formId: {}, username: {}",
                        googleFormResponse.getFormId(),
                        username
                );
                googleDriveService.deleteFormInDrive(googleFormResponse.getFormId(), userEntity.getGoogleAccessToken());
            }

            throw new BusinessException(ErrorCode.EVENT_CREATE_FAILED);
        }

    }

    /**
     * CSV, TSV 이벤트 생성 메서드
     * TODO: 보상 추가해야해요.
     *
     * @param username : 사용자 username
     * @param eventRequestDto : 이벤트 생성 요청 DTO
     * @return CreatedFileEvent DTO
     */
    @Override
    public ResponseSaveDto createFileEvent(String username, EventRequestDto.FileEvent eventRequestDto, MultipartFile file) {

        // 유저 엔티티 조회
        UserEntity userEntity = userService.findUserByUsername(username);

        // 이벤트 생성
        CreatedFileEvent fileEventResponse = eventFileCommandService.createFileEvent(userEntity, eventRequestDto);

        Long eventId = fileEventResponse.getEventId();

        // 응답 저장
        responseCommandService.saveFileResponses(file, eventRequestDto.getSearchColumns(), eventId);

        return ResponseSaveDto.of(fileEventResponse.getEventId());
    }
}

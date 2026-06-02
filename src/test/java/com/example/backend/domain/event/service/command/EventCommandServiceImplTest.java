package com.example.backend.domain.event.service.command;

import com.example.backend.domain.event.EventTestFixture;
import com.example.backend.domain.event.converter.EventRequestConverter;
import com.example.backend.domain.event.converter.EventResponseConverter;
import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.repository.command.EventCommandRepository;
import com.example.backend.domain.event.repository.command.EventMetadataCommandRepository;
import com.example.backend.domain.user.entity.UserEntity;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.google.dto.GoogleFormCreateResponseDto;
import com.example.backend.infra.redis.EventCountCache;
import com.example.backend.infra.redis.EventMetadataCache;
import com.example.backend.infra.redis.EventUserEventIdsCache;
import com.example.backend.infra.redis.EventViewCountCache;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.backend.domain.event.EventTestFixture.EVENT_ID;
import static com.example.backend.domain.event.EventTestFixture.OTHER_USERNAME;
import static com.example.backend.domain.event.EventTestFixture.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EventCommandServiceImplTest {

    @Mock private EventCommandRepository eventCommandRepository;
    @Mock private EventMetadataCommandRepository eventMetadataCommandRepository;
    @Mock private EventRequestConverter eventRequestConverter;
    @Mock private EventResponseConverter eventResponseConverter;
    @Mock private EventMetadataCache eventMetadataCache;
    @Mock private EventCountCache eventCountCache;
    @Mock private EventViewCountCache eventViewCountCache;
    @Mock private EventUserEventIdsCache eventUserEventIdsCache;

    @InjectMocks private EventCommandServiceImpl sut;

    private void verifyAllEventCachesEvicted(String username, Long eventId) {
        verify(eventUserEventIdsCache).evict(username);
        verify(eventMetadataCache).evict(eventId);
        verify(eventCountCache).evict(eventId);
        verify(eventViewCountCache).evict(eventId);
    }

    @Nested
    @DisplayName("createFormEvent")
    class CreateFormEvent {

        @Test
        @DisplayName("폼 이벤트 생성 후 컨버터 결과를 반환하고 4종 캐시를 모두 evict 한다")
        void success() {
            UserEntity user = EventTestFixture.user();
            EventRequestDto.FormEvent request = EventTestFixture.formRequest();
            GoogleFormCreateResponseDto google = EventTestFixture.googleFormResponse();
            EventEntity preSave = EventTestFixture.formEvent();
            EventMetadataEntity metadata = EventTestFixture.metadata();
            EventResponseDto.CreatedFormEvent expected =
                    EventResponseDto.CreatedFormEvent.builder().eventId(EVENT_ID).formUrl("url").build();

            given(eventRequestConverter.formDtoToEntity(request, user)).willReturn(preSave);
            given(eventRequestConverter.formDtoToMetadataEntity(request, preSave, google)).willReturn(metadata);
            given(eventCommandRepository.save(preSave)).willReturn(preSave);
            given(eventResponseConverter.toCreatedFormEventDto(preSave, google)).willReturn(expected);

            EventResponseDto.CreatedFormEvent result = sut.createFormEvent(user, request, google);

            assertThat(result).isSameAs(expected);
            verify(eventMetadataCommandRepository).save(metadata);
            verifyAllEventCachesEvicted(USERNAME, EVENT_ID);
        }
    }

    @Nested
    @DisplayName("updatePollingData")
    class UpdatePollingData {

        @Test
        @DisplayName("delta > 0 이면 count 가 증가하고 polling/lastResponse 시간이 갱신된다")
        void increasesCountWhenDeltaPositive() {
            EventEntity event = spy(EventTestFixture.formEvent());
            EventMetadataEntity metadata = spy(EventTestFixture.metadata(3L));
            LocalDateTime next = LocalDateTime.now().plusMinutes(10);
            LocalDateTime lastResp = LocalDateTime.now();

            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
            given(eventMetadataCommandRepository.findByEventId(EVENT_ID)).willReturn(metadata);

            sut.updatePollingData(EVENT_ID, next, lastResp, 5L);

            verify(metadata).updateCount(5L);
            verify(event).updateNextPollingAt(next);
            verify(event).updateLastResponseTime(lastResp);
        }

        @Test
        @DisplayName("delta <= 0 이면 count 는 갱신되지 않고 polling 시간만 갱신된다")
        void doesNotUpdateCountWhenDeltaNonPositive() {
            EventEntity event = spy(EventTestFixture.formEvent());
            EventMetadataEntity metadata = spy(EventTestFixture.metadata(10L));

            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
            given(eventMetadataCommandRepository.findByEventId(EVENT_ID)).willReturn(metadata);

            sut.updatePollingData(EVENT_ID, LocalDateTime.now(), LocalDateTime.now(), 10L);

            verify(metadata, never()).updateCount(any());
            verify(event).updateNextPollingAt(any());
            verify(event).updateLastResponseTime(any());
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenEventMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updatePollingData(EVENT_ID, LocalDateTime.now(), LocalDateTime.now(), 1L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("메타데이터가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenMetadataMissing() {
            EventEntity event = EventTestFixture.formEvent();
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
            given(eventMetadataCommandRepository.findByEventId(EVENT_ID)).willReturn(null);

            assertThatThrownBy(() -> sut.updatePollingData(EVENT_ID, LocalDateTime.now(), LocalDateTime.now(), 1L))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("updateEventStatus")
    class UpdateEventStatus {

        @Test
        @DisplayName("이벤트의 활성 상태를 갱신한다")
        void success() {
            EventEntity event = spy(EventTestFixture.formEvent());
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            sut.updateEventStatus(EVENT_ID, false);

            verify(event).updateIsActive(false);
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenEventMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateEventStatus(EVENT_ID, true))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("updateNextPollingAt")
    class UpdateNextPollingAt {

        @Test
        @DisplayName("다음 폴링 시간을 갱신한다")
        void success() {
            EventEntity event = spy(EventTestFixture.formEvent());
            LocalDateTime next = LocalDateTime.now().plusMinutes(7);
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            sut.updateNextPollingAt(EVENT_ID, next);

            verify(event).updateNextPollingAt(next);
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenEventMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateNextPollingAt(EVENT_ID, LocalDateTime.now()))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("deleteEvent")
    class DeleteEvent {

        @Test
        @DisplayName("본인 이벤트는 metadata, event 순으로 삭제되고 4종 캐시가 모두 evict 된다")
        void success() {
            EventEntity event = EventTestFixture.formEvent();
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            sut.deleteEvent(USERNAME, EVENT_ID);

            verify(eventMetadataCommandRepository).deleteByEventId(EVENT_ID);
            verify(eventCommandRepository).deleteById(EVENT_ID);
            verifyAllEventCachesEvicted(USERNAME, EVENT_ID);
        }

        @Test
        @DisplayName("다른 사용자의 이벤트는 EVENT_FORBIDDEN 이 발생한다")
        void throwsWhenForbidden() {
            EventEntity event = EventTestFixture.formEvent();
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            assertThatThrownBy(() -> sut.deleteEvent(OTHER_USERNAME, EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_FORBIDDEN.getCode()));

            verify(eventMetadataCommandRepository, never()).deleteByEventId(any());
            verify(eventCommandRepository, never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.deleteEvent(USERNAME, EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("updateFormEvent")
    class UpdateFormEvent {

        @Test
        @DisplayName("본인 이벤트는 폼 정보가 수정되고 캐시가 evict 된다")
        void success() {
            EventEntity event = spy(EventTestFixture.formEvent());
            EventRequestDto.FormEvent request = EventTestFixture.formRequest();
            EventResponseDto.Update expected = EventResponseDto.Update.builder().eventId(EVENT_ID).build();

            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
            given(eventResponseConverter.toUpdateSuccessDto(event)).willReturn(expected);

            EventResponseDto.Update result = sut.updateFormEvent(USERNAME, EVENT_ID, request);

            assertThat(result).isSameAs(expected);
            verify(event).updateFormEvent(request);
            verifyAllEventCachesEvicted(USERNAME, EVENT_ID);
        }

        @Test
        @DisplayName("다른 사용자가 호출하면 EVENT_FORBIDDEN 이 발생한다")
        void throwsWhenForbidden() {
            EventEntity event = EventTestFixture.formEvent();
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            assertThatThrownBy(() -> sut.updateFormEvent(OTHER_USERNAME, EVENT_ID, EventTestFixture.formRequest()))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_FORBIDDEN.getCode()));

            verifyNoInteractions(eventMetadataCache, eventCountCache, eventViewCountCache, eventUserEventIdsCache);
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.updateFormEvent(USERNAME, EVENT_ID, EventTestFixture.formRequest()))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("closeEvent")
    class CloseEvent {

        @Test
        @DisplayName("본인 이벤트는 비활성화되고 캐시가 evict 된다")
        void success() {
            EventEntity event = spy(EventTestFixture.formEvent());
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            sut.closeEvent(USERNAME, EVENT_ID);

            verify(event).updateIsActive(false);
            verifyAllEventCachesEvicted(USERNAME, EVENT_ID);
        }

        @Test
        @DisplayName("다른 사용자가 호출하면 EVENT_FORBIDDEN 이 발생한다")
        void throwsWhenForbidden() {
            EventEntity event = EventTestFixture.formEvent();
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            assertThatThrownBy(() -> sut.closeEvent(OTHER_USERNAME, EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_FORBIDDEN.getCode()));
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.closeEvent(USERNAME, EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("addViewCount")
    class AddViewCount {

        @Test
        @DisplayName("메타데이터의 viewCount 를 증가시키고 캐시에 반영한다")
        void success() {
            EventMetadataEntity metadata = spy(EventTestFixture.metadata());
            given(eventMetadataCommandRepository.findByEventId(EVENT_ID)).willReturn(metadata);

            sut.addViewCount(EVENT_ID);

            verify(metadata).addViewCount();
            verify(eventViewCountCache).put(EVENT_ID, metadata.getViewCount());
        }
    }

    @Nested
    @DisplayName("expireEventIds")
    class ExpireEventIds {

        @Test
        @DisplayName("repository 가 반환한 갱신 개수를 그대로 반환한다")
        void returnsUpdatedCount() {
            List<Long> ids = List.of(1L, 2L, 3L);
            given(eventCommandRepository.updateIsActiveFalseByIds(any(), any())).willReturn(3L);

            long result = sut.expireEventIds(ids);

            assertThat(result).isEqualTo(3L);
        }

        @Test
        @DisplayName("입력 개수와 갱신 개수가 다르더라도 갱신 개수를 반환한다")
        void returnsUpdatedCountEvenIfMismatch() {
            List<Long> ids = List.of(1L, 2L, 3L);
            given(eventCommandRepository.updateIsActiveFalseByIds(any(), any())).willReturn(2L);

            long result = sut.expireEventIds(ids);

            assertThat(result).isEqualTo(2L);
            verify(eventCommandRepository, times(1)).updateIsActiveFalseByIds(any(), any());
        }
    }
}

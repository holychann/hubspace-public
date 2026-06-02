package com.example.backend.domain.event.service.command.file;

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

import java.util.Optional;

import static com.example.backend.domain.event.EventTestFixture.EVENT_ID;
import static com.example.backend.domain.event.EventTestFixture.OTHER_USERNAME;
import static com.example.backend.domain.event.EventTestFixture.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventFileCommandServiceImplTest {

    @Mock private EventCommandRepository eventCommandRepository;
    @Mock private EventMetadataCommandRepository eventMetadataCommandRepository;
    @Mock private EventRequestConverter eventRequestConverter;
    @Mock private EventResponseConverter eventResponseConverter;
    @Mock private EventMetadataCache eventMetadataCache;
    @Mock private EventCountCache eventCountCache;
    @Mock private EventViewCountCache eventViewCountCache;
    @Mock private EventUserEventIdsCache eventUserEventIdsCache;

    @InjectMocks private EventFileCommandServiceImpl sut;

    private void verifyAllEventCachesEvicted(String username, Long eventId) {
        verify(eventUserEventIdsCache).evict(username);
        verify(eventMetadataCache).evict(eventId);
        verify(eventCountCache).evict(eventId);
        verify(eventViewCountCache).evict(eventId);
    }

    @Nested
    @DisplayName("createFileEvent")
    class CreateFileEvent {

        @Test
        @DisplayName("파일 이벤트를 저장하고 4종 캐시를 evict 한 뒤 컨버터 결과를 반환한다")
        void success() {
            UserEntity user = EventTestFixture.user();
            EventRequestDto.FileEvent request = EventTestFixture.fileRequest();
            EventEntity preSave = EventTestFixture.fileEvent();
            EventMetadataEntity metadata = EventTestFixture.metadata();
            EventResponseDto.CreatedFileEvent expected =
                    EventResponseDto.CreatedFileEvent.builder().eventId(EVENT_ID).build();

            given(eventRequestConverter.fileDtoToEntity(request, user)).willReturn(preSave);
            given(eventRequestConverter.fileDtoToMetadataEntity(request, preSave)).willReturn(metadata);
            given(eventCommandRepository.save(preSave)).willReturn(preSave);
            given(eventResponseConverter.entityToCreatedFileEvent(preSave)).willReturn(expected);

            EventResponseDto.CreatedFileEvent result = sut.createFileEvent(user, request);

            assertThat(result).isSameAs(expected);
            verify(eventMetadataCommandRepository).save(metadata);
            verifyAllEventCachesEvicted(USERNAME, EVENT_ID);
        }
    }

    @Nested
    @DisplayName("simpleUpdateFileEvent")
    class SimpleUpdateFileEvent {

        @Test
        @DisplayName("본인 이벤트는 제목이 수정되고 4종 캐시가 evict 된 뒤 컨버터 결과가 반환된다")
        void success() {
            EventEntity event = spy(EventTestFixture.fileEvent());
            EventRequestDto.UpdateFileEvent request = EventTestFixture.updateFileRequest("새제목");
            EventResponseDto.UpdateFileEvent expected =
                    EventResponseDto.UpdateFileEvent.builder().eventId(EVENT_ID).build();

            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));
            given(eventResponseConverter.toUpdateFileEvent(EVENT_ID)).willReturn(expected);

            EventResponseDto.UpdateFileEvent result = sut.simpleUpdateFileEvent(USERNAME, EVENT_ID, request);

            assertThat(result).isSameAs(expected);
            verify(event).updateEventTitle("새제목");
            verifyAllEventCachesEvicted(USERNAME, EVENT_ID);
        }

        @Test
        @DisplayName("다른 사용자가 호출하면 EVENT_FORBIDDEN 이 발생한다")
        void throwsWhenForbidden() {
            EventEntity event = EventTestFixture.fileEvent();
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            assertThatThrownBy(() -> sut.simpleUpdateFileEvent(OTHER_USERNAME, EVENT_ID,
                    EventTestFixture.updateFileRequest("새제목")))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_FORBIDDEN.getCode()));
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenMissing() {
            given(eventCommandRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.simpleUpdateFileEvent(USERNAME, EVENT_ID,
                    EventTestFixture.updateFileRequest("새제목")))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }
}

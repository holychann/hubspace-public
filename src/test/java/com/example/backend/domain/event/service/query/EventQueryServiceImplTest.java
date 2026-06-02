package com.example.backend.domain.event.service.query;

import com.example.backend.domain.event.EventTestFixture;
import com.example.backend.domain.event.converter.EventResponseConverter;
import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.event.dto.EventWithMetadataDto;
import com.example.backend.domain.event.entity.EventEntity;
import com.example.backend.domain.event.entity.EventMetadataEntity;
import com.example.backend.domain.event.entity.EventType;
import com.example.backend.domain.event.repository.query.EventMetadataQueryRepository;
import com.example.backend.domain.event.repository.query.EventQueryRepository;
import com.example.backend.global.error.BusinessException;
import com.example.backend.global.error.ErrorCode;
import com.example.backend.infra.redis.EventCountCache;
import com.example.backend.infra.redis.EventMetadataCache;
import com.example.backend.infra.redis.EventSearchColumnsCache;
import com.example.backend.infra.redis.EventUserEventIdsCache;
import com.example.backend.infra.redis.EventViewCountCache;
import com.example.backend.infra.redis.dto.EventMetadataCacheDto;
import com.example.backend.infra.redis.helper.RedisBatchContext;
import com.example.backend.infra.redis.helper.RedisPipeline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.example.backend.domain.event.EventTestFixture.EVENT_ID;
import static com.example.backend.domain.event.EventTestFixture.USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EventQueryServiceImplTest {

    @Mock private EventQueryRepository eventQueryRepository;
    @Mock private EventResponseConverter eventResponseConverter;
    @Mock private EventMetadataQueryRepository eventMetadataQueryRepository;
    @Mock private EventSearchColumnsCache eventSearchColumnsCache;
    @Mock private EventMetadataCache eventMetadataCache;
    @Mock private EventUserEventIdsCache eventUserEventIdsCache;
    @Mock private EventViewCountCache eventViewCountCache;
    @Mock private EventCountCache eventCountCache;
    @Mock private RedisPipeline redisPipeline;

    @InjectMocks private EventQueryServiceImpl sut;

    @Nested
    @DisplayName("getEventList")
    class GetEventList {

        @Test
        @DisplayName("캐시 HIT 이면 repository 를 호출하지 않고 캐시 데이터로 결과를 만든다")
        void cacheHit() {
            List<Long> ids = List.of(1L, 2L);
            EventMetadataCacheDto cached1 = EventTestFixture.cacheDto(1L, EventType.FORM);
            EventMetadataCacheDto cached2 = EventTestFixture.cacheDto(2L, EventType.FILE);
            EventResponseDto.SearchList expected = EventResponseDto.SearchList.builder().count(2).build();

            given(eventUserEventIdsCache.get(USERNAME)).willReturn(ids);
            given(eventMetadataCache.multiGetMap(ids)).willReturn(Map.of(1L, cached1, 2L, cached2));
            given(eventCountCache.getCounts(ids)).willReturn(Map.of(1L, 3L, 2L, 5L));
            given(eventViewCountCache.getViewCounts(ids)).willReturn(Map.of(1L, 10L, 2L, 20L));
            given(eventResponseConverter.toSearchListDto(any())).willReturn(expected);

            EventResponseDto.SearchList result = sut.getEventList(USERNAME);

            assertThat(result).isSameAs(expected);
            verify(eventQueryRepository, never()).findByUserIdAndIsActive(any(), any());
            verifyNoInteractions(redisPipeline);
        }

        @Test
        @DisplayName("캐시 MISS(eventIds null) 이면 repository 조회 후 파이프라인으로 모든 캐시에 적재한다")
        void cacheMiss() {
            List<EventWithMetadataDto> dbResult = List.of(
                    EventTestFixture.eventWithMetadata(11L, EventType.FORM),
                    EventTestFixture.eventWithMetadata(12L, EventType.FILE)
            );
            EventResponseDto.SearchList expected = EventResponseDto.SearchList.builder().count(2).build();
            RedisBatchContext ctx = org.mockito.Mockito.mock(RedisBatchContext.class);

            given(eventUserEventIdsCache.get(USERNAME)).willReturn(null);
            given(eventQueryRepository.findByUserIdAndIsActive(USERNAME, true)).willReturn(dbResult);
            given(eventResponseConverter.toSearchListDto(dbResult)).willReturn(expected);

            doAnswer(invocation -> {
                Consumer<RedisBatchContext> work = invocation.getArgument(0);
                work.accept(ctx);
                return null;
            }).when(redisPipeline).execute(any());

            EventResponseDto.SearchList result = sut.getEventList(USERNAME);

            assertThat(result).isSameAs(expected);
            verify(eventMetadataCache).putAllInBatch(ctx, dbResult);
            verify(eventCountCache).putAllInBatch(ctx, dbResult);
            verify(eventViewCountCache).putAllInBatch(ctx, dbResult);
            verify(eventUserEventIdsCache).putInBatch(eq(ctx), eq(USERNAME), eq(List.of(11L, 12L)));
        }
    }

    @Nested
    @DisplayName("getEventDetail")
    class GetEventDetail {

        @Test
        @DisplayName("FORM 타입은 toSearchFormDto 결과를 반환한다")
        void form() {
            EventWithMetadataDto data = EventTestFixture.eventWithMetadata(EVENT_ID, EventType.FORM);
            EventResponseDto.SearchFormDetail expected = EventResponseDto.SearchFormDetail.builder().eventTitle("t").build();

            given(eventQueryRepository.findByUserIdAndEventIdAndIsActive(USERNAME, EVENT_ID, true)).willReturn(data);
            given(eventResponseConverter.toSearchFormDto(data)).willReturn(expected);

            assertThat(sut.getEventDetail(USERNAME, EVENT_ID)).isSameAs(expected);
        }

        @Test
        @DisplayName("FILE 타입은 toSearchFileDto 결과를 반환한다")
        void file() {
            EventWithMetadataDto data = EventTestFixture.eventWithMetadata(EVENT_ID, EventType.FILE);
            EventResponseDto.SearchFileDetail expected = EventResponseDto.SearchFileDetail.builder().eventTitle("t").build();

            given(eventQueryRepository.findByUserIdAndEventIdAndIsActive(USERNAME, EVENT_ID, true)).willReturn(data);
            given(eventResponseConverter.toSearchFileDto(data)).willReturn(expected);

            assertThat(sut.getEventDetail(USERNAME, EVENT_ID)).isSameAs(expected);
        }

        @Test
        @DisplayName("타입이 null 등 알 수 없는 값이면 INVALID_INPUT_VALUE 가 발생한다")
        void invalidType() {
            EventWithMetadataDto data = EventTestFixture.eventWithMetadata(EVENT_ID, null);
            given(eventQueryRepository.findByUserIdAndEventIdAndIsActive(USERNAME, EVENT_ID, true)).willReturn(data);

            assertThatThrownBy(() -> sut.getEventDetail(USERNAME, EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }
    }

    @Nested
    @DisplayName("getEventIsActive")
    class GetEventIsActive {

        @Test
        @DisplayName("repository 결과를 그대로 컨버터에 전달한다")
        void delegatesToConverter() {
            EventResponseDto.IsActive expected =
                    EventResponseDto.IsActive.builder().eventId(EVENT_ID).isActive(true).build();
            given(eventQueryRepository.existsById(EVENT_ID)).willReturn(true);
            given(eventResponseConverter.toIsActiveDto(EVENT_ID, true)).willReturn(expected);

            assertThat(sut.getEventIsActive(EVENT_ID)).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("getEventColumns")
    class GetEventColumns {

        @Test
        @DisplayName("캐시 HIT 이면 repository 를 호출하지 않고 캐시 값을 반환한다")
        void cacheHit() {
            EventResponseDto.SearchColumnsAndEventId cached =
                    EventResponseDto.SearchColumnsAndEventId.builder().eventId(EVENT_ID).build();
            given(eventSearchColumnsCache.get(EVENT_ID)).willReturn(cached);

            assertThat(sut.getEventColumns(EVENT_ID)).isSameAs(cached);

            verifyNoInteractions(eventQueryRepository);
            verify(eventSearchColumnsCache, never()).put(anyLong(), any());
        }

        @Test
        @DisplayName("캐시 MISS 이고 이벤트가 존재하면 결과를 캐시에 저장하고 반환한다")
        void cacheMissAndFound() {
            EventWithMetadataDto data = EventTestFixture.eventWithMetadata(EVENT_ID, EventType.FORM);
            EventResponseDto.SearchColumnsAndEventId expected =
                    EventResponseDto.SearchColumnsAndEventId.builder().eventId(EVENT_ID).build();

            given(eventSearchColumnsCache.get(EVENT_ID)).willReturn(null);
            given(eventQueryRepository.findByEventIdAndIsActive(EVENT_ID, true)).willReturn(Optional.of(data));
            given(eventResponseConverter.toSearchColumnsAndEventIdDto(data)).willReturn(expected);

            assertThat(sut.getEventColumns(EVENT_ID)).isSameAs(expected);

            verify(eventSearchColumnsCache).put(EVENT_ID, expected);
        }

        @Test
        @DisplayName("캐시 MISS 이고 이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void cacheMissAndMissing() {
            given(eventSearchColumnsCache.get(EVENT_ID)).willReturn(null);
            given(eventQueryRepository.findByEventIdAndIsActive(EVENT_ID, true)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.getEventColumns(EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("getNextPollingEvents")
    class GetNextPollingEvents {

        @Test
        @DisplayName("repository 결과를 그대로 반환한다")
        void returnsAsIs() {
            List<EventWithMetadataDto> dbResult = List.of(EventTestFixture.eventWithMetadata(1L, EventType.FORM));
            LocalDateTime threshold = LocalDateTime.now();
            given(eventQueryRepository.findByNextPollingAtBefore(threshold)).willReturn(dbResult);

            assertThat(sut.getNextPollingEvents(threshold)).isSameAs(dbResult);
        }
    }

    @Nested
    @DisplayName("getEventColumnIds")
    class GetEventColumnIds {

        @Test
        @DisplayName("메타데이터의 searchColumnsIds 를 반환한다")
        void returnsIds() {
            EventMetadataEntity metadata = EventTestFixture.metadata();
            given(eventMetadataQueryRepository.findByEventId(EVENT_ID)).willReturn(metadata);

            assertThat(sut.getEventColumnIds(EVENT_ID)).isEqualTo(metadata.getSearchColumnsIds());
        }
    }

    @Nested
    @DisplayName("getEventType")
    class GetEventType {

        @Test
        @DisplayName("이벤트가 존재하면 EventType 을 반환한다")
        void success() {
            EventEntity event = EventTestFixture.formEvent();
            given(eventQueryRepository.findById(EVENT_ID)).willReturn(Optional.of(event));

            assertThat(sut.getEventType(EVENT_ID)).isEqualTo(EventType.FORM);
        }

        @Test
        @DisplayName("이벤트가 없으면 EVENT_NOT_FOUND 가 발생한다")
        void throwsWhenMissing() {
            given(eventQueryRepository.findById(EVENT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.getEventType(EVENT_ID))
                    .isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.EVENT_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("getInactiveEventIds")
    class GetInactiveEventIds {

        @Test
        @DisplayName("현재 시간을 repository 에 전달하고 결과를 반환한다")
        void delegates() {
            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            List<Long> dbResult = List.of(1L, 2L);
            given(eventQueryRepository.findInactiveEventIds(captor.capture())).willReturn(dbResult);

            LocalDateTime before = LocalDateTime.now();
            List<Long> result = sut.getInactiveEventIds();
            LocalDateTime after = LocalDateTime.now();

            assertThat(result).isSameAs(dbResult);
            assertThat(captor.getValue()).isBetween(before, after);
        }
    }
}

package com.example.backend.application.facade.helper;

import com.example.backend.domain.event.dto.EventResponseDto;
import com.example.backend.domain.response.dto.ResponseDto;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ResponseHelper {

    public static boolean extracted(ResponseDto queryDto, EventResponseDto.SearchColumnsAndEventId eventColumns) {
        List<String> requiredColumns = eventColumns.getSearchColumns();
        Set<String> required = new HashSet<>(requiredColumns);

        Map<String, String> provided = queryDto.getAnswers();
        Set<String> providedKeys = provided.keySet();

        return required.equals(providedKeys);
    }
}

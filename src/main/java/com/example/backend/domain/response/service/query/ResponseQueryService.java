package com.example.backend.domain.response.service.query;

import com.example.backend.domain.response.dto.ResponseDto;

import java.util.List;
import java.util.Map;

public interface ResponseQueryService {

    public ResponseDto getResponse(Long eventId, Map<String,String> answers);
    public List<ResponseDto> getResponses(Long eventId);

}

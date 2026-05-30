package com.example.backend.domain.response.service.command;

import com.example.backend.domain.event.dto.EventRequestDto;
import com.example.backend.domain.response.dto.ResponseDto;
import com.example.backend.infra.google.dto.GoogleFormResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResponseCommandService {
    void saveFormResponses(List<GoogleFormResponseDto> responses, Long eventId);
    List<ResponseDto> saveFileResponses(MultipartFile file, List<String> searchColumns, Long eventId);
}

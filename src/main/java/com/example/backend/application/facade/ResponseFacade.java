package com.example.backend.application.facade;


import com.example.backend.domain.response.dto.ResponseDto;

public interface ResponseFacade {
    ResponseDto getResponse(ResponseDto responseDto);
}

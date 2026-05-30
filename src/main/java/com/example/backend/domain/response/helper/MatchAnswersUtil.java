package com.example.backend.domain.response.helper;

import com.example.backend.domain.response.dto.ResponseDto;

import java.util.List;
import java.util.Map;

public class MatchAnswersUtil {

    public static ResponseDto findMatchedResponse(
            List<ResponseDto> responses,
            Map<String, String> answersWithSearchCoulumnIds
    ) {
        return responses.stream()
                .filter(dto -> matchAnswers(dto.getAnswers(), answersWithSearchCoulumnIds))
                .findFirst()
                .orElse(null);
    }

    public static boolean matchAnswers(
            Map<String, String> cachedAnswers,
            Map<String, String> queryAnswers
    ) {
        if (cachedAnswers == null) return false;

        for (Map.Entry<String, String> entry : queryAnswers.entrySet()) {
            String key = entry.getKey();
            String expected = entry.getValue();

            if (!expected.equals(cachedAnswers.get(key))) {
                return false;
            }
        }
        return true;
    }


}

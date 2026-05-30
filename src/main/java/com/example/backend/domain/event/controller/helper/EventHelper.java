package com.example.backend.domain.event.controller.helper;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class EventHelper {


    public static List<Map<String, String>> fileParser(MultipartFile file, List<String> searchColumns) {

        char separator = isTsv(file) ? '\t' : ',';

        List<Map<String, String>> resultList = new ArrayList<>();

        try (InputStreamReader isr = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVReader reader = new CSVReaderBuilder(isr)
                     .withCSVParser(new CSVParserBuilder().withSeparator(separator).build())
                     .build()) {

            // 2. 헤더 읽기
            String[] headers = reader.readNext();
            if (headers == null) {
                return resultList; // 빈 리스트 반환
            }

            // BOM 제거
            for (int i = 0; i < headers.length; i++) {
                headers[i] = cleanHeader(headers[i]);
            }

            String[] line;
            // 3. 전체 데이터 읽기
            while ((line = reader.readNext()) != null) {
                // 데이터 불일치 검증
                if (line.length != headers.length) continue;

                Map<String, String> rowMap = new LinkedHashMap<>();

                // header -> index 매핑 (성능 최적화)
                Map<String, Integer> headerIndexMap = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    headerIndexMap.put(headers[i], i);
                }

                // searchColumns 순서대로 넣기
                for (String targetColumn : searchColumns) {
                    Integer idx = headerIndexMap.get(targetColumn);
                    if (idx != null) {
                        rowMap.put(targetColumn, cleanValue(line[idx]));
                    }
                }

                resultList.add(rowMap);
            }

        } catch (Exception e) {
            throw new RuntimeException("CSV/TSV 파싱 중 오류 발생", e);
        }

        log.info("👉파싱 {}", resultList.get(0).toString());

        return resultList;


    }

    private static boolean isTsv(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".tsv");
    }

    private static String cleanHeader(String header) {
        if (header == null) return null;
        return header.replace("\uFEFF", "").trim();
    }

    private static String cleanValue(String value) {
        return value == null ? null : value.trim();
    }


}

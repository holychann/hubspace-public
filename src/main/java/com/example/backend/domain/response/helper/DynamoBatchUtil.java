package com.example.backend.domain.response.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamoBatchUtil {

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;
    private static final int BATCH_SIZE = 25;

    /**
     * DynamoDB에 리스트를 배치로 저장합니다. (25개씩 분할 처리)
     *
     * @param items 저장할 아이템 리스트 (이미 변환된 객체들)
     * @param clazz 아이템의 클래스 타입
     * @param table 매핑된 DynamoDbTable 리소스
     * @param <T>   엔티티 타입
     */
    public <T> void batchWrite(List<T> items, Class<T> clazz, DynamoDbTable<T> table) {
        if (items == null || items.isEmpty()) {
            return;
        }

        log.info("📦[DYNAMO_BATCH] 배치 저장 시작 | 총 개수: {}", items.size());

        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, items.size());
            List<T> batch = items.subList(i, end);

            WriteBatch.Builder<T> writeBatchBuilder = WriteBatch.builder(clazz)
                    .mappedTableResource(table);

            batch.forEach(writeBatchBuilder::addPutItem);

            WriteBatch writeBatch = writeBatchBuilder.build();

            dynamoDbEnhancedClient.batchWriteItem(b -> b.writeBatches(writeBatch));
        }

        log.info("✅[DYNAMO_BATCH] 배치 저장 완료");
    }
}

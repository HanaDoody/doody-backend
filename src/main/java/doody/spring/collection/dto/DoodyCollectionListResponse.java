package doody.spring.collection.dto;

import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyTemplate;
import java.time.LocalDateTime;

public record DoodyCollectionListResponse(
    Long doodyCollectionId,
    String doodyId,
    String name,
    String tier,
    String axis,
    String imageUrl,
    String earnedReason,
    String source,
    Long sourceId,
    LocalDateTime collectedAt
) {

    public static DoodyCollectionListResponse from(DoodyCollection collection) {
        DoodyTemplate template = collection.getDoodyTemplate();
        return new DoodyCollectionListResponse(
            collection.getId(),
            template.getId(),
            template.getName(),
            collection.getTier(),
            collection.getAxis(),
            template.getImageUrl(),
            collection.getEarnedReason(),
            collection.getSource(),
            collection.getSourceId(),
            collection.getCollectedAt()
        );
    }
}
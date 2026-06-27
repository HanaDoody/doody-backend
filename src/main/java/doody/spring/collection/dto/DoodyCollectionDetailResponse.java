package doody.spring.collection.dto;

import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyCollectionDetail;
import doody.spring.domain.entity.DoodyTemplate;
import java.time.LocalDateTime;

public record DoodyCollectionDetailResponse(
    Long doodyCollectionId,
    String doodyId,
    String name,
    String tier,
    String axis,
    String description,
    String imageUrl,
    String unlockCondition,
    String earnedReason,
    String source,
    Long sourceId,
    LocalDateTime collectedAt,
    Detail detail
) {

    public static DoodyCollectionDetailResponse from(DoodyCollection collection, DoodyCollectionDetail detail) {
        DoodyTemplate template = collection.getDoodyTemplate();
        return new DoodyCollectionDetailResponse(
            collection.getId(),
            template.getId(),
            template.getName(),
            collection.getTier(),
            collection.getAxis(),
            template.getDescription(),
            template.getImageUrl(),
            template.getUnlockCondition(),
            collection.getEarnedReason(),
            collection.getSource(),
            collection.getSourceId(),
            collection.getCollectedAt(),
            Detail.from(detail)
        );
    }

    public record Detail(
        Long detailId,
        String detailTitle,
        String detailDescription,
        String personality,
        String detailImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {

        public static Detail from(DoodyCollectionDetail detail) {
            if (detail == null) {
                return null;
            }

            return new Detail(
                detail.getId(),
                detail.getDetailTitle(),
                detail.getDetailDescription(),
                detail.getPersonality(),
                detail.getDetailImageUrl(),
                detail.getCreatedAt(),
                detail.getUpdatedAt()
            );
        }
    }
}
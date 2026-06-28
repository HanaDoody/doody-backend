package doody.spring.collection.dto;

import java.math.BigDecimal;

public record CollectionPinResponse(
    String pinId,
    String title,
    BigDecimal lat,
    BigDecimal lng,
    String doodyId,
    String doodyName,
    String imageUrl,
    Double distanceMeter,
    Boolean captureAvailable
) {
}
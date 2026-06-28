package doody.spring.collection.dto;

import java.math.BigDecimal;

public record CollectionCaptureRequest(
    String userId,
    String pinId,
    BigDecimal lat,
    BigDecimal lng
) {
}
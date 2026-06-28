package doody.spring.collection.controller;

import doody.spring.collection.dto.CollectionCaptureRequest;
import doody.spring.collection.dto.CollectionCaptureResponse;
import doody.spring.collection.dto.CollectionPinResponse;
import doody.spring.collection.service.CollectionCaptureService;
import doody.spring.common.dto.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collection")
public class CollectionCaptureController {

    private final CollectionCaptureService collectionCaptureService;

    public CollectionCaptureController(CollectionCaptureService collectionCaptureService) {
        this.collectionCaptureService = collectionCaptureService;
    }

    @GetMapping("/pins")
    public ApiResponse<List<CollectionPinResponse>> getNearbyPins(
        @RequestParam BigDecimal lat,
        @RequestParam BigDecimal lng,
        @RequestParam(required = false) Double radiusMeter
    ) {
        return ApiResponse.success(HttpStatus.OK, "collection pins lookup success.", collectionCaptureService.getNearbyPins(lat, lng, radiusMeter));
    }

    @PostMapping("/capture")
    public ApiResponse<CollectionCaptureResponse> capture(@RequestBody CollectionCaptureRequest request) {
        return ApiResponse.success(HttpStatus.OK, "collection capture success.", collectionCaptureService.capture(request));
    }
}
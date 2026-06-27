package doody.spring.collection.controller;

import doody.spring.collection.dto.DoodyCollectionDetailResponse;
import doody.spring.collection.dto.DoodyCollectionListResponse;
import doody.spring.collection.service.DoodyCollectionService;
import doody.spring.common.dto.ApiResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/doody-collections")
public class DoodyCollectionController {

    private final DoodyCollectionService doodyCollectionService;

    public DoodyCollectionController(DoodyCollectionService doodyCollectionService) {
        this.doodyCollectionService = doodyCollectionService;
    }

    @GetMapping
    public ApiResponse<List<DoodyCollectionListResponse>> getCollections(@PathVariable String userId) {
        return ApiResponse.success(HttpStatus.OK, "doody collection lookup success.", doodyCollectionService.getCollections(userId));
    }

    @GetMapping("/{collectionId}")
    public ApiResponse<DoodyCollectionDetailResponse> getCollectionDetail(
        @PathVariable String userId,
        @PathVariable Long collectionId
    ) {
        return ApiResponse.success(HttpStatus.OK, "doody collection detail lookup success.", doodyCollectionService.getCollectionDetail(userId, collectionId));
    }
}
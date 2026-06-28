package doody.spring.autonomy.controller;

import doody.spring.autonomy.dto.AutonomyMissionCompleteResponse;
import doody.spring.autonomy.dto.AutonomyMissionDetailResponse;
import doody.spring.autonomy.dto.AutonomyMissionEvidenceResponse;
import doody.spring.autonomy.dto.AutonomyMissionRejectRequest;
import doody.spring.autonomy.dto.AutonomyMissionRejectResponse;
import doody.spring.autonomy.dto.AutonomyMissionStartResponse;
import doody.spring.autonomy.dto.AutonomyPathResponse;
import doody.spring.autonomy.service.AutonomyMissionService;
import doody.spring.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users/{userId}/autonomy")
public class AutonomyMissionController {

    private final AutonomyMissionService autonomyMissionService;

    public AutonomyMissionController(AutonomyMissionService autonomyMissionService) {
        this.autonomyMissionService = autonomyMissionService;
    }

    @GetMapping("/path")
    public ApiResponse<AutonomyPathResponse> getPath(@PathVariable String userId) {
        return ApiResponse.success(HttpStatus.OK, "autonomy path lookup success.", autonomyMissionService.getPath(userId));
    }

    @GetMapping("/missions/{missionId}")
    public ApiResponse<AutonomyMissionDetailResponse> getMission(
        @PathVariable String userId,
        @PathVariable String missionId
    ) {
        return ApiResponse.success(HttpStatus.OK, "autonomy mission lookup success.", autonomyMissionService.getMission(userId, missionId));
    }

    @PostMapping("/missions/{missionId}/start")
    public ApiResponse<AutonomyMissionStartResponse> start(
        @PathVariable String userId,
        @PathVariable String missionId
    ) {
        return ApiResponse.success(HttpStatus.OK, "autonomy mission start success.", autonomyMissionService.start(userId, missionId));
    }

    @PostMapping("/missions/{missionId}/reject")
    public ApiResponse<AutonomyMissionRejectResponse> reject(
        @PathVariable String userId,
        @PathVariable String missionId,
        @RequestBody(required = false) AutonomyMissionRejectRequest request
    ) {
        return ApiResponse.success(HttpStatus.OK, "autonomy mission reject success.", autonomyMissionService.reject(userId, missionId, request));
    }

    @PostMapping(value = "/missions/{missionId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AutonomyMissionEvidenceResponse> addEvidence(
        @PathVariable String userId,
        @PathVariable String missionId,
        @RequestParam(required = false) String fileUrl,
        @RequestParam(required = false) String contentType,
        @RequestParam(required = false) MultipartFile file
    ) {
        String resolvedUrl = fileUrl;
        String resolvedContentType = contentType;
        if ((resolvedUrl == null || resolvedUrl.isBlank()) && file != null && !file.isEmpty()) {
            resolvedUrl = "upload://" + file.getOriginalFilename();
            resolvedContentType = file.getContentType();
        }
        return ApiResponse.success(
            HttpStatus.OK,
            "autonomy mission evidence upload success.",
            autonomyMissionService.addEvidence(userId, missionId, resolvedUrl, resolvedContentType)
        );
    }

    @PostMapping("/missions/{missionId}/complete")
    public ApiResponse<AutonomyMissionCompleteResponse> complete(
        @PathVariable String userId,
        @PathVariable String missionId
    ) {
        return ApiResponse.success(HttpStatus.OK, "autonomy mission complete success.", autonomyMissionService.complete(userId, missionId));
    }
}

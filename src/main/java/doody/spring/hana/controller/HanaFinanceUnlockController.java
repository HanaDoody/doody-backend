package doody.spring.hana.controller;

import doody.spring.common.dto.ApiResponse;
import doody.spring.hana.dto.HanaFinanceUnlockRequest;
import doody.spring.hana.dto.HanaFinanceUnlockResponse;
import doody.spring.hana.dto.HanaFinanceUnlockStatusResponse;
import doody.spring.hana.service.HanaFinanceUnlockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/hana-finance")
public class HanaFinanceUnlockController {

    private final HanaFinanceUnlockService hanaFinanceUnlockService;

    public HanaFinanceUnlockController(HanaFinanceUnlockService hanaFinanceUnlockService) {
        this.hanaFinanceUnlockService = hanaFinanceUnlockService;
    }

    @GetMapping("/unlock-status")
    public ApiResponse<HanaFinanceUnlockStatusResponse> getStatus(
        @PathVariable String userId,
        @RequestParam String axis
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "hana finance unlock status lookup success.",
            hanaFinanceUnlockService.getStatus(userId, axis)
        );
    }

    @PostMapping("/unlocks")
    public ApiResponse<HanaFinanceUnlockResponse> unlock(
        @PathVariable String userId,
        @RequestBody HanaFinanceUnlockRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "hana finance unlock success.",
            hanaFinanceUnlockService.unlock(userId, request == null ? null : request.axis())
        );
    }
}
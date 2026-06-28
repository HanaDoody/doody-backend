package doody.spring.mission.controller;

import doody.spring.common.dto.ApiResponse;
import doody.spring.mission.dto.TodayMissionResponse;
import doody.spring.mission.service.MissionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/missions")
public class MissionController {

    private final MissionService missionService;

    public MissionController(MissionService missionService) {
        this.missionService = missionService;
    }

    @GetMapping("/today")
    public ApiResponse<TodayMissionResponse> getTodayMission(@PathVariable String userId) {
        return ApiResponse.success(HttpStatus.OK, "today mission lookup success.", missionService.getTodayMission(userId));
    }
}
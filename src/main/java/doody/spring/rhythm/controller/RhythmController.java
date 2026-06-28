package doody.spring.rhythm.controller;

import doody.spring.common.dto.ApiResponse;
import doody.spring.rhythm.dto.EveningRhythmRequest;
import doody.spring.rhythm.dto.EveningRhythmResponse;
import doody.spring.rhythm.dto.MorningRhythmRequest;
import doody.spring.rhythm.dto.MorningRhythmResponse;
import doody.spring.rhythm.service.RhythmService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rhythm")
public class RhythmController {

    private final RhythmService rhythmService;

    public RhythmController(RhythmService rhythmService) {
        this.rhythmService = rhythmService;
    }

    @PostMapping("/morning")
    public ApiResponse<MorningRhythmResponse> checkInMorning(@RequestBody MorningRhythmRequest request) {
        return ApiResponse.success(HttpStatus.OK, "morning rhythm check-in success.", rhythmService.checkInMorning(request));
    }

    @PostMapping("/evening")
    public ApiResponse<EveningRhythmResponse> leaveEveningNote(@RequestBody EveningRhythmRequest request) {
        return ApiResponse.success(HttpStatus.OK, "evening rhythm note success.", rhythmService.leaveEveningNote(request));
    }
}
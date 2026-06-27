package doody.spring.report.service;

import doody.spring.domain.repository.ReportSummaryRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.report.dto.ReportSummaryResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {

    private final UserRepository userRepository;
    private final ReportSummaryRepository reportSummaryRepository;

    public ReportService(UserRepository userRepository, ReportSummaryRepository reportSummaryRepository) {
        this.userRepository = userRepository;
        this.reportSummaryRepository = reportSummaryRepository;
    }

    @Transactional(readOnly = true)
    public List<ReportSummaryResponse> getReports(String userId) {
        validateUserId(userId);
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found.");
        }

        return reportSummaryRepository.findByUser_IdOrderByGeneratedAtDesc(userId).stream()
            .map(ReportSummaryResponse::from)
            .toList();
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
    }
}
package doody.spring.report.service;

import doody.spring.domain.entity.MissionLog;
import doody.spring.domain.entity.ReportSummary;
import doody.spring.domain.entity.RhythmLog;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.PointTransactionRepository;
import doody.spring.domain.repository.ReportSummaryRepository;
import doody.spring.domain.repository.RhythmLogRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.report.client.AiReportSummaryClient;
import doody.spring.report.client.AiReportSummaryClient.RecentMission;
import doody.spring.report.client.AiReportSummaryClient.ReportSummaryStats;
import doody.spring.report.dto.RecoveryReportResponse;
import doody.spring.report.dto.RecoveryReportResponse.ActivitySummary;
import doody.spring.report.dto.RecoveryReportResponse.AxisRecordGroup;
import doody.spring.report.dto.RecoveryReportResponse.AxisSummary;
import doody.spring.report.dto.RecoveryReportResponse.RecordItem;
import doody.spring.report.dto.RecoveryReportResponse.Summary;
import doody.spring.report.dto.ReportSummaryResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReportService {

    private static final List<String> AXES = List.of("RHYTHM", "AUTONOMY", "CONNECTION");

    private final UserRepository userRepository;
    private final ReportSummaryRepository reportSummaryRepository;
    private final RhythmLogRepository rhythmLogRepository;
    private final MissionLogRepository missionLogRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final AiReportSummaryClient aiReportSummaryClient;

    public ReportService(
        UserRepository userRepository,
        ReportSummaryRepository reportSummaryRepository,
        RhythmLogRepository rhythmLogRepository,
        MissionLogRepository missionLogRepository,
        PointTransactionRepository pointTransactionRepository,
        AiReportSummaryClient aiReportSummaryClient
    ) {
        this.userRepository = userRepository;
        this.reportSummaryRepository = reportSummaryRepository;
        this.rhythmLogRepository = rhythmLogRepository;
        this.missionLogRepository = missionLogRepository;
        this.pointTransactionRepository = pointTransactionRepository;
        this.aiReportSummaryClient = aiReportSummaryClient;
    }

    @Transactional(readOnly = true)
    public List<ReportSummaryResponse> getReports(String userId) {
        validateUserId(userId);
        validateUserExists(userId);

        return reportSummaryRepository.findByUser_IdOrderByGeneratedAtDesc(userId).stream()
            .map(ReportSummaryResponse::from)
            .toList();
    }

    @Transactional
    public RecoveryReportResponse getRecoveryReport(String userId, String period) {
        validateUserId(userId);
        User user = getUser(userId);

        ReportPeriod reportPeriod = ReportPeriod.from(period);
        LocalDateTime startAt = reportPeriod.startDate().atStartOfDay();
        LocalDateTime endAt = reportPeriod.endDate().atTime(LocalTime.MAX);

        List<RecordWithAxis> records = new ArrayList<>();
        Set<LocalDate> recordedDays = new HashSet<>();

        List<RhythmLog> rhythmLogs = rhythmLogRepository.findByUser_IdAndTimestampBetweenOrderByTimestampDesc(userId, startAt, endAt);
        for (RhythmLog rhythmLog : rhythmLogs) {
            LocalDate recordDate = rhythmLog.getTimestamp().toLocalDate();
            recordedDays.add(recordDate);
            records.add(new RecordWithAxis(
                "RHYTHM",
                new RecordItem(
                    "RHYTHM_LOG",
                    rhythmLog.getId(),
                    resolveRhythmTitle(rhythmLog),
                    rhythmLog.getReward(),
                    recordDate,
                    rhythmLog.getTimestamp()
                )
            ));
        }

        List<MissionLog> missionLogs = missionLogRepository.findByUser_IdAndCompletedAtBetweenOrderByCompletedAtDesc(userId, startAt, endAt);
        for (MissionLog missionLog : missionLogs) {
            LocalDate recordDate = missionLog.getCompletedAt().toLocalDate();
            recordedDays.add(recordDate);
            String axis = normalizeAxis(missionLog.getMissionTemplate().getAxis());
            records.add(new RecordWithAxis(
                axis,
                new RecordItem(
                    "MISSION_LOG",
                    missionLog.getId(),
                    missionLog.getMissionTemplate().getTitle(),
                    missionLog.getMissionTemplate().getReward(),
                    recordDate,
                    missionLog.getCompletedAt()
                )
            ));
        }

        records.sort(Comparator.comparing((RecordWithAxis record) -> record.item().recordedAt()).reversed());

        Integer totalPoint = pointTransactionRepository.sumAmountByUserIdAndCreatedAtBetween(userId, startAt, endAt);
        Summary summary = new Summary(records.size(), recordedDays.size(), totalPoint);

        List<AxisSummary> axisSummaries = AXES.stream()
            .map(axis -> new AxisSummary(axis, countByAxis(records, axis)))
            .toList();

        List<AxisRecordGroup> recordGroups = AXES.stream()
            .map(axis -> {
                List<RecordItem> axisRecords = records.stream()
                    .filter(record -> axis.equals(record.axis()))
                    .map(RecordWithAxis::item)
                    .toList();
                return new AxisRecordGroup(axis, axisRecords.size(), axisRecords);
            })
            .toList();

        ActivitySummary activitySummary = "month".equals(reportPeriod.value())
            ? getOrCreateMonthlyActivitySummary(user, reportPeriod, buildStats(summary, axisSummaries, missionLogs))
            : null;

        return new RecoveryReportResponse(
            reportPeriod.value(),
            reportPeriod.startDate(),
            reportPeriod.endDate(),
            activitySummary,
            summary,
            axisSummaries,
            recordGroups
        );
    }

    private ActivitySummary getOrCreateMonthlyActivitySummary(
        User user,
        ReportPeriod reportPeriod,
        ReportSummaryStats stats
    ) {
        return reportSummaryRepository
            .findTopByUser_IdAndPeriodAndPeriodStartAndPeriodEndOrderByGeneratedAtDesc(
                user.getId(),
                "1m",
                reportPeriod.startDate(),
                reportPeriod.endDate()
            )
            .map(this::toActivitySummary)
            .orElseGet(() -> createAndSaveMonthlyActivitySummary(user, reportPeriod, stats));
    }

    private ActivitySummary createAndSaveMonthlyActivitySummary(
        User user,
        ReportPeriod reportPeriod,
        ReportSummaryStats stats
    ) {
        ActivitySummary activitySummary = aiReportSummaryClient.summarize(user.getId(), "1m", stats);

        reportSummaryRepository.save(ReportSummary.create(
            user,
            "1m",
            reportPeriod.startDate(),
            reportPeriod.endDate(),
            toStatsJson(stats),
            activitySummary.summary(),
            toJsonArray(activitySummary.highlights()),
            activitySummary.generatedAt()
        ));

        return activitySummary;
    }

    private ActivitySummary toActivitySummary(ReportSummary reportSummary) {
        return new ActivitySummary(
            reportSummary.getSummary(),
            readJsonArray(reportSummary.getHighlights()),
            reportSummary.getGeneratedAt(),
            "DB"
        );
    }

    private String toStatsJson(ReportSummaryStats stats) {
        return "{"
            + "\"total_records\":" + stats.totalRecords()
            + ",\"active_days\":" + stats.activeDays()
            + ",\"points\":" + stats.points()
            + ",\"axis_counts\":{"
            + "\"rhythm\":" + stats.axisCounts().getOrDefault("rhythm", 0)
            + ",\"autonomy\":" + stats.axisCounts().getOrDefault("autonomy", 0)
            + ",\"connection\":" + stats.axisCounts().getOrDefault("connection", 0)
            + "}"
            + ",\"recent_missions\":" + toRecentMissionsJson(stats.recentMissions())
            + "}";
    }

    private String toRecentMissionsJson(List<RecentMission> recentMissions) {
        return recentMissions.stream()
            .map(mission -> "{"
                + "\"mission_id\":\"" + escapeJson(mission.missionId()) + "\""
                + ",\"axis\":\"" + escapeJson(mission.axis()) + "\""
                + ",\"title\":\"" + escapeJson(mission.title()) + "\""
                + ",\"completed_at\":\"" + mission.completedAt() + "\""
                + "}")
            .reduce("[", (acc, item) -> "[".equals(acc) ? acc + item : acc + "," + item)
            + "]";
    }

    private String toJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }

        return values.stream()
            .map(value -> "\"" + escapeJson(value) + "\"")
            .reduce("[", (acc, item) -> "[".equals(acc) ? acc + item : acc + "," + item)
            + "]";
    }

    private List<String> readJsonArray(String jsonArray) {
        if (jsonArray == null || jsonArray.length() < 2) {
            return List.of();
        }

        String trimmed = jsonArray.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return List.of();
        }

        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (ch == ',' && !inString) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString());
        return values;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n");
    }

    private ReportSummaryStats buildStats(
        Summary summary,
        List<AxisSummary> axisSummaries,
        List<MissionLog> missionLogs
    ) {
        Map<String, Integer> axisCounts = new HashMap<>();
        for (AxisSummary axisSummary : axisSummaries) {
            axisCounts.put(axisSummary.axis().toLowerCase(Locale.ROOT), axisSummary.recordCount());
        }

        List<RecentMission> recentMissions = missionLogs.stream()
            .limit(10)
            .map(missionLog -> new RecentMission(
                missionLog.getMissionTemplate().getId(),
                missionLog.getMissionTemplate().getAxis(),
                missionLog.getMissionTemplate().getTitle(),
                missionLog.getCompletedAt()
            ))
            .toList();

        return new ReportSummaryStats(
            summary.totalRecordCount(),
            summary.recordedDayCount(),
            summary.totalPoint(),
            axisCounts,
            recentMissions
        );
    }

    private int countByAxis(List<RecordWithAxis> records, String axis) {
        return (int) records.stream()
            .filter(record -> axis.equals(record.axis()))
            .count();
    }

    private String resolveRhythmTitle(RhythmLog rhythmLog) {
        if (rhythmLog.getText() != null && !rhythmLog.getText().isBlank()) {
            return rhythmLog.getText();
        }
        return rhythmLog.getRhythmType();
    }

    private String normalizeAxis(String axis) {
        if (axis == null || axis.isBlank()) {
            return "RHYTHM";
        }
        return axis.toUpperCase(Locale.ROOT);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
    }

    private void validateUserExists(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found.");
        }
    }

    private User getUser(String userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
    }

    private record RecordWithAxis(
        String axis,
        RecordItem item
    ) {
    }

    private record ReportPeriod(
        String value,
        LocalDate startDate,
        LocalDate endDate
    ) {

        private static ReportPeriod from(String value) {
            LocalDate today = LocalDate.now();
            String normalized = value == null || value.isBlank() ? "month" : value.toLowerCase(Locale.ROOT);

            if ("week".equals(normalized)) {
                LocalDate startDate = today.minusDays(6);
                return new ReportPeriod("week", startDate, today);
            }

            if ("month".equals(normalized)) {
                YearMonth yearMonth = YearMonth.from(today);
                return new ReportPeriod("month", yearMonth.atDay(1), today);
            }

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "period must be month or week.");
        }
    }
}
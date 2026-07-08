package doody.spring.report.service;

import doody.spring.domain.entity.CollectionCapture;
import doody.spring.domain.entity.EnergyLog;
import doody.spring.domain.entity.MissionLog;
import doody.spring.domain.entity.ReportSummary;
import doody.spring.domain.entity.RhythmLog;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.CollectionCaptureRepository;
import doody.spring.domain.repository.EnergyLogRepository;
import doody.spring.domain.repository.MissionLogRepository;
import doody.spring.domain.repository.PointTransactionRepository;
import doody.spring.domain.repository.ReportSummaryRepository;
import doody.spring.domain.repository.RhythmLogRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.common.util.RhythmTextNormalizer;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
    private final EnergyLogRepository energyLogRepository;
    private final MissionLogRepository missionLogRepository;
    private final CollectionCaptureRepository collectionCaptureRepository;
    private final PointTransactionRepository pointTransactionRepository;
    private final AiReportSummaryClient aiReportSummaryClient;

    public ReportService(
        UserRepository userRepository,
        ReportSummaryRepository reportSummaryRepository,
        RhythmLogRepository rhythmLogRepository,
        EnergyLogRepository energyLogRepository,
        MissionLogRepository missionLogRepository,
        CollectionCaptureRepository collectionCaptureRepository,
        PointTransactionRepository pointTransactionRepository,
        AiReportSummaryClient aiReportSummaryClient
    ) {
        this.userRepository = userRepository;
        this.reportSummaryRepository = reportSummaryRepository;
        this.rhythmLogRepository = rhythmLogRepository;
        this.energyLogRepository = energyLogRepository;
        this.missionLogRepository = missionLogRepository;
        this.collectionCaptureRepository = collectionCaptureRepository;
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
        Map<Long, Short> energyByRhythmLogId = findEnergyByRhythmLogId(rhythmLogs);
        for (RhythmLog rhythmLog : rhythmLogs) {
            LocalDate recordDate = rhythmLog.getTimestamp().toLocalDate();
            recordedDays.add(recordDate);
            records.add(new RecordWithAxis(
                "RHYTHM",
                new RecordItem(
                    "RHYTHM_LOG",
                    rhythmLog.getId(),
                    resolveRhythmTitle(rhythmLog, energyByRhythmLogId.get(rhythmLog.getId())),
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

        List<CollectionCapture> collectionCaptures =
            collectionCaptureRepository.findByUser_IdAndCapturedAtBetweenOrderByCapturedAtDesc(userId, startAt, endAt);
        for (CollectionCapture capture : collectionCaptures) {
            LocalDate recordDate = capture.getCapturedAt().toLocalDate();
            recordedDays.add(recordDate);
            records.add(new RecordWithAxis(
                "CONNECTION",
                new RecordItem(
                    "COLLECTION_CAPTURE",
                    capture.getId(),
                    resolveCollectionCaptureTitle(capture),
                    capture.getReward(),
                    recordDate,
                    capture.getCapturedAt()
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
            ? generateMonthlyActivitySummary(user, reportPeriod, buildStats(summary, axisSummaries, missionLogs))
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

    private ActivitySummary generateMonthlyActivitySummary(
        User user,
        ReportPeriod reportPeriod,
        ReportSummaryStats stats
    ) {
        ActivitySummary activitySummary = aiReportSummaryClient.summarize(user.getId(), "1m", stats);

        if ("AI".equals(activitySummary.source())) {
            saveOrUpdateMonthlyActivitySummary(user, reportPeriod, stats, activitySummary);
        }

        return activitySummary;
    }

    private void saveOrUpdateMonthlyActivitySummary(
        User user,
        ReportPeriod reportPeriod,
        ReportSummaryStats stats,
        ActivitySummary activitySummary
    ) {
        String statsJson = toStatsJson(stats);
        String highlightsJson = toJsonArray(activitySummary.highlights());

        reportSummaryRepository
            .findTopByUser_IdAndPeriodAndPeriodStartAndPeriodEndOrderByGeneratedAtDesc(
                user.getId(),
                "1m",
                reportPeriod.startDate(),
                reportPeriod.endDate()
            )
            .ifPresentOrElse(
                reportSummary -> reportSummary.update(
                    statsJson,
                    activitySummary.summary(),
                    highlightsJson,
                    activitySummary.generatedAt()
                ),
                () -> reportSummaryRepository.save(ReportSummary.create(
                    user,
                    "1m",
                    reportPeriod.startDate(),
                    reportPeriod.endDate(),
                    statsJson,
                    activitySummary.summary(),
                    highlightsJson,
                    activitySummary.generatedAt()
                ))
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

    private Map<Long, Short> findEnergyByRhythmLogId(List<RhythmLog> rhythmLogs) {
        List<Long> morningRhythmLogIds = rhythmLogs.stream()
            .filter(rhythmLog -> "MORNING".equals(rhythmLog.getRhythmType()))
            .map(RhythmLog::getId)
            .filter(Objects::nonNull)
            .toList();
        if (morningRhythmLogIds.isEmpty()) {
            return Map.of();
        }

        return energyLogRepository.findByRhythmLog_IdIn(morningRhythmLogIds).stream()
            .filter(energyLog -> energyLog.getRhythmLog() != null && energyLog.getRhythmLog().getId() != null)
            .collect(Collectors.toMap(
                energyLog -> energyLog.getRhythmLog().getId(),
                EnergyLog::getEnergy,
                (first, second) -> first
            ));
    }

    private String resolveRhythmTitle(RhythmLog rhythmLog, Short energy) {
        return RhythmTextNormalizer.reportRhythmTitle(rhythmLog.getRhythmType(), rhythmLog.getText(), energy);
    }

    private String resolveCollectionCaptureTitle(CollectionCapture capture) {
        if (capture.getDoodyTemplate() != null && capture.getDoodyTemplate().getName() != null
            && !capture.getDoodyTemplate().getName().isBlank()) {
            return capture.getDoodyTemplate().getName();
        }
        return "두디 포획";
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

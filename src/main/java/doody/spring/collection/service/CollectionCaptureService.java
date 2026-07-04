package doody.spring.collection.service;

import doody.spring.collection.client.AiCollectionCaptureClient;
import doody.spring.collection.client.AiCollectionCaptureClient.AiCaptureRequest;
import doody.spring.collection.client.AiCollectionCaptureClient.AiCaptureResult;
import doody.spring.collection.client.AiCollectionCaptureClient.Location;
import doody.spring.collection.dto.CollectionCaptureRequest;
import doody.spring.collection.dto.CollectionCaptureResponse;
import doody.spring.collection.dto.CollectionCaptureResponse.AriVector;
import doody.spring.collection.dto.CollectionCaptureResponse.Contact;
import doody.spring.collection.dto.CollectionCaptureResponse.Dudy;
import doody.spring.collection.dto.CollectionCaptureResponse.Reward;
import doody.spring.collection.dto.CollectionPinResponse;
import doody.spring.domain.entity.AriSnapshot;
import doody.spring.domain.entity.CollectionCapture;
import doody.spring.domain.entity.CollectionPin;
import doody.spring.domain.entity.DoodyTemplate;
import doody.spring.domain.entity.EnergyLog;
import doody.spring.domain.entity.Goal;
import doody.spring.domain.entity.User;
import doody.spring.domain.repository.AriSnapshotRepository;
import doody.spring.domain.repository.CollectionCaptureRepository;
import doody.spring.domain.repository.CollectionPinRepository;
import doody.spring.domain.repository.DoodyCollectionRepository;
import doody.spring.domain.repository.DoodyTemplateRepository;
import doody.spring.domain.repository.EnergyLogRepository;
import doody.spring.domain.repository.GoalRepository;
import doody.spring.domain.repository.UserRepository;
import doody.spring.common.service.RewardPersistenceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CollectionCaptureService {

    private static final double DEFAULT_RADIUS_METER = 500.0;
    private static final double CAPTURE_DISTANCE_METER = 100.0;
    private static final double RANDOM_PIN_RADIUS_METER = 90.0;
    private static final int DEFAULT_PIN_COUNT = 5;

    private final UserRepository userRepository;
    private final CollectionPinRepository collectionPinRepository;
    private final CollectionCaptureRepository collectionCaptureRepository;
    private final DoodyCollectionRepository doodyCollectionRepository;
    private final DoodyTemplateRepository doodyTemplateRepository;
    private final RewardPersistenceService rewardPersistenceService;
    private final GoalRepository goalRepository;
    private final EnergyLogRepository energyLogRepository;
    private final AiCollectionCaptureClient aiCollectionCaptureClient;
    private final AriSnapshotRepository ariSnapshotRepository;

    public CollectionCaptureService(
        UserRepository userRepository,
        CollectionPinRepository collectionPinRepository,
        CollectionCaptureRepository collectionCaptureRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        DoodyTemplateRepository doodyTemplateRepository,
        RewardPersistenceService rewardPersistenceService,
        GoalRepository goalRepository,
        EnergyLogRepository energyLogRepository,
        AiCollectionCaptureClient aiCollectionCaptureClient,
        AriSnapshotRepository ariSnapshotRepository
    ) {
        this.userRepository = userRepository;
        this.collectionPinRepository = collectionPinRepository;
        this.collectionCaptureRepository = collectionCaptureRepository;
        this.doodyCollectionRepository = doodyCollectionRepository;
        this.doodyTemplateRepository = doodyTemplateRepository;
        this.rewardPersistenceService = rewardPersistenceService;
        this.goalRepository = goalRepository;
        this.energyLogRepository = energyLogRepository;
        this.aiCollectionCaptureClient = aiCollectionCaptureClient;
        this.ariSnapshotRepository = ariSnapshotRepository;
    }

    @Transactional
    public List<CollectionPinResponse> getNearbyPins(BigDecimal lat, BigDecimal lng, Double radiusMeter) {
        validateLocation(lat, lng);
        double radius = radiusMeter == null ? DEFAULT_RADIUS_METER : radiusMeter;

        List<CollectionPinResponse> nearbyPins = nearbyPinResponses(lat, lng, radius);
        if (nearbyPins.size() < DEFAULT_PIN_COUNT) {
            generateRandomPins(lat, lng, DEFAULT_PIN_COUNT - nearbyPins.size());
            nearbyPins = nearbyPinResponses(lat, lng, radius);
        }

        return nearbyPins.stream()
            .limit(DEFAULT_PIN_COUNT)
            .toList();
    }

    private List<CollectionPinResponse> nearbyPinResponses(BigDecimal lat, BigDecimal lng, double radius) {
        return collectionPinRepository.findByActiveTrue().stream()
            .map(pin -> toPinResponse(pin, lat, lng))
            .filter(pin -> pin.distanceMeter() <= radius)
            .sorted(Comparator.comparing(CollectionPinResponse::distanceMeter))
            .toList();
    }

    private void generateRandomPins(BigDecimal lat, BigDecimal lng, int count) {
        List<DoodyTemplate> templates = doodyTemplateRepository.findByActiveTrue();
        if (templates.isEmpty() || count <= 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            DoodyTemplate template = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
            RandomPoint point = randomPointNear(lat.doubleValue(), lng.doubleValue());
            collectionPinRepository.save(CollectionPin.createRandom(
                template.getName() + " 발견 지점",
                toCoordinate(point.lat()),
                toCoordinate(point.lng()),
                template
            ));
        }
    }

    private RandomPoint randomPointNear(double lat, double lng) {
        double distance = RANDOM_PIN_RADIUS_METER * Math.sqrt(ThreadLocalRandom.current().nextDouble());
        double bearing = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double latOffset = (distance * Math.cos(bearing)) / 111_320.0;
        double lngOffset = (distance * Math.sin(bearing)) / (111_320.0 * Math.cos(Math.toRadians(lat)));
        return new RandomPoint(lat + latOffset, lng + lngOffset);
    }

    private BigDecimal toCoordinate(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    @Transactional
    public CollectionCaptureResponse capture(CollectionCaptureRequest request) {
        validateCaptureRequest(request);

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
        CollectionPin pin = collectionPinRepository.findByIdAndActiveTrue(request.pinId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "collection pin not found."));

        double distanceMeter = distanceMeter(request.lat(), request.lng(), pin.getLat(), pin.getLng());
        if (distanceMeter > CAPTURE_DISTANCE_METER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pin is too far to capture.");
        }

        Dudy fallbackDudy = new Dudy(
            pin.getDoodyTemplate().getId(),
            pin.getDoodyTemplate().getTier(),
            pin.getDoodyTemplate().getAxis(),
            "근처에서 발견한 두디야."
        );
        long currentCollectionCount = doodyCollectionRepository.countByUser_Id(user.getId());

        AiCaptureResult aiResult = aiCollectionCaptureClient.capture(
            new AiCaptureRequest(
                user.getId(),
                pin.getId(),
                new Location(request.lat(), request.lng()),
                currentAri(user.getId()),
                goalAri(user.getId()),
                latestEnergy(user.getId()),
                currentCollectionCount
            ),
            fallbackDudy
        );

        DoodyTemplate capturedTemplate = resolveTemplate(aiResult.dudy(), pin.getDoodyTemplate());
        Integer rewardAmount = aiResult.reward() == null ? 0 : aiResult.reward().hanaMoney();

        CollectionCapture capture = collectionCaptureRepository.save(CollectionCapture.create(
            user,
            pin.getId(),
            request.lat(),
            request.lng(),
            capturedTemplate,
            rewardAmount
        ));

        rewardPersistenceService.earnPoint(user, rewardAmount, "collection capture", "COLLECTION_CAPTURE", capture.getId());

        saveCollectedDudy(user, capture.getId(), aiResult.collectedDudy(), aiResult.dudy(), capturedTemplate);
        saveUnlockedContacts(user, capture.getId(), aiResult.unlockedContacts());
        updateGoalAri(user.getId(), aiResult.updatedAri());
        saveAriSnapshot(user, capture.getId(), aiResult.updatedAri());

        return new CollectionCaptureResponse(
            capture.getId(),
            aiResult.dudy(),
            aiResult.reward() == null ? new Reward(0) : aiResult.reward(),
            aiResult.updatedAri(),
            aiResult.appliedDelta(),
            aiResult.collectedDudy(),
            aiResult.unlockedContacts()
        );
    }

    private CollectionPinResponse toPinResponse(CollectionPin pin, BigDecimal lat, BigDecimal lng) {
        double distance = distanceMeter(lat, lng, pin.getLat(), pin.getLng());
        return new CollectionPinResponse(
            pin.getId(),
            pin.getTitle(),
            pin.getLat(),
            pin.getLng(),
            pin.getDoodyTemplate().getId(),
            pin.getDoodyTemplate().getName(),
            pin.getDoodyTemplate().getImageUrl(),
            distance,
            distance <= CAPTURE_DISTANCE_METER
        );
    }

    private void saveCollectedDudy(User user, Long captureId, List<Dudy> collectedDudy, Dudy primaryDudy, DoodyTemplate primaryTemplate) {
        boolean savedAny = false;
        if (collectedDudy != null) {
            for (Dudy dudy : collectedDudy) {
                savedAny = rewardPersistenceService.collectDoody(
                    user,
                    dudy.id(),
                    dudy.tier(),
                    dudy.axis(),
                    dudy.earnedReason() == null ? "collection capture" : dudy.earnedReason(),
                    "COLLECTION_CAPTURE",
                    captureId
                ).isPresent() || savedAny;
            }
        }
        if (!savedAny && primaryDudy != null) {
            rewardPersistenceService.collectDoody(
                user,
                primaryTemplate,
                primaryDudy.tier(),
                primaryDudy.axis(),
                primaryDudy.earnedReason() == null ? "collection capture" : primaryDudy.earnedReason(),
                "COLLECTION_CAPTURE",
                captureId
            );
        }
    }

    private void saveUnlockedContacts(User user, Long captureId, List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return;
        }
        for (Contact contact : contacts) {
            rewardPersistenceService.unlockContact(
                user,
                contact.id(),
                contact.axis() == null ? "connection" : contact.axis(),
                "COLLECTION_CAPTURE",
                captureId
            );
        }
    }
    private void updateGoalAri(String userId, AriVector updatedAri) {
        if (updatedAri == null) {
            return;
        }
        goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId)
            .ifPresent(goal -> goal.updateAri(
                goal.getRhythm(),
                toBigDecimal(updatedAri.autonomy()),
                toBigDecimal(updatedAri.connection())
            ));
    }

    private AriVector currentAri(String userId) {
        AriSnapshot snapshot = ariSnapshotRepository.findTopByUser_IdOrderByTimestampDesc(userId).orElse(null);
        if (snapshot != null) {
            return new AriVector(
                snapshot.getRhythm().doubleValue(),
                snapshot.getAutonomy().doubleValue(),
                snapshot.getConnection().doubleValue()
            );
        }
        Goal goal = goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId).orElse(null);
        return new AriVector(
            goal == null || goal.getRhythm() == null ? 0.2 : goal.getRhythm().doubleValue(),
            goal == null || goal.getAutonomy() == null ? 0.3 : goal.getAutonomy().doubleValue(),
            goal == null || goal.getConnection() == null ? 0.35 : goal.getConnection().doubleValue()
        );
    }

    private AriVector goalAri(String userId) {
        Goal goal = goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId).orElse(null);
        return new AriVector(
            goal == null || goal.getRhythm() == null ? 0.8 : goal.getRhythm().doubleValue(),
            goal == null || goal.getAutonomy() == null ? 0.8 : goal.getAutonomy().doubleValue(),
            goal == null || goal.getConnection() == null ? 0.4 : goal.getConnection().doubleValue()
        );
    }

    private Short latestEnergy(String userId) {
        return energyLogRepository.findTopByUser_IdOrderByCreatedAtDesc(userId)
            .map(EnergyLog::getEnergy)
            .orElse((short) 4);
    }

    private void saveAriSnapshot(User user, Long captureId, AriVector updatedAri) {
        if (updatedAri == null) {
            return;
        }
        BigDecimal rhythmSeed = ariSnapshotRepository.findTopByUser_IdOrderByTimestampDesc(user.getId())
            .map(AriSnapshot::getRhythm)
            .orElseGet(() -> currentRhythmSeed(user.getId()));
        ariSnapshotRepository.save(AriSnapshot.create(
            user,
            rhythmSeed,
            toBigDecimal(updatedAri.autonomy()),
            toBigDecimal(updatedAri.connection()),
            "COLLECTION_CAPTURE",
            captureId
        ));
    }

    private BigDecimal currentRhythmSeed(String userId) {
        return goalRepository.findTopByUser_IdAndActiveTrueOrderByCreatedAtDesc(userId)
            .map(Goal::getRhythm)
            .orElse(BigDecimal.valueOf(0.2));
    }

    private DoodyTemplate resolveTemplate(Dudy dudy, DoodyTemplate fallbackTemplate) {
        if (dudy == null || dudy.id() == null || dudy.id().isBlank()) {
            return fallbackTemplate;
        }
        return doodyTemplateRepository.findById(dudy.id()).orElse(fallbackTemplate);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private void validateCaptureRequest(CollectionCaptureRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
        if (request.pinId() == null || request.pinId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pinId is required.");
        }
        validateLocation(request.lat(), request.lng());
    }

    private void validateLocation(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "lat and lng are required.");
        }
    }

    private double distanceMeter(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double earthRadius = 6371000.0;
        double latDistance = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double lngDistance = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
            * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private record RandomPoint(
        double lat,
        double lng
    ) {
    }
}

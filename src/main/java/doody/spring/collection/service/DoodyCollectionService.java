package doody.spring.collection.service;

import doody.spring.collection.dto.DoodyCollectionDetailResponse;
import doody.spring.collection.dto.DoodyCollectionListResponse;
import doody.spring.domain.entity.DoodyCollection;
import doody.spring.domain.entity.DoodyCollectionDetail;
import doody.spring.domain.repository.DoodyCollectionDetailRepository;
import doody.spring.domain.repository.DoodyCollectionRepository;
import doody.spring.domain.repository.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DoodyCollectionService {

    private final UserRepository userRepository;
    private final DoodyCollectionRepository doodyCollectionRepository;
    private final DoodyCollectionDetailRepository doodyCollectionDetailRepository;

    public DoodyCollectionService(
        UserRepository userRepository,
        DoodyCollectionRepository doodyCollectionRepository,
        DoodyCollectionDetailRepository doodyCollectionDetailRepository
    ) {
        this.userRepository = userRepository;
        this.doodyCollectionRepository = doodyCollectionRepository;
        this.doodyCollectionDetailRepository = doodyCollectionDetailRepository;
    }

    @Transactional(readOnly = true)
    public List<DoodyCollectionListResponse> getCollections(String userId) {
        validateUserId(userId);
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found.");
        }

        return doodyCollectionRepository.findByUser_IdOrderByCollectedAtDesc(userId).stream()
            .map(DoodyCollectionListResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public DoodyCollectionDetailResponse getCollectionDetail(String userId, Long collectionId) {
        validateUserId(userId);
        if (collectionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "collectionId is required.");
        }

        DoodyCollection collection = doodyCollectionRepository.findByIdAndUser_Id(collectionId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "doody collection not found."));

        DoodyCollectionDetail detail = doodyCollectionDetailRepository.findByDoodyCollection_Id(collectionId)
            .orElse(null);

        return DoodyCollectionDetailResponse.from(collection, detail);
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required.");
        }
    }
}
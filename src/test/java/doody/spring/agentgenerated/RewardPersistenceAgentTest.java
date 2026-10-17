package doody.spring.agentgenerated;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import doody.spring.common.service.RewardPersistenceService;
import doody.spring.domain.entity.*;
import doody.spring.domain.repository.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RewardPersistenceAgentTest {
    @Mock PointTransactionRepository points;
    @Mock DoodyTemplateRepository templates;
    @Mock DoodyCollectionRepository collections;
    @Mock ContactUnlockRepository contacts;
    RewardPersistenceService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new RewardPersistenceService(points, templates, collections, contacts);
        user = User.create("reward-test@example.invalid", "test-user", false);
    }

    // R01: verify absence of persistence, not merely a successful return.
    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, -100})
    void R01_rejectsNullZeroAndNegativeAmounts(Integer amount) {
        service.earnPoint(user, amount, "mission", "MISSION", 17L);
        verifyNoInteractions(points);
    }

    @Test
    void R02_persistsPositiveAmountAndItsProvenance() {
        service.earnPoint(user, 30, "completed mission", "MISSION", 17L);
        var captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(points).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getAmount()).isEqualTo(30);
        assertThat(saved.getTransactionType()).isEqualTo("EARN");
        assertThat(saved.getReason()).isEqualTo("completed mission");
        assertThat(saved.getSourceType()).isEqualTo("MISSION");
        assertThat(saved.getSourceId()).isEqualTo(17L);
        verifyNoMoreInteractions(points);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void R03_blankDoodyIdNeverQueriesOrSaves(String id) {
        assertThat(service.collectDoody(user, id, null, null, null, "MISSION", 17L)).isEmpty();
        verifyNoInteractions(templates, collections);
    }

    @Test
    void R04_missingTemplateDoesNotCreateCollection() {
        when(templates.findById("missing")).thenReturn(Optional.empty());
        assertThat(service.collectDoody(user, "missing", null, null, null, "MISSION", 17L)).isEmpty();
        verify(templates).findById("missing");
        verifyNoInteractions(collections);
    }

    @Test
    void R04_nullTemplateDoesNotCreateCollection() {
        assertThat(service.collectDoody(user, (DoodyTemplate) null, null, null, null, "MISSION", 17L)).isEmpty();
        verifyNoInteractions(templates, collections);
    }

    @Test
    void R05_existingCollectionDoesNotSaveAgain() {
        var template = templateWithId();
        when(collections.existsByUser_IdAndDoodyTemplate_Id(user.getId(), "d01")).thenReturn(true);
        // If a regression saves anyway, return a real entity so the failure
        // identifies duplicate acceptance rather than a mock-induced NPE.
        lenient().when(collections.save(any(DoodyCollection.class))).thenAnswer(call -> call.getArgument(0));
        assertThat(service.collectDoody(user, template, null, null, null, "MISSION", 17L)).isEmpty();
        verify(collections, never()).save(any());
    }

    @Test
    void R06_nullMetadataUsesTemplateAndSourceDefaults() {
        var template = templateWithId();
        when(template.getTier()).thenReturn("common");
        when(template.getAxis()).thenReturn("connection");
        when(collections.save(any(DoodyCollection.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.collectDoody(user, template, null, null, null, "MISSION", 17L);
        var captor = ArgumentCaptor.forClass(DoodyCollection.class);
        verify(collections).save(captor.capture());
        var saved = captor.getValue();
        assertThat(result).contains(saved);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getDoodyTemplate()).isSameAs(template);
        assertThat(saved.getTier()).isEqualTo("common");
        assertThat(saved.getAxis()).isEqualTo("connection");
        assertThat(saved.getEarnedReason()).isEqualTo("MISSION");
        assertThat(saved.getSource()).isEqualTo("MISSION");
        assertThat(saved.getSourceId()).isEqualTo(17L);
    }

    @Test
    void R06_explicitMetadataIsPreserved() {
        var template = templateWithId();
        when(collections.save(any(DoodyCollection.class))).thenAnswer(call -> call.getArgument(0));
        var saved = service.collectDoody(user, template, "rare", "autonomy", "special reward", "COLLECTION_CAPTURE", 25L).orElseThrow();
        assertThat(saved.getTier()).isEqualTo("rare");
        assertThat(saved.getAxis()).isEqualTo("autonomy");
        assertThat(saved.getEarnedReason()).isEqualTo("special reward");
        assertThat(saved.getSource()).isEqualTo("COLLECTION_CAPTURE");
        assertThat(saved.getSourceId()).isEqualTo(25L);
        verify(collections).save(saved);
    }

    @Test
    void R06_idOverloadFindsTemplateAndPersistsResolvedCollection() {
        var template = templateWithId();
        when(templates.findById("d01")).thenReturn(Optional.of(template));
        when(collections.save(any(DoodyCollection.class))).thenAnswer(call -> call.getArgument(0));
        var saved = service.collectDoody(user, "d01", "rare", "autonomy", "reason", "MISSION", 17L).orElseThrow();
        assertThat(saved.getDoodyTemplate()).isSameAs(template);
        verify(templates).findById("d01");
        verify(collections).save(saved);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void R07_blankContactIdDoesNotAccessStorage(String id) {
        assertThat(service.unlockContact(user, id, "connection", "MISSION", 17L)).isEmpty();
        verifyNoInteractions(contacts);
    }

    @Test
    void R07_existingContactIsReturnedWithoutOverwritingItsOrigin() {
        var existing = ContactUnlock.create(user, "c01", "connection", "EARLIER", 2L);
        when(contacts.findByUser_IdAndContactId(user.getId(), "c01")).thenReturn(Optional.of(existing));
        var result = service.unlockContact(user, "c01", "autonomy", "MISSION", 17L);
        assertThat(result).contains(existing);
        assertThat(existing.getSource()).isEqualTo("EARLIER");
        assertThat(existing.getSourceId()).isEqualTo(2L);
        verify(contacts, never()).save(any());
    }

    @Test
    void R08_newContactPersistsAllProvenanceFields() {
        when(contacts.findByUser_IdAndContactId(user.getId(), "c01")).thenReturn(Optional.empty());
        when(contacts.save(any(ContactUnlock.class))).thenAnswer(call -> call.getArgument(0));
        var result = service.unlockContact(user, "c01", "connection", "MISSION", 17L);
        var captor = ArgumentCaptor.forClass(ContactUnlock.class);
        verify(contacts).save(captor.capture());
        var saved = captor.getValue();
        assertThat(result).contains(saved);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getContactId()).isEqualTo("c01");
        assertThat(saved.getAxis()).isEqualTo("connection");
        assertThat(saved.getSource()).isEqualTo("MISSION");
        assertThat(saved.getSourceId()).isEqualTo(17L);
    }

    @Test
    void storageFailureIsNotReportedAsSuccessfulReward() {
        var failure = new IllegalStateException("storage unavailable");
        when(points.save(any(PointTransaction.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.earnPoint(user, 10, "reason", "MISSION", 17L)).isSameAs(failure);
    }

    private DoodyTemplate templateWithId() {
        var template = mock(DoodyTemplate.class);
        when(template.getId()).thenReturn("d01");
        return template;
    }
}

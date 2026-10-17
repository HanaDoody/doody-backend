package doody.spring.collection.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import doody.spring.collection.dto.CollectionCaptureRequest;
import doody.spring.collection.dto.CollectionCaptureResponse;
import doody.spring.collection.service.CollectionCaptureService;
import doody.spring.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

// HTTP controller contract only. The capture service is mocked.
@ExtendWith(MockitoExtension.class)
class CollectionCaptureApiContractTest {
    @Mock CollectionCaptureService service;
    MockMvc mvc;
    static final String BODY = """
        {"userId":"user-1","pinId":"pin-1","lat":37.5,"lng":127.0}
        """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CollectionCaptureController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void serializesCaptureResponseAndDeserializesRequest() throws Exception {
        var dudy = new CollectionCaptureResponse.Dudy("d01", "common", "connection", "nearby");
        when(service.capture(any())).thenReturn(new CollectionCaptureResponse(
            1L, dudy, new CollectionCaptureResponse.Reward(30),
            new CollectionCaptureResponse.AriVector(0.8, 0.3, 0.4),
            new CollectionCaptureResponse.AriVector(0.0, 0.0, 0.05), List.of(dudy), List.of()));
        mvc.perform(post("/api/collection/capture").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.message").value("collection capture success."))
            .andExpect(jsonPath("$.result.captureId").value(1))
            .andExpect(jsonPath("$.result.reward.hanaMoney").value(30))
            .andExpect(jsonPath("$.result.dudy.id").value("d01"))
            .andExpect(jsonPath("$.result.updatedAri.rhythm").value(0.8))
            .andExpect(jsonPath("$.result.appliedDelta.connection").value(0.05))
            .andExpect(jsonPath("$.result.collectedDudy[0].id").value("d01"))
            .andExpect(jsonPath("$.result.unlockedContacts").isEmpty());
        var request = ArgumentCaptor.forClass(CollectionCaptureRequest.class);
        verify(service).capture(request.capture());
        assertThat(request.getValue().userId()).isEqualTo("user-1");
        assertThat(request.getValue().pinId()).isEqualTo("pin-1");
        assertThat(request.getValue().lat()).isEqualByComparingTo("37.5");
        assertThat(request.getValue().lng()).isEqualByComparingTo("127.0");
    }

    @Test
    void mapsServiceBadRequestTo400Envelope() throws Exception {
        when(service.capture(any())).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "pin is too far to capture."));
        mvc.perform(post("/api/collection/capture").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.message").value("pin is too far to capture."))
            .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void mapsMissingResourceTo404Envelope() throws Exception {
        when(service.capture(any())).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found."));
        mvc.perform(post("/api/collection/capture").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.isSuccess").value(false)).andExpect(jsonPath("$.message").value("user not found."));
    }

    @Test
    void unexpectedErrorsDoNotLeakExceptionDetails() throws Exception {
        when(service.capture(any())).thenThrow(new IllegalStateException("internal database details"));
        mvc.perform(post("/api/collection/capture").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.isSuccess").value(false)).andExpect(jsonPath("$.message").value("internal server error."));
    }
}

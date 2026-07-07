package doody.spring.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AiEngineWarmupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AiEngineWarmupScheduler.class);

    private final String baseUrl;
    private final RestClient restClient;

    public AiEngineWarmupScheduler(
        @Value("${AI_ENGINE_BASE_URL:}") String baseUrl,
        @Qualifier("aiEngineRestClient") RestClient restClient
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        this.restClient = restClient;
    }

    @Scheduled(
        fixedDelayString = "${AI_ENGINE_WARMUP_INTERVAL_MS:600000}",
        initialDelayString = "${AI_ENGINE_WARMUP_INITIAL_DELAY_MS:30000}"
    )
    public void warmup() {
        if (baseUrl.isBlank()) {
            return;
        }

        try {
            restClient.get()
                .uri(baseUrl + "/health")
                .retrieve()
                .toBodilessEntity();
        } catch (Exception exception) {
            log.warn("AI engine warmup failed. url={}", baseUrl + "/health", exception);
        }
    }
}

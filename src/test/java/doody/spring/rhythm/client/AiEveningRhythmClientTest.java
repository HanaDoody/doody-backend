package doody.spring.rhythm.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiEveningRhythmClientTest {

    @Test
    void sendsOnlyContractFieldsAndUsesAiReply() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AiEveningRhythmClient client = new AiEveningRhythmClient("https://ai.example", builder.build());

        server.expect(requestTo("https://ai.example/rhythm/evening"))
            .andExpect(content().json("""
                {"user_id":"user-123","text":"사람 만나는 게 싫어 외로워"}
                """, true))
            .andRespond(withSuccess("""
                {
                  "reward":{"hana_money":20},
                  "signals":{"connection":0.8},
                  "reply":"외롭다는 마음을 들려줘서 고마워.",
                  "collected_dudy":[{
                    "id":"d_first_night",
                    "tier":"common",
                    "axis":null,
                    "earned_reason":"처음으로 저녁 감정을 남겼어요"
                  }]
                }
                """, MediaType.APPLICATION_JSON));

        var result = client.leaveNote(
            "user-123",
            LocalDateTime.of(2026, 7, 17, 21, 0),
            "사람 만나는 게 싫어 외로워"
        );

        assertThat(result.hanaMoney()).isEqualTo(20);
        assertThat(result.reply()).isEqualTo("외롭다는 마음을 들려줘서 고마워.");
        assertThat(result.signals()).isEqualTo("{connection=0.8}");
        assertThat(result.collectedDudy()).hasSize(1);
        assertThat(result.collectedDudy().getFirst().axis()).isNull();
        server.verify();
    }

    @Test
    void fallsBackWithContractRewardAndMessageWhenAiIsUnavailable() {
        AiEveningRhythmClient client = new AiEveningRhythmClient("", RestClient.create());

        var result = client.leaveNote("user-123", LocalDateTime.now(), "오늘 힘들었어");

        assertThat(result.hanaMoney()).isEqualTo(20);
        assertThat(result.reply()).isEqualTo("오늘 하루도 수고 많았어. 이야기해줘서 고마워.");
        assertThat(result.collectedDudy()).isEmpty();
    }
}

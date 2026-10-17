package doody.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "AI_ENGINE_BASE_URL=")
@ActiveProfiles("test")
class ApplicationTests {

	@Test
	void contextLoads() {
	}

}

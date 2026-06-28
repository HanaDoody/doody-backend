package doody.spring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI doodyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Doody API")
                        .description("Doody Spring Boot API Documentation")
                        .version("v1"));
    }
}
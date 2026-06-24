package org.weatherservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI weatherServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather Service API")
                        .version("1.0")
                        .description("REST API contract for fetching cached weather observations.")
                        .contact(new Contact().name("Weather Service Team"))
                        .license(new License().name("Apache-2.0")));
    }
}

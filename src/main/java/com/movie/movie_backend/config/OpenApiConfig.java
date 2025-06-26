package com.movie.movie_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI movieApiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie API")
                        .description("영화 정보 및 박스오피스 API (왓챠피디아 스타일)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Movie API Team")
                                .email("api@movie.com")
                                .url("https://github.com/movie-api"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost").description("개발 서버"),
                        new Server().url("https://api.movie.com").description("운영 서버")
                ));
    }
} 

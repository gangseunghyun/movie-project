package com.movie.movie_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // JavaTimeModule 등록
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        
        // LocalDate Serializer 설정
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        
        // LocalDate Deserializer 설정 - 여러 형식 지원
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter) {
            @Override
            public LocalDate deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
                String dateStr = p.getValueAsString();
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    return null;
                }
                
                // 여러 날짜 형식 시도
                DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("yyyyMMdd"),
                    DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                    DateTimeFormatter.ISO_LOCAL_DATE
                };
                
                for (DateTimeFormatter formatter : formatters) {
                    try {
                        return LocalDate.parse(dateStr, formatter);
                    } catch (DateTimeParseException e) {
                        // 다음 형식 시도
                        continue;
                    }
                }
                
                // 모든 형식이 실패하면 기본 형식으로 시도
                return super.deserialize(p, ctxt);
            }
        });
        
        // LocalDateTime Serializer 설정
        javaTimeModule.addSerializer(new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        objectMapper.registerModule(javaTimeModule);
        
        // 순환 참조 방지 설정
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // 무한 루프 방지
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        
        // 순환 참조 방지를 위한 추가 설정
        objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, true);
        
        return objectMapper;
    }
} 

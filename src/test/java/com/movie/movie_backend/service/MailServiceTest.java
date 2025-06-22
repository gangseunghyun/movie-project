package com.movie.movie_backend.service;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource("classpath:application-test.properties")
public class MailServiceTest {

    @Autowired
    private MailService mailService;

    @Test
    public void sendTestEmail() throws MessagingException {
        mailService.sendVerificationEmail("tmdgusl0907@naver.com");


    }

}

package com.nevgiu.hrai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootApplication

public class HrAiRecruitmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrAiRecruitmentApplication.class, args);
    }

}

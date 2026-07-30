package com.aivle13.fin_audit_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class FinAuditAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinAuditAiApplication.class, args);
	}

}

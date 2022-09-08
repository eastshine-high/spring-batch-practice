package com.eastshine.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBatchProcessing // 본 애플리케이션이 배치 프로세싱을 하겠다는 것을 의미
public class SpringBatchPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchPracticeApplication.class, args);
	}
}

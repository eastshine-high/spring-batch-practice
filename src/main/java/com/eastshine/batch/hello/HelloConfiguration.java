package com.eastshine.batch.hello;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class HelloConfiguration {

    private JobBuilderFactory jobBuilderFactory; // SpringBatch 설정으로 빈으로 주입받을 수 있다.
    private StepBuilderFactory stepBuilderFactory;

    public HelloConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job helloJob() { // Job은 배치의 실행 단위이다.
        return jobBuilderFactory.get("helloJob") // name은 스프링 배치를 실행할 수 있는 key이키도 하다.
                .incrementer( // 실행 단위를 구분,
                        new RunIdIncrementer()) // Job이 실행할 때 마다, parameter id를 자동으로 생성
                .start(helloStep()) // Job 실행 시 최초로 실행될 스텝을 설정
                .build();
    }

    @Bean
    public Step helloStep() { // Job의 실행 단위
        return stepBuilderFactory.get("helloStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("hello spring batch!");
                    return RepeatStatus.FINISHED;
                }).build();
    }
}

package com.eastshine.batch.usage.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 웹 애플리케이션은 일반적으로 HttpSession을 사용해 상태를 저장한다. ExecutionContext는 기본적으로 배치 잡의 세션입니다.
 * ExecutionContext는 간단한 키-값 쌍을 보관하는 도구에 불과합니다. 그러나 ExecutionContext는 잡의 상태를 안전하게 보관하는 방법을 제공합니다.
 * 웹 애플리케이션의 세션과 ExecutionContext의 한 가지 차이점은, 잡을 다루는 과정에서 실제로 여러 개의 ExecutionContext가 존재할 수 있다는 점입니다.
 * JobExecution처럼 각 StepExecution도 마찬가지로 ExecutionContext를 가집니다. 이렇게 함으로써 적절한 수준(개별 스텝용 데이터 또는 글로벌 데이터)으로 데이터 사용 범위를 지정할 수 있습니다.
 * (사용 범위)JobExecutionContext와 StepExecutionContext는 각각 하나의 Job과 하나의 Step에서만 공유할 수 있습니다. 따라서 JobExecutionContext은 스텝끼리 공유할 수 있지만 StepExecutionContext은 스텝끼리 공유할 수 없습니다.
 */
@Slf4j
@Configuration
public class ExecutionContextConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public ExecutionContextConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job shareJob() {
        return jobBuilderFactory.get("shareJob")
                .incrementer(new RunIdIncrementer())
                .start(this.shareStep1())
                .next(this.shareStep2())
                .build();
    }

    /**
     * ExecutionContext에 값을 넣습니다.
     */
    // job execution context와 step의 execution context의 사용 범위를 이해한다.
    @Bean
    public Step shareStep1() {
        return stepBuilderFactory.get("shareStep1")
                .tasklet((contribution, chunkContext) -> {
                    /**
                     * Step ExecutionContext에 접근하여 값을 저장합니다.
                     * ExecutionContext에 접근하는 과정을 메타 테이블과 연관지어 생각할 수 있으면 좋습니다.
                     */
                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();
                    stepExecutionContext.putString("step", "step execution context");

                    /**
                     * Job ExecutionContext에 접근하여 값을 저장합니다.
                     * ExecutionContext에 접근하는 과정을 메타 테이블과 연관지어 생각할 수 있으면 좋습니다.
                     */
                    JobExecution jobExecution = stepExecution.getJobExecution();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();
                    jobExecutionContext.putString("job", "job execution context");

                    // log
                    JobInstance jobInstance = jobExecution.getJobInstance();
                    JobParameters jobParameters = jobExecution.getJobParameters();
//                    JobParameters jobParameters1 = stepExecution.getJobParameters();

                    log.info("jobName : {}, stepName : {}, run.id : {}",
                            jobInstance.getJobName(),
                            stepExecution.getStepName(),
                            jobParameters.getLong("run.id"));

                    return RepeatStatus.FINISHED;
                }).build();
    }

    /**
     * shareStep1에서 ExecutionContext에 넣은 값을 출력합니다.
     */
    @Bean
    public Step shareStep2() {
        return stepBuilderFactory.get("shareStep2")
                .tasklet((contribution, chunkContext) -> {
                    // step ExecutionContext.get
                    StepExecution stepExecution = contribution.getStepExecution();
                    ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();

                    // job ExecutionContext.get
                    JobExecution jobExecution = stepExecution.getJobExecution();
                    ExecutionContext jobExecutionContext = jobExecution.getExecutionContext();

                    // log
                    log.info("jobValue : {}, stepValue : {}",
                            jobExecutionContext.getString("job", "emptyJob"), // "job execution context" 출력
                            stepExecutionContext.getString("step", "emptyStep")); // "emptyStep" 출력
                    // 머릿말에서 설명한 것과 같이 JobExecutionContext은 스텝끼리 공유할 수 있지만 StepExecutionContext은 스텝끼리 공유할 수 없습니다.

                    return RepeatStatus.FINISHED;

                }).build();
    }
}

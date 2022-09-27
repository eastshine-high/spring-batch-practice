package com.eastshine.batch.usage.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Job Parameter는 스프링 배치에서 실행 시점에 외부에서 주입되는 파라미터로 좀 더 유연한 프로그램을 만들 수 있는 방법입니다.
 *
 * 잡 파라미터에 접근하는 방법은 2가지가 있습니다.
 * 1. Tasklet의 stepContribution에서 접근할 수 있습니다.
 * com.eastshine.batch.usage.job.step.tasklet.TaskletProcessingConfiguration 클래스의 예시 참조(여기서는 두 번째 방법의 예시를 정리합니다).
 *
 * 2 Late binding : 스프링의 기능을 사용해 잡 파라미터를 컴포넌트에 주입하는 방식으로, 가장 보편적인 접근 방법입니다.
 * 이 방법으로 Job Parameter를 사용하기 위해선 항상 Spring Batch 전용 Scope를 선언해야만 합니다. 크게 @StepScope와 @JobScope 2가지가 있습니다.
 * @JobScope는 Step 선언문에서 사용 가능하고, @StepScope는 Tasklet이나 ItemReader, ItemWriter, ItemProcessor에서 사용할 수 있습니다.
 */
// --job.name=jobParameters -name=human
@Slf4j
@Configuration
public class JobParametersConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public JobParametersConfiguration(JobBuilderFactory jobBuilderFactory,
                                      StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job jobParameters() {
        return jobBuilderFactory.get("jobParameters")
                .incrementer(new UniqueRunIdIncrementer())
                .start(lateBindingOfStep(null)) // null 아규먼트를 전달해도 정상적으로 작동하는 이유는 @JobScope 어노테이션 때문입니다.
                .build();
    }

    /**
     * lazy binding(늦은 바인딩)
     * spring이 제공하는 value 어노테이션과 SpEL(Spring Expression Lenguage)을 사용합니다.
     * Bean의 생성 시점을 지정된 Scope가 실행되는 시점으로 지연시킵니다.
     * JobScope StepScope를 설정한 Bean의 생명 주기는 Job 또는 Step의 실행과 종료 시점에 생성되고 소멸됩니다.
     * Bean의 생성시점을 어플리케이션 실행 시점이 아닌, Step 혹은 Job의 실행시점으로 지연시키면서 얻는 장점으로
     * 1. JobParameter의 Late Binding이 가능합니다.
     * 2. 동일한 컴포넌트를 병렬 혹은 동시에 사용할때 유용합니다.
     */
    @Bean
    @JobScope
    public Step lateBindingOfStep( @Value("#{JobParameters[name]}") String name) {
        log.info("print late binding of Step  : " + name);
        return stepBuilderFactory.get("lateBindingOfStep")
                .tasklet(printJobParameters(null))
                .build();
    }

    @Bean
    @StepScope
    public Tasklet printJobParameters( @Value("#{JobParameters[name]}") String name) {
        return (contribution, chunkContext) -> {
            log.info("print Job Parameters  : " + name);
            return RepeatStatus.FINISHED;
        };
    }
}

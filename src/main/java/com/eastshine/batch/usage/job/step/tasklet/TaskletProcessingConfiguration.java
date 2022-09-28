package com.eastshine.batch.usage.job.step.tasklet;

import com.eastshine.batch.usage.job.UniqueRunIdIncrementer;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 테스크 기반 처리
 * 배치를 처리할 수 있는 방법은 크게 2가지입니다.
 * 1. Tasklet을 사용한 Task 기반 처리는 배치 처리 과정이 비교적 쉬운 경우 쉽게 사용합니다.
 * 2. 대량 처리를 하는 배치 프로세싱의 경우는 chunk 기반 처리를 사용하는 것이 권장됩니다.
 *
 * 실행 파라미터 --job.name=chunkProcessingJob -chunkSize=20
 */
@Configuration
@Slf4j
public class TaskletProcessingConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public TaskletProcessingConfiguration(JobBuilderFactory jobBuilderFactory,
                                          StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job chunkProcessingJob() {
        return jobBuilderFactory.get("chunkProcessingJob")
                .incrementer(new UniqueRunIdIncrementer())
                .start(this.taskBaseStep())
                .build();
    }

    @Bean
    public Step taskBaseStep() {
        return stepBuilderFactory.get("taskBaseStep")
                .tasklet(tasklet())
                .build();
    }

    @Bean
    @StepScope
    public Tasklet tasklet() {
        List<String> items = getItems();

        /**
         * StepContribution : StepContribution은 아직 커밋되지 않은 현재 트랜잭션에 대한 정보(쓰기 수, 읽기 수 등)를 가지고 있습니다.
         * Represents a contribution to a StepExecution, buffering changes until they can be applied at a chunk boundary.
         * 청크 경계에 적용될 수 있을 때까지 변경 사항을 버퍼링하는 StepExecution에 대한 기여를 나타냅니다.
         *
         * ChunkContext : 실행 시점의 잡 상태를 제공한다. 또한 태스크릿 내에서는 처리 중인 청크와 관련된 정보도 갖고 있다.
         * Context object for weakly typed data stored for the duration of a chunk (usually a group of items processed together in a transaction).
         * If there is a rollback and the chunk is retried the same context will be associated with it.
         * 청크(일반적으로 트랜잭션에서 함께 처리되는 항목 그룹) 기간 동안 저장된 약한 형식의 데이터에 대한 컨텍스트 객체입니다.
         * 롤백이 있고 청크가 재시도되면 동일한 컨텍스트가 관련됩니다.
         * 청크 정보는 스텝 및 잡과 관련된 정보도 갖고 있습니다. 예를 들어, 청크 정보에서도 잡 파라미터에 접근할 수 있습니다.
         */
        return (stepContribution, chunkContext) -> {
            StepExecution stepExecution = stepContribution.getStepExecution();
            JobParameters jobParameters = stepExecution.getJobParameters(); // Tasklet에서는 다음 방식으로 JobParameters를 이용할 수 있다.
            String value = jobParameters.getString("chunkSize", "10");

            int chunkSize = StringUtils.isNotEmpty(value) ? Integer.parseInt(value) : 10;

            int fromIndex = stepExecution.getReadCount();
            int toIndex = fromIndex + chunkSize;

            if (fromIndex >= items.size()) {
                return RepeatStatus.FINISHED;
            }

            List<String> subList = items.subList(fromIndex, toIndex);

            log.info("task item size : {}", subList.size());

            stepExecution.setReadCount(toIndex);

            return RepeatStatus.CONTINUABLE;
        };
    }

    private List<String> getItems() {
        List<String> items = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            items.add(i + " Hello");
        }

        return items;
    }

}

package com.eastshine.batch.usage.job.step.chunk.processor;

import com.eastshine.batch.usage.domain.Person;
import com.eastshine.batch.usage.job.UniqueRunIdIncrementer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.item.support.builder.CompositeItemProcessorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * ItemProcessor는 잡이 처리하는 아이템에 비즈니스 로직을 적용하는 곳입니다.
 * 다음과 같은 몇 가지 작업을 수행할 수 있습니다.
 * 입력의 유효성 검증
 * 기존 서비스의 재사용(ItemProcessorAdapter)
 * 스크립트 실행(ScriptItemProcessor)
 * 아이템 필터링
 * ItemProcessor의 체인(CompositeItemProcessor) : 작성한 모든 프로세서들을 조합
 */
@Slf4j
@Configuration
public class ItemProcessorConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    public ItemProcessorConfiguration(JobBuilderFactory jobBuilderFactory,
                                      StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    public Job itemProcessorJob() throws Exception {
        return this.jobBuilderFactory.get("itemProcessorJob")
                .incrementer(new UniqueRunIdIncrementer())
                .start(this.itemProcessorStep())
                .build();
    }

    @Bean
    public Step itemProcessorStep() throws Exception {
        return this.stepBuilderFactory.get("itemProcessorStep")
                .<Person, Person>chunk(10)
                .reader(itemReader())
                .processor(itemProcessor())
                .writer(itemWriter())
                .build();
    }

    private ItemWriter<? super Person> itemWriter() {
        return items -> items.forEach(x -> log.info("PERSON.ID : {}", x.getId()));
    }

    /**
     * CompositeItemProcessor을 이용해 작성한 Processor들을 연결(chain)할 수 있습니다.
     */
    @Bean
    public ItemProcessor<? super Person, ? extends Person> itemProcessor() throws Exception {
        CompositeItemProcessor<Person, Person> itemProcessor = new CompositeItemProcessorBuilder<Person, Person>()
                .delegates(filteringOddNumberIdProcessor(), new DuplicateValidationProcessor<>(Person::getName))
                .build(); // delegates()에 전달한 아규먼트의 순서대로 프로세서 체인이 동작합니다.

        itemProcessor.afterPropertiesSet();
        return itemProcessor;
    }

    /**
     * 홀수 id를 필터링한다.
     */
    private ItemProcessor<? super Person, ? extends Person> filteringOddNumberIdProcessor() {
        return item -> {
            if (item.getId() % 2 == 0) {
                return item;
            }

            return null; // ItemProcessor에서 null을 리턴하면 해당 아이템은 처리하지 않습니다.
        };
    }

    private ItemReader<Person> itemReader() {
        return new ListItemReader<>(getItems());
    }

    private List<Person> getItems() {
        List<Person> items = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            items.add(new Person(i + 1, "test name" + i, "test age", "test address"));
        }

        return items;
    }
}

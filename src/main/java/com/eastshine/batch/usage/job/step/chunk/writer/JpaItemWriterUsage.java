package com.eastshine.batch.usage.job.step.chunk.writer;

import com.eastshine.batch.usage.domain.Person;
import com.eastshine.batch.usage.domain.Person2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JpaItemWriterUsage {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final EntityManagerFactory entityManagerFactory;

    private static final int CHUNK_SIZE = 10;

    @Bean
    public Job jpaItemWriterJob() {
        return jobBuilderFactory.get("jpaItemWriterJob")
                .start(jpaItemWriterStep())
                .build();
    }

    @Bean
    public Step jpaItemWriterStep() {
        return stepBuilderFactory.get("jpaItemWriterStep")
                .<Person, Person2>chunk(CHUNK_SIZE)
                .reader(jpaItemWriterReader())
                .processor(jpaItemProcessor())
                .writer(jpaItemWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Person> jpaItemWriterReader() {
        return new JpaPagingItemReaderBuilder<Person>()
                .name("jpaItemWriterReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT p FROM Person p")
                .build();
    }

    @Bean
    public ItemProcessor<Person, Person2> jpaItemProcessor() {
        return person -> new Person2(person.getName(), person.getAge(), person.getAddress());
    }


    /**
     * JpaItemWriter를 보면 EmtityManager를 감싼 간단한 래퍼에 불과하다는 것을 알 수 있습니다.
     * 청크가 완료되면 청크 내의 아이템 목록이 JpaItemWriter로 전달됩니다.
     * JpaItemWriter는 모든 아이템을 저장한 뒤 flush를 호출하기 전에 아이템 목록 내 아이템을 순회하면서 아이템마다 EntityManager의 merge 메서드를 호출합니다.
     * JpaItemWriter는 Entity 클래스를 제네릭 타입으로 받아야만 합니다.
     * JdbcBatchItemWriter의 경우 DTO 클래스를 받더라도 sql로 지정된 쿼리가 실행되니 문제가 없지만, JpaItemWriter 는 넘어온 Item을 그대로 entityManger.merge()로 테이블에 반영합니다.
     */
    @Bean
    public JpaItemWriter<Person2> jpaItemWriter() {
        JpaItemWriter<Person2> jpaItemWriter = new JpaItemWriter<>();
        jpaItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaItemWriter;
    }
}

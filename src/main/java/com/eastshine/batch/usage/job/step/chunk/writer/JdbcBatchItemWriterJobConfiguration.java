package com.eastshine.batch.usage.job.step.chunk.writer;

import com.eastshine.batch.usage.domain.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcBatchItemWriterJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    private static final int chunkSize = 10;

    @Bean
    public Job jdbcBatchItemWriterJob() {
        return jobBuilderFactory.get("jdbcBatchItemWriterJob")
                .start(jdbcBatchItemWriterStep())
                .build();
    }

    @Bean
    public Step jdbcBatchItemWriterStep() {
        return stepBuilderFactory.get("jdbcBatchItemWriterStep")
                .<Person, Person>chunk(chunkSize)
                .reader(jdbcBatchItemWriterReader())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Person> jdbcBatchItemWriterReader() {
        return new JdbcCursorItemReaderBuilder<Person>()
                // .fetchSize(chunkSize) -> InvalidDataAccessResourceUsageException: Unexpected cursor position change.
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Person.class))
                .sql("select id, name, age, address from person")
                .name("jdbcBatchItemWriter")
                .build();
    }

    /**
     * reader에서 넘어온 데이터를 하나씩 출력하는 writer
     */
    @Bean // beanMapped()을 사용할때는 필수
    public JdbcBatchItemWriter<Person> jdbcBatchItemWriter() {
        return new JdbcBatchItemWriterBuilder<Person>()
                .dataSource(dataSource)
                .sql("insert into person2(name, age, address) values (:name, :age, :address)")
                .beanMapped() // Pojo(Person) 기반으로 Insert SQL의 Values를 매핑합니다
                .assertUpdates(true) // 적어도 하나의 항목이 행을 업데이트하거나 삭제하지 않을 경우 예외를 throw할지 여부를 설정합니다. 기본값은 true입니다. Exception:EmptyResultDataAccessException
                .build();
    }

    /*
    public JdbcBatchItemWriter<Map<String, Object>> jdbcBatchItemWriterWithColumnMapped() {
        return new JdbcBatchItemWriterBuilder<Map<String, Object>>()
                .columnMapped() // Key,Value 기반으로 Insert SQL의 Values를 매핑합니다 (ex: Map<String, Object>)
                .dataSource(this.dataSource)
                .sql("insert into person2(name, age, address) values (:name, :age, :address)")
                .build();
    }
    */
}

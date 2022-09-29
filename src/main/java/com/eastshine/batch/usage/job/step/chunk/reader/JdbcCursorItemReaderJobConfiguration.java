package com.eastshine.batch.usage.job.step.chunk.reader;

import com.eastshine.batch.usage.domain.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

/**
 * Cursor 기반 조회
 * 배치 처리가 완료될 때 까지 DB Connection이 연결(Paging과 다르게 Streaming 으로 데이터를 처리합니다)
 * DB Connection 빈도가 낮아 성능이 좋은 반면, 긴 Connection 유지 시간 필요
 * 하나의 Connection에서 처리되기 때문에, Thread Safe 하지 않음
 * 모든 결과를 메모리에 할당하기 때문에, 더 많은 메모리를 사용
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class JdbcCursorItemReaderJobConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;

    private static final int chunkSize = 10;

    @Bean
    public Job jdbcCursorItemReaderJob() throws Exception {
        return jobBuilderFactory.get("jdbcCursorItemReaderJob")
                .incrementer(new RunIdIncrementer())
                .start(jdbcCursorItemReaderStep())
                .build();
    }


    @Bean
    public Step jdbcCursorItemReaderStep() throws Exception {
        return stepBuilderFactory.get("jdbcCursorItemReaderStep")
                .<Person, Person>chunk(chunkSize)
        /**
         * <Person, Person>
         * 첫번째 파라미터 - Reader에서 반환할 타입(이것을 ItemProcessor에서 받아서 처리하고 Output한다).
         * 두번째 파라미터 - Person Writer에 파라미터로 넘어올 타입.
         * chunkSize로 인자값을 넣은 경우는 Reader & Writer가 묶일 Chunk 트랜잭션 범위이다.
         */
                .reader(jdbcCursorItemReader())
                .writer(jdbcCursorItemWriter())
                .build();
    }

    @Bean
    public JdbcCursorItemReader<Person> jdbcCursorItemReader() throws Exception {
        JdbcCursorItemReader<Person> itemReader = new JdbcCursorItemReaderBuilder<Person>()
                .name("jdbcCursorItemReader") // reader의 이름을 지정, Bean의 이름이 아니며 Spring Batch의 ExecutionContext에서 저장되어질 이름.
                .dataSource(dataSource)
                .sql("select id, name, age, address from person")
                .rowMapper(new BeanPropertyRowMapper<>(Person.class))
                /*.
                 * 쿼리 결과를 Java 인스턴스로 매핑하기 위한 Mapper
                 * 커스텀하게 생성해서 사용할 수 도 있지만, 이렇게 될 경우 매번 커스텀 Mapper 클래스를 생성해야 된다.
                 * 보편적으로 Spring에서 공식적으로 지원하는 BeanPropertyRowMapper.class를 많이 사용.
                 * 아래는 커스텀 매퍼 생성 예시
                .rowMapper((rs, rowNum) -> new Person(
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)))*/
                .build();
        itemReader.afterPropertiesSet(); // Writer들이 실행되기 위해 필요한 필수값들이 제대로 세팅되어있는지를 체크합니다.
        return itemReader;
    }

    private ItemWriter<Person> jdbcCursorItemWriter() {
        return list -> {
            for (Person person: list) {
                log.info("Current Person={}", person);
            }
        };
    }
}

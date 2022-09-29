package com.eastshine.batch.usage.job.step.chunk.reader;

import com.eastshine.batch.usage.domain.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;


@Slf4j
@RequiredArgsConstructor
@Configuration
public class FlatFileItemReaderUsage {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    private int chunkSize = 10;

    @Bean
    public Job flatFileItemReaderJob() throws Exception {
        return jobBuilderFactory.get("flatFileItemReaderJob")
                .start(csvFileStep())
                .build();
    }

    @Bean
    public Step csvFileStep() throws Exception {
        return stepBuilderFactory.get("csvFileStep")
                .allowStartIfComplete(true) //테스트를 위한 옵션
                .<Person, Person>chunk(chunkSize)
                .reader(csvFileItemReader())
                .writer(itemWriter())
                .build();
    }

    private FlatFileItemReader<Person> csvFileItemReader() throws Exception {
        DefaultLineMapper<Person> lineMapper = new DefaultLineMapper<>(); // 파일을 한 줄씩 읽어들인다.
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "name", "age", "address"); // csv파일을 Person 객체에 매핑하기 위해서 Person 필드명을 설정.
        lineMapper.setLineTokenizer(tokenizer);

        lineMapper.setFieldSetMapper(fieldSet ->{
        int id = fieldSet.readInt("id");
        String name = fieldSet.readString("name");
        String age = fieldSet.readString("age");
        String address = fieldSet.readString("address"); // csv 파일에서 읽은 값을

        return new Person(id, name, age, address); // Person 객체에 매핑
            });

        FlatFileItemReader<Person> itemReader = new FlatFileItemReaderBuilder<Person>()
                .name("csvFileItemReader")
                .encoding("UTF-8")
                .resource(new ClassPathResource("person.csv")) //ClassPathResource는 스프링에서 제공
                .linesToSkip(1) // 파일의 2번째 라인부터 읽는다.
                .lineMapper(lineMapper) // 위에서 작성한 lineMapper 적용
                .build();
        itemReader.afterPropertiesSet(); //ItemReader에서 필요한 필수 설정 값이 정상적으로 설정이 되었는 지 검증.

        return itemReader;
    }

    private ItemWriter<Person> itemWriter() {
        return list -> {
            for (Person person: list) {
                log.info("Current Person={}", person);
            }
        };
    }
}

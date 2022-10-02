package com.eastshine.batch.task.membership;

import com.eastshine.batch.task.domain.Member;
import com.eastshine.batch.task.domain.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.persistence.EntityManagerFactory;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MemberLevelConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final MemberRepository memberRepository;
    private final EntityManagerFactory entityManagerFactory;

    private final String JOB_NAME = "memberLevelUpJob";
    private int chunkSize = 10;

    @Bean
    public Job memberLevelUpJob() {
        return jobBuilderFactory.get("memberLevelUpJob")
                .incrementer(new RunIdIncrementer())
                //.start(saveMemberStep())
                .start(this.memberLevelUpStep())
                .listener(new MemberLevelJobExecutionListener())
                .build();
    }

    public Step saveMemberStep() {
        return stepBuilderFactory.get("saveMemberStep")
                .tasklet(new SaveMemberTasklet(memberRepository))
                .build();
    }

    @Bean
    public Step memberLevelUpStep() {
        return stepBuilderFactory.get("memberLevelUpStep")
                .<Member, Member>chunk(10)
                .reader(this.memberLevelUpReader())
                .processor(this.memberLevelUpProcessor())
                .writer(this.memberLevelUpWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Member> memberLevelUpReader() {
        return new JpaPagingItemReaderBuilder<Member>()
                .name("jpaPagingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(chunkSize)
                .queryString("SELECT m FROM Member m")
                .build();
    }

    @Bean
    public ItemProcessor<Member, Member> memberLevelUpProcessor() {
        return member -> {
            if (member.notAvailableLeveUp()) {
                return null;
            }
            return member;
        };
    }

    @Bean
    public ItemWriter<? super Member> memberLevelUpWriter() {
        return members -> members.forEach(m -> {
            m.levelUp();
            memberRepository.save(m);
        });
    }

    private static class MemberLevelJobExecutionListener {

        @AfterJob
        public void afterJob(JobExecution jobExecution) {
            int sum = jobExecution.getStepExecutions().stream()
                    .mapToInt(StepExecution::getWriteCount)
                    .sum();

            log.info("----------------------------");
            log.info("annotationAfterJob : {}", sum);
        }
    }
}

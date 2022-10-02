package com.eastshine.batch.task.membership;

import com.eastshine.batch.common.TestBatchConfig;
import com.eastshine.batch.task.domain.Member;
import com.eastshine.batch.task.domain.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes={TestBatchConfig.class, MemberLevelConfiguration.class, MemberRepository.class})
@RunWith(SpringRunner.class)
@EnableJpaRepositories(basePackages = {"com.eastshine.batch.task.domain"})
@EntityScan(basePackages = {"com.eastshine.batch.task.domain"})
class MemberLevelConfigurationTest {

    @Autowired MemberRepository memberRepository;
    @Autowired JobLauncherTestUtils jobLauncherTestUtils; // 내부에 JobLauncher를 포함하고 있으며, Job과 Step을 테스트할 수 있다.
    @Autowired JpaPagingItemReader<Member> reader;
    @Autowired ItemProcessor<Member, Member> processor;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    // Reader 단위 테스트 예시
    @Test
    @DisplayName("Cf. 현재 Reader는 모든 Member를 조회한다")
    void testReader() throws Exception {
        // given
        List<Member> members = Arrays.asList(
                new Member("최동호",450000),
                new Member("김동호",250000),
                new Member("이동호",50000)
        );
        memberRepository.saveAll(members);

        // when
        reader.open(new ExecutionContext());

        //then
        Member memberRecord;
        while ((memberRecord = reader.read()) != null) {
            assertThat(memberRecord.getTotalAmount()).isNotNull();
            assertThat(memberRecord.getName()).contains("동호");
        }

        reader.close();
    }

    @Nested
    @DisplayName("memberLevelUpProcessor는")
    class Describe_memberLevelUpProcessor{

        @Nested
        @DisplayName("레벨업 대상이 아닌 회원일 경우")
        class Context_with_non_levelup_member{
            Member member = new Member("김동호", 3000);

            @Test
            @DisplayName("null을 반환한다.")
            void testProcessor() throws Exception {
                Member actual = processor.process(member);

                assertThat(actual).isNull();
            }
        }

        @Nested
        @DisplayName("레벨업 대상 회원일 경우")
        class Context_with_levelup_member{
            Member member = new Member("김동호", 250_000);

            @Test
            @DisplayName("대상 회원을 반환한다.")
            void it_returns_member() throws Exception {
                Member actual = processor.process(member);

                assertThat(actual).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("memberLevelUpStep은 레벨업 대상자의 레벨을 상향한다.")
    public void test_memberLevelUpStep() {
        Member member = new Member("김동호", 330_000, Member.Level.SILVER);
        memberRepository.save(member);

        JobExecution jobExecution = jobLauncherTestUtils.launchStep("memberLevelUpStep");

        List<Member> members = memberRepository.findAll();

        assertThat(members.get(0).getLevel())
                .isEqualTo(Member.Level.GOLD);
        assertThat(jobExecution.getStepExecutions().stream()
                        .mapToInt(StepExecution::getWriteCount)
                        .sum()
        )
                .isEqualTo(1);
    }

    @Test
    @DisplayName("memberLevelUpJob 테스트")
    public void testMemberLevelUpJob() throws Exception {
        // given
        List<Member> members = Arrays.asList(

                new Member("김동호",250000, Member.Level.SILVER),
                new Member("이동호",50000),
                new Member("박동호",350000),
                new Member("최동호",450000)
        );
        memberRepository.saveAll(members);

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getStepExecutions().stream()
                .mapToInt(StepExecution::getWriteCount)
                .sum()
        )
                .isEqualTo(2);
    }
}

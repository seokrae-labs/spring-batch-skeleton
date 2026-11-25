package com.example.batch.job.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import com.example.batch.core.StepTransactionRules;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringBatchTest
@SpringJUnitConfig(SampleJobGroupBootstrap.class)
@TestPropertySource(properties = {
    "job-sample.base-path=build/test-input",
    "job-sample.chunk-size=2"
})
class SampleJobTest {
  private static final Path BASE_PATH = Path.of("build/test-input");

  @Autowired
  Job sampleJob;

  @Autowired
  JobOperatorTestUtils testUtils;

  @Autowired
  @Qualifier("mainDataSource")
  DataSource mainDataSource;

  @Test
  @DisplayName("이 그룹의 모든 스텝은 업무 DB 트랜잭션 안에서 실행된다")
  void everyStepRunsInBusinessTransaction(@Autowired ApplicationContext context) {
    StepTransactionRules.assertStepsUseTransactionManager(context, "mainTransactionManager", Set.of());
  }

  @Test
  @DisplayName("대상일 파일을 읽어 금액이 양수인 행만 업무 DB에 쓴다")
  void loadsPositiveAmountsOnly() throws Exception {
    LocalDate targetDate = LocalDate.of(2026, 1, 1);
    writeInput(targetDate, "a,100", "b,0", "c,300", "d,-5", "e,500");

    JobExecution execution = testUtils.startJob(paramsOf(targetDate));

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(itemKeys(targetDate)).containsExactly("a", "c", "e");
  }

  @Test
  @DisplayName("실패한 실행을 같은 파라미터로 재시작하면 누락과 중복 없이 다시 적재한다")
  void restartReloadsWithoutGapOrDuplicate() throws Exception {
    LocalDate targetDate = LocalDate.of(2026, 1, 2);
    writeInput(targetDate, "a,1", "b,2", "c,3", "d,not-a-number", "e,5");
    JobParameters params = paramsOf(targetDate);

    JobExecution first = testUtils.startJob(params);

    assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
    assertThat(itemKeys(targetDate)).containsExactly("a", "b");

    writeInput(targetDate, "a,1", "b,2", "c,3", "d,4", "e,5");
    JobExecution restarted = testUtils.startJob(params);

    assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(restarted.getJobInstance().getId()).isEqualTo(first.getJobInstance().getId());
    assertThat(itemKeys(targetDate)).containsExactly("a", "b", "c", "d", "e");
  }

  @Test
  @DisplayName("대상일 파라미터가 없으면 잡을 시작하지 않는다")
  void requiresTargetDate() {
    testUtils.setJob(sampleJob);

    assertThatThrownBy(() -> testUtils.startJob(new JobParameters()))
        .isInstanceOf(InvalidJobParametersException.class);
  }

  private JobParameters paramsOf(LocalDate targetDate) {
    testUtils.setJob(sampleJob);
    return testUtils.getUniqueJobParametersBuilder()
        .addLocalDate(SampleJobConfig.TARGET_DATE, targetDate)
        .toJobParameters();
  }

  private void writeInput(LocalDate targetDate, String... lines) throws IOException {
    Files.createDirectories(BASE_PATH);
    Files.write(BASE_PATH.resolve(targetDate + ".csv"), List.of(lines), StandardCharsets.UTF_8);
  }

  private List<String> itemKeys(LocalDate targetDate) {
    return new JdbcTemplate(mainDataSource).queryForList(
        "SELECT item_key FROM sample_record WHERE target_date = ? ORDER BY item_key",
        String.class, targetDate);
  }
}

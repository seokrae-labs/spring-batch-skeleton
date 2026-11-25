package com.example.batch.admincli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

class JobLaunchingRunnerTest {

  @Test
  @DisplayName("잡 이름이 없으면 잡을 실행하지 않고 종료 코드 2를 돌려준다")
  void missingJobNameExitsWithNoSuchJob() {
    var runner = new JobLaunchingRunner(null, "");

    runner.run(new DefaultApplicationArguments("targetDate=2026-11-25,java.time.LocalDate"));

    assertThat(runner.getExitCode()).isEqualTo(JobLaunchingRunner.EXIT_NO_SUCH_JOB);
  }

  @Test
  @DisplayName("name=value 인자를 잡 파라미터로 바꾸고 값 안의 등호와 쉼표는 그대로 둔다")
  void convertsArgumentsToProperties() {
    Properties properties = JobLaunchingRunner.toProperties(
        List.of("targetDate=2026-11-25,java.time.LocalDate", "query=a=b"));

    assertThat(properties)
        .containsEntry("targetDate", "2026-11-25,java.time.LocalDate")
        .containsEntry("query", "a=b");
  }

  @Test
  @DisplayName("등호가 없거나 이름이 빈 인자는 거부한다")
  void rejectsMalformedArguments() {
    assertThatThrownBy(() -> JobLaunchingRunner.toProperties(List.of("targetDate")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> JobLaunchingRunner.toProperties(List.of("=value")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

package com.example.batch.job.sample;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class ClearTargetDateTasklet implements Tasklet {
  public static final String DELETED_COUNT = "deletedCount";

  private final NamedParameterJdbcTemplate jdbc;
  private final LocalDate targetDate;

  public ClearTargetDateTasklet(NamedParameterJdbcTemplate jdbc, LocalDate targetDate) {
    this.jdbc = jdbc;
    this.targetDate = targetDate;
  }

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    int deleted = jdbc.update(SampleSql.DELETE_BY_TARGET_DATE, Map.of("targetDate", targetDate));
    chunkContext.getStepContext().getStepExecution().getExecutionContext().putInt(DELETED_COUNT, deleted);
    return RepeatStatus.FINISHED;
  }
}

package com.example.batch.sample;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@PropertySource("classpath:/sample-job.properties")
public class SampleJobConfig {
  public static final String JOB_NAME = "sampleJob";
  public static final String TARGET_DATE = "targetDate";

  private final JobRepository jobRepository;
  private final DataSource mainDataSource;
  private final PlatformTransactionManager mainTransactionManager;
  private final Path basePath;
  private final int chunkSize;

  public SampleJobConfig(
      JobRepository jobRepository,
      @Qualifier("mainDataSource") DataSource mainDataSource,
      @Qualifier("mainTransactionManager") PlatformTransactionManager mainTransactionManager,
      @Value("${sample.base-path}") Path basePath,
      @Value("${sample.chunk-size}") int chunkSize) {
    this.jobRepository = jobRepository;
    this.mainDataSource = mainDataSource;
    this.mainTransactionManager = mainTransactionManager;
    this.basePath = basePath;
    this.chunkSize = chunkSize;
  }

  @Bean
  public Job sampleJob() {
    return new JobBuilder(JOB_NAME, jobRepository)
        .validator(new DefaultJobParametersValidator(new String[] {TARGET_DATE}, new String[0]))
        .start(clearTargetDateStep())
        .next(loadSampleRecordStep())
        .build();
  }

  @Bean
  public Step clearTargetDateStep() {
    return new StepBuilder("clearTargetDateStep", jobRepository)
        .allowStartIfComplete(true)
        .tasklet(clearTargetDateTasklet(null), mainTransactionManager)
        .build();
  }

  @Bean
  @StepScope
  public ClearTargetDateTasklet clearTargetDateTasklet(
      @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
    return new ClearTargetDateTasklet(new NamedParameterJdbcTemplate(mainDataSource), targetDate);
  }

  @Bean
  public Step loadSampleRecordStep() {
    return new StepBuilder("loadSampleRecordStep", jobRepository)
        .<SampleRecord, SampleRecord>chunk(chunkSize)
        .transactionManager(mainTransactionManager)
        .reader(sampleRecordReader(null))
        .processor(new SampleProcessor())
        .writer(buildSampleRecordWriter())
        .build();
  }

  @Bean
  @StepScope
  public FlatFileItemReader<SampleRecord> sampleRecordReader(
      @Value("#{jobParameters['targetDate']}") LocalDate targetDate) {
    return new FlatFileItemReaderBuilder<SampleRecord>()
        .name("sampleRecordReader")
        .resource(new FileSystemResource(basePath.resolve(targetDate + ".csv")))
        .encoding(StandardCharsets.UTF_8.name())
        .saveState(false)
        .lineTokenizer(new DelimitedLineTokenizer())
        .fieldSetMapper(new SampleFieldSetMapper(targetDate))
        .build();
  }

  JdbcBatchItemWriter<SampleRecord> buildSampleRecordWriter() {
    return new JdbcBatchItemWriterBuilder<SampleRecord>()
        .dataSource(mainDataSource)
        .sql(SampleSql.INSERT)
        .beanMapped()
        .build();
  }
}

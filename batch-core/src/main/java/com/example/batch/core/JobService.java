package com.example.batch.core;

import java.util.Properties;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.launch.JobOperator;

public class JobService {
  private final JobRegistry jobRegistry;
  private final JobOperator jobOperator;
  private final JobParametersConverter parametersConverter = new DefaultJobParametersConverter();

  public JobService(JobRegistry jobRegistry, JobOperator jobOperator) {
    this.jobRegistry = jobRegistry;
    this.jobOperator = jobOperator;
  }

  public JobRunResult run(String jobName, Properties parameters) {
    Job job = jobRegistry.getJob(jobName);
    if (job == null) {
      throw new UnknownJobException(jobName, jobRegistry.getJobNames());
    }
    try {
      JobExecution execution = jobOperator.start(job, parametersConverter.getJobParameters(parameters));
      return new JobRunResult(execution.getId(), execution.getExitStatus().getExitCode());
    } catch (JobExecutionException ex) {
      throw new IllegalStateException("Job '" + jobName + "' could not be started", ex);
    }
  }
}

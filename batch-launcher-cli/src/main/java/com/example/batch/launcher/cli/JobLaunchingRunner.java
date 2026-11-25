package com.example.batch.launcher.cli;

import com.example.batch.core.JobRunResult;
import com.example.batch.core.JobService;
import com.example.batch.core.UnknownJobException;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

@Component
public class JobLaunchingRunner implements ApplicationRunner, ExitCodeGenerator {
  static final int EXIT_COMPLETED = 0;
  static final int EXIT_NOT_COMPLETED = 1;
  static final int EXIT_NO_SUCH_JOB = 2;

  private static final Logger log = LoggerFactory.getLogger(JobLaunchingRunner.class);

  private final JobService jobService;
  private final String jobName;
  private int exitCode = EXIT_NOT_COMPLETED;

  public JobLaunchingRunner(JobService jobService, @Value("${job.name:}") String jobName) {
    this.jobService = jobService;
    this.jobName = jobName;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (jobName.isBlank()) {
      log.error("No job name given. Pass it as --job.name=<name> or -Djob.name=<name>.");
      exitCode = EXIT_NO_SUCH_JOB;
      return;
    }
    try {
      JobRunResult result = jobService.run(jobName, toProperties(args.getNonOptionArgs()));
      exitCode = result.completed() ? EXIT_COMPLETED : EXIT_NOT_COMPLETED;
    } catch (UnknownJobException ex) {
      log.error(ex.getMessage());
      exitCode = EXIT_NO_SUCH_JOB;
    }
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }

  static Properties toProperties(List<String> keyValues) {
    var properties = new Properties();
    for (String keyValue : keyValues) {
      int separator = keyValue.indexOf('=');
      if (separator <= 0) {
        throw new IllegalArgumentException("Job parameter must be name=value: " + keyValue);
      }
      properties.setProperty(keyValue.substring(0, separator), keyValue.substring(separator + 1));
    }
    return properties;
  }
}

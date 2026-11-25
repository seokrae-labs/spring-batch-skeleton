package com.example.batch.launcher.cli;

import com.example.batch.job.sample.SampleJobGroup;
import com.example.batch.core.BatchConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({BatchConfig.class, SampleJobGroup.class})
public class CliLauncherApplication {

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(CliLauncherApplication.class, args)));
  }
}

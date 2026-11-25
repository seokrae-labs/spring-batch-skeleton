package com.example.batch.admincli;

import com.example.batch.sample.SampleJobGroup;
import com.example.batch.support.BatchConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({BatchConfig.class, SampleJobGroup.class})
public class AdminCliApplication {

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(AdminCliApplication.class, args)));
  }
}

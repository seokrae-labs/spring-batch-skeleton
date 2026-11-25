package com.example.batch.core;

import org.springframework.batch.core.ExitStatus;

public record JobRunResult(long executionId, String exitCode) {

  public boolean completed() {
    return ExitStatus.COMPLETED.getExitCode().equals(exitCode);
  }
}

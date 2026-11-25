package com.example.batch.support;

import java.util.Collection;
import java.util.TreeSet;

public class UnknownJobException extends IllegalArgumentException {

  public UnknownJobException(String jobName, Collection<String> knownJobNames) {
    super("Unknown job '" + jobName + "'. Registered jobs: " + new TreeSet<>(knownJobNames));
  }
}

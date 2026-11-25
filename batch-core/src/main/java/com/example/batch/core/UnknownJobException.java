package com.example.batch.core;

import java.util.Collection;
import java.util.TreeSet;

public class UnknownJobException extends IllegalArgumentException {

  public UnknownJobException(String jobName, Collection<String> knownJobNames) {
    super("Unknown job '" + jobName + "'. Registered jobs: " + new TreeSet<>(knownJobNames));
  }
}

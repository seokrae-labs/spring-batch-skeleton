package com.example.batch.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.ListableStepLocator;
import org.springframework.batch.core.step.Step;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ReflectionUtils;

public final class StepTransactionRules {

  private StepTransactionRules() {
  }

  public static void assertStepsUseTransactionManager(
      ApplicationContext context, String transactionManagerBeanName, Set<String> exemptStepNames) {
    PlatformTransactionManager expected =
        context.getBean(transactionManagerBeanName, PlatformTransactionManager.class);
    Map<String, Job> jobs = context.getBeansOfType(Job.class);
    if (jobs.isEmpty()) {
      throw new AssertionError("No Job beans found in the context");
    }

    List<String> problems = new ArrayList<>();
    for (Job job : jobs.values()) {
      if (!(job instanceof ListableStepLocator locator)) {
        problems.add(job.getName() + ": steps cannot be listed (" + job.getClass().getName() + ")");
        continue;
      }
      for (String stepName : locator.getStepNames()) {
        if (exemptStepNames.contains(stepName)) {
          continue;
        }
        Object actual = readTransactionManager(locator.getStep(stepName));
        if (actual != expected) {
          problems.add(job.getName() + "." + stepName + " uses " + describe(actual)
              + " instead of " + transactionManagerBeanName);
        }
      }
    }

    if (!problems.isEmpty()) {
      throw new AssertionError("Steps must run in the business database transaction:\n  "
          + String.join("\n  ", problems)
          + "\nSet .transactionManager(...) on the step, or exempt a step that does not write to the database.");
    }
  }

  private static Object readTransactionManager(Step step) {
    Field field = ReflectionUtils.findField(step.getClass(), "transactionManager");
    if (field == null) {
      throw new AssertionError("Cannot read the transaction manager of step '" + step.getName() + "' ("
          + step.getClass().getName() + "). Exempt this step or update StepTransactionRules"
          + " if Spring Batch changed its internals.");
    }
    ReflectionUtils.makeAccessible(field);
    return ReflectionUtils.getField(field, step);
  }

  private static String describe(Object transactionManager) {
    return transactionManager == null ? "no transaction manager" : transactionManager.getClass().getSimpleName();
  }
}

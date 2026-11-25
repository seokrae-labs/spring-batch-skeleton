package com.example.batch.sample;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.infrastructure.item.ItemProcessor;

public class SampleProcessor implements ItemProcessor<SampleRecord, SampleRecord> {

  @Override
  public @Nullable SampleRecord process(SampleRecord item) {
    if (item.amount() <= 0) {
      return null;
    }
    return item;
  }
}

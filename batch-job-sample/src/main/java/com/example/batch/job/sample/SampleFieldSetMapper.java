package com.example.batch.job.sample;

import java.time.LocalDate;
import org.springframework.batch.infrastructure.item.file.mapping.FieldSetMapper;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

public class SampleFieldSetMapper implements FieldSetMapper<SampleRecord> {
  private final LocalDate targetDate;

  public SampleFieldSetMapper(LocalDate targetDate) {
    this.targetDate = targetDate;
  }

  @Override
  public SampleRecord mapFieldSet(FieldSet fieldSet) {
    return new SampleRecord(targetDate, fieldSet.readString(0), fieldSet.readLong(1));
  }
}

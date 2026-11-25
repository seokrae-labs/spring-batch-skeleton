package com.example.batch.job.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

class SampleFieldSetMapperTest {
  private static final LocalDate TARGET_DATE = LocalDate.of(2026, 11, 25);

  private final DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
  private final SampleFieldSetMapper mapper = new SampleFieldSetMapper(TARGET_DATE);

  @Test
  @DisplayName("한 줄의 키와 금액을 대상일이 붙은 SampleRecord로 바꾼다")
  void mapsLine() {
    FieldSet fieldSet = tokenizer.tokenize("item-한글,1200");

    SampleRecord record = mapper.mapFieldSet(fieldSet);

    assertThat(record).isEqualTo(new SampleRecord(TARGET_DATE, "item-한글", 1200L));
  }

  @Test
  @DisplayName("금액이 숫자가 아니면 예외를 던진다")
  void rejectsNonNumericAmount() {
    FieldSet fieldSet = tokenizer.tokenize("item-1,abc");

    assertThatThrownBy(() -> mapper.mapFieldSet(fieldSet))
        .isInstanceOf(NumberFormatException.class);
  }
}

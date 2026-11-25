package com.example.batch.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SampleProcessorTest {
  private final SampleProcessor processor = new SampleProcessor();

  @ParameterizedTest(name = "amount={0} -> kept={1}")
  @CsvSource({"-1, false", "0, false", "1, true"})
  @DisplayName("금액이 0 이하인 아이템은 걸러내고 양수만 남긴다")
  void filtersNonPositiveAmount(long amount, boolean kept) {
    var item = new SampleRecord(LocalDate.of(2026, 11, 25), "item", amount);

    SampleRecord result = processor.process(item);

    assertThat(result != null).isEqualTo(kept);
  }
}

package com.example.batch.sample;

import java.time.LocalDate;

public record SampleRecord(LocalDate targetDate, String itemKey, long amount) {
}

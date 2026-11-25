package com.example.batch.job.sample;

import java.time.LocalDate;

public record SampleRecord(LocalDate targetDate, String itemKey, long amount) {
}

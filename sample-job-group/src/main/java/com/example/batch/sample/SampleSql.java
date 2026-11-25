package com.example.batch.sample;

public final class SampleSql {
  public static final String INSERT = """
      INSERT INTO sample_record (target_date, item_key, amount)
      VALUES (:targetDate, :itemKey, :amount)
      """;

  public static final String DELETE_BY_TARGET_DATE = """
      DELETE FROM sample_record
      WHERE target_date = :targetDate
      """;

  private SampleSql() {
  }
}

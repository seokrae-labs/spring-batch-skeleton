package com.example.batch.core;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing(transactionManagerRef = "jobMetaTransactionManager")
@EnableJdbcJobRepository(
    dataSourceRef = "jobMetaDataSource",
    transactionManagerRef = "jobMetaTransactionManager"
)
@PropertySource("classpath:/core.properties")
@Import(JobService.class)
public class BatchConfig {

  @Bean
  public DataSource jobMetaDataSource(Environment env) {
    var dataSource = new HikariDataSource();
    dataSource.setPoolName("job-meta");
    dataSource.setJdbcUrl(env.getRequiredProperty("core.jdbc-url"));
    dataSource.setUsername(env.getRequiredProperty("core.username"));
    dataSource.setPassword(env.getProperty("core.password", ""));
    return dataSource;
  }

  @Bean
  public PlatformTransactionManager jobMetaTransactionManager(
      @Qualifier("jobMetaDataSource") DataSource jobMetaDataSource) {
    return new JdbcTransactionManager(jobMetaDataSource);
  }

  @Bean
  public JobRegistry jobRegistry() {
    return new MapJobRegistry();
  }

  @Bean
  public DataSourceInitializer jobMetaSchemaInitializer(
      @Qualifier("jobMetaDataSource") DataSource jobMetaDataSource, Environment env) {
    var schema = new DefaultResourceLoader().getResource(env.getRequiredProperty("core.schema-script"));
    var initializer = new DataSourceInitializer();
    initializer.setDataSource(jobMetaDataSource);
    initializer.setDatabasePopulator(new ResourceDatabasePopulator(schema));
    initializer.setEnabled(env.getProperty("core.initialize-schema", Boolean.class, false));
    return initializer;
  }
}

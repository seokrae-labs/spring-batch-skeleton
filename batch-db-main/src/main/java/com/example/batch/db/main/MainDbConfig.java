package com.example.batch.db.main;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@PropertySource("classpath:/db-main.properties")
public class MainDbConfig {

  @Bean
  public DataSource mainDataSource(Environment env) {
    var dataSource = new HikariDataSource();
    dataSource.setPoolName("main");
    dataSource.setJdbcUrl(env.getRequiredProperty("db-main.jdbc-url"));
    dataSource.setUsername(env.getRequiredProperty("db-main.username"));
    dataSource.setPassword(env.getProperty("db-main.password", ""));
    return dataSource;
  }

  @Bean
  public PlatformTransactionManager mainTransactionManager(
      @Qualifier("mainDataSource") DataSource mainDataSource) {
    return new JdbcTransactionManager(mainDataSource);
  }

  @Bean
  public DataSourceInitializer mainSchemaInitializer(
      @Qualifier("mainDataSource") DataSource mainDataSource, Environment env) {
    var schema = new DefaultResourceLoader().getResource(env.getRequiredProperty("db-main.schema-script"));
    var initializer = new DataSourceInitializer();
    initializer.setDataSource(mainDataSource);
    initializer.setDatabasePopulator(new ResourceDatabasePopulator(schema));
    initializer.setEnabled(env.getProperty("db-main.initialize-schema", Boolean.class, false));
    return initializer;
  }
}

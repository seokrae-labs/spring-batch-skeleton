package com.example.batch.maindb;

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
@PropertySource("classpath:/main-db.properties")
public class MainDbConfig {

  @Bean
  public DataSource mainDataSource(Environment env) {
    var dataSource = new HikariDataSource();
    dataSource.setPoolName("main");
    dataSource.setJdbcUrl(env.getRequiredProperty("main-db.jdbc-url"));
    dataSource.setUsername(env.getRequiredProperty("main-db.username"));
    dataSource.setPassword(env.getProperty("main-db.password", ""));
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
    var schema = new DefaultResourceLoader().getResource(env.getRequiredProperty("main-db.schema-script"));
    var initializer = new DataSourceInitializer();
    initializer.setDataSource(mainDataSource);
    initializer.setDatabasePopulator(new ResourceDatabasePopulator(schema));
    initializer.setEnabled(env.getProperty("main-db.initialize-schema", Boolean.class, false));
    return initializer;
  }
}

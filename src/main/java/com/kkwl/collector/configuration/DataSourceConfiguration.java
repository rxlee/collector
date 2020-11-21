  package  com.kkwl.collector.configuration;
  
  import javax.sql.DataSource;
  import org.springframework.beans.factory.annotation.Qualifier;
  import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
  import org.springframework.boot.context.properties.ConfigurationProperties;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.context.annotation.Primary;
  import org.springframework.jdbc.core.JdbcTemplate;
  
  
  @Configuration
  public class DataSourceConfiguration
  {
    @Bean(name = {"mysqlDataSource"})
    @Qualifier("mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() { return DataSourceBuilder.create().build(); }
    ///zth add
    @Bean(name = {"cloudDataSource"})
    @Qualifier("cloudDataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource.cloud")
    public DataSource cloudDataSource() { return DataSourceBuilder.create().build(); }
  
    
    @Bean(name = {"h2DataSource"})
    @Qualifier("h2DataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.h2")
    public DataSource h2DataSource() { return DataSourceBuilder.create().build(); }

    @Bean(name = {"mysqlJdbcTemplate"})
    public JdbcTemplate mysqlJdbcTemplate(@Qualifier("mysqlDataSource") DataSource dataSource) { return new JdbcTemplate(dataSource); }
//   ///zth add
//    @Primary
//    @Bean(name = {"cloudJdbcTemplate"})
//    public JdbcTemplate cloudJdbcTemplate(@Qualifier("cloudDataSource") DataSource dataSource) { return new JdbcTemplate(dataSource); }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\DataSourceConfiguration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
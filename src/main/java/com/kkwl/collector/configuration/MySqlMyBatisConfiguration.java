  package  com.kkwl.collector.configuration;
  
  import com.kkwl.collector.configuration.MySqlMyBatisConfiguration;
  import javax.sql.DataSource;
  import org.apache.ibatis.session.SqlSessionFactory;
  import org.mybatis.spring.SqlSessionFactoryBean;
  import org.mybatis.spring.SqlSessionTemplate;
  import org.mybatis.spring.annotation.MapperScan;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Qualifier;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import org.springframework.jdbc.datasource.DataSourceTransactionManager;
  import org.springframework.transaction.annotation.EnableTransactionManagement;
  
  @Configuration
  @EnableTransactionManagement
  @MapperScan(basePackages = {"com.kkwl.collector.dao"}, sqlSessionFactoryRef = "mysqlSqlSessionFactory")
  public class MySqlMyBatisConfiguration {
    @Autowired
    @Qualifier("mysqlDataSource")
    private DataSource mysqlDataSource;
    
    @Bean({"mysqlSqlSessionFactory"})
    public SqlSessionFactory mysqlSqlSessionFactory() throws Exception {
      SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
      factoryBean.setDataSource(this.mysqlDataSource);
      
      return factoryBean.getObject();
    }
  
    
    @Bean({"mysqlSqlSessionTemplate"})
    public SqlSessionTemplate mysqlSqlSessionTemplate() throws Exception { return new SqlSessionTemplate(mysqlSqlSessionFactory()); }
  
  
  
    
    @Bean({"mysqlTransactionManager"})
    DataSourceTransactionManager mysqlTransactionManager(@Qualifier("mysqlDataSource") DataSource dataSource) { return new DataSourceTransactionManager(dataSource); }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\MySqlMyBatisConfiguration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
  package  com.kkwl.collector.configuration;
  
  import com.kkwl.collector.configuration.SwaggerConfiguration;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;
  import springfox.documentation.builders.ApiInfoBuilder;
  import springfox.documentation.builders.PathSelectors;
  import springfox.documentation.builders.RequestHandlerSelectors;
  import springfox.documentation.service.ApiInfo;
  import springfox.documentation.spi.DocumentationType;
  import springfox.documentation.spring.web.plugins.Docket;
  import springfox.documentation.swagger2.annotations.EnableSwagger2;
  
  @Configuration
  @EnableSwagger2
  public class SwaggerConfiguration {
    @Value("${swagger.version}")
    private String version;
    
    @Bean
    public Docket createRestApi() { return (new Docket(DocumentationType.SWAGGER_2))
        .select()
        .apis(RequestHandlerSelectors.basePackage("com.kkwl.collector.api"))
        .paths(PathSelectors.any())
        .build()
        .apiInfo(apiInfo()); }
  
  
  
    
    private ApiInfo apiInfo() { return (new ApiInfoBuilder()).title("采集器控制API")
        .description("采集器控制API")
        .license("The Apache License, Version 2.0")
        .licenseUrl("http://www.apache.org/licenses/LICENSE-2.0.html")
        .termsOfServiceUrl("http://my.csdn.net/elvishehai")
        .version(this.version).build(); }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\configuration\SwaggerConfiguration.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
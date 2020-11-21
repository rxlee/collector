package com.kkwl.collector.init;

import com.kkwl.collector.common.GlobalVariables;
import org.mapstruct.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlarmComponent implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(BusinessInitComponent.class);
    @Resource(name="cloudDataSource")
    private JdbcTemplate jdbcTemplate;
    String sql = "SELECT ";



    public void run(String... strings) throws Exception {
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleAtFixedRate(new Runnable(){
            public void run() {
                try{

                }catch (Exception e){
                    logger.error("Global configuration call alarm error.", e);
                }
            }
        },200L,200L, TimeUnit.MILLISECONDS);
    }
}

  package  com.kkwl.collector.init;
  
  import com.kkwl.collector.channels.netty.NettyClient;
  import com.kkwl.collector.common.AccessType;
  import com.kkwl.collector.common.GlobalVariables;
  import com.kkwl.collector.dao.Configuration;
  import com.kkwl.collector.init.NettyComponent;
  import com.kkwl.collector.models.CollectorDtu;
  import com.kkwl.collector.services.DataLoadService;
  import java.util.HashMap;
  import java.util.List;
  import java.util.Map;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.CommandLineRunner;
  import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
  import org.springframework.core.annotation.Order;
  import org.springframework.stereotype.Component;
//@Order标记定义了组件的加载顺序。
  @Component
  @ConditionalOnProperty(name = {"com.kkwl.collector.runmode"}, havingValue = "tcp_server")//根据特定条件来控制Bean的创建行为
  @Order(3)//1.GlobalVariablesInitComponent 3. KafkaComponent,MqttComponent 4. BussinessInitComponent 5. DataTransferComponent
  public class NettyComponent implements CommandLineRunner {//项目启动后执行
    private static final Logger logger = LoggerFactory.getLogger(NettyComponent.class);
    //netty绑定的服务器ip
    @Value("${netty.bind.ip_address}")
    private String ipAddress;
    //netty绑定的公用服务ip
    @Value("${netty.bind.public_ip_address}")
    private String publicIpAddress;
    //netty监听的端口，某个设备向这个地址（publicIpAddress + port）发送数据，netty监听到后做数据的处理
    @Value("${netty.bind.port}")
    private String ports;
    //重连时长
    @Value("${netty.client.reconnect-duration}")
    private Integer reconnectDuration;
    
    @Autowired
    private Configuration configuration;
    //数据加载
    @Autowired
    private DataLoadService dataLoadService;
  
    
    public void run(String... args) throws Exception {
      logger.info("Global configuration begin to initialize netty server.");
      GlobalVariables.IS_SERVICE_RUNNING = true;
  
      
      String[] portArr = this.ports.split(",");
      //遍历监听接口2404-2405-2406-2407-2408-2409，得到通道、加载设备，nettyServer处理数据
      for (int i = 0; i < portArr.length; i++) {
        String tempPort = portArr[i];
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("ip_address", this.publicIpAddress);
        filter.put("port", tempPort);
        //根据ip，port获取所有通道  通道连接的是哪个地址
        //monitor_channel.collector_id =  monitor_server.id
        List<CollectorDtu> tempDtus = this.configuration.getDtus(filter); //**************初始化查库 通过映射过滤查询数据库得到所有通道*******************
          if (tempDtus.isEmpty()) {//2405-2409 5个端口没有绑定通道, 2404除外
            logger.warn("Data load service dtu size = 0");
          }
          else {
                try {//填充数据（属性等）从数据库映射
                   this.dataLoadService.loadData(tempDtus); //***********初始化 根据查库通道加载数据,绑定的都是2404端口
                   } catch (Exception e) {
                     logger.error("Netty server error occured when loading data.", e);
                    }
            }
        //对每个端口，nettyServer处理数据：（激活通道、读通道类容。。。）
        GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
          {
            public void run() {//拨号连接时系统属netty服务端
              GlobalVariables.NETTY_SERVER.start(NettyComponent.this.publicIpAddress, NettyComponent.this.ipAddress, tempPort); //publicIpAddress没有使用到

            }
          });
      } 

      Map<String, Object> filters = new HashMap<String, Object>();
      filters.put("access_type", AccessType.TCPCLIENT.value());  //此时为tcpClient客户端方式，需要设备的ip和port,如果没有连接则报错
      List<CollectorDtu> collectorDtus = this.configuration.getDtus(filters);
      if (collectorDtus.isEmpty()) {
        logger.warn("测试tcp----------------Global configuration get tcp client count = 0");
        return;
      }
      try {
        this.dataLoadService.loadData(collectorDtus);
      } catch (Exception e) {
        logger.error("Netty server error occured when loading data.", e);
        return;
      }
      //如果有tcpClient，建立nettyClient
      for (CollectorDtu collectorDtu : collectorDtus) {
        logger.info("Global configuration begin connecting to server ip = " + collectorDtu.getIpAddress() + " and port = " + collectorDtu.getPort());
        //客户端连接时系统属netty客户端
        NettyClient nettyClient = new NettyClient(collectorDtu.getSn(), collectorDtu.getIpAddress(), collectorDtu.getPort().intValue(), this.reconnectDuration.intValue());
        nettyClient.start();
        GlobalVariables.NETTY_CLIENT_LIST.add(nettyClient);
      } 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\init\NettyComponent.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
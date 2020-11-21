  package  com.kkwl.collector.channels.netty;
  
  import com.kkwl.collector.channels.netty.NettyServer;
  import com.kkwl.collector.channels.netty.NettyServerInitializer;
  import io.netty.bootstrap.ServerBootstrap;
  import io.netty.channel.ChannelFuture;
  import io.netty.channel.ChannelOption;
  import io.netty.channel.EventLoopGroup;
  import io.netty.channel.nio.NioEventLoopGroup;
  import io.netty.handler.logging.LogLevel;
  import io.netty.handler.logging.LoggingHandler;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.context.annotation.Bean;
  import org.springframework.stereotype.Component;
  
  @Component
  public class NettyServer
  {
  /*
	 * NettyServerInitializer 在客户端成功连接时：
	 * 		接收到连接后，创建服务端和客户端之间的通道ChannelInitializer
	 * 		在管道末端加上处理器   当监听到有数据的传输，将会由处理器对数据进行处理
	 */
    @Autowired
    private NettyServerInitializer nettyServerInitializer;
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
  
  //输出连接日志
    @Bean
    public LoggingHandler createLoggingHandler() { return new LoggingHandler(LogLevel.INFO); }
  
  //连接线程池，负责接收线程并创建通道
    private EventLoopGroup bossGroup = null;
 // 数据处理线程池，负责处理数据，类似于工人 
    private EventLoopGroup workerGroup = null;
    
    public void start(String publicIpAddress, String ipAddress, String port) {
      Integer portNum = Integer.valueOf(Integer.parseInt(port));
      this.bossGroup = new NioEventLoopGroup();
      this.workerGroup = new NioEventLoopGroup();
      try {
        ServerBootstrap b = new ServerBootstrap();
        ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)b.group(this.bossGroup, this.workerGroup)
          .channel(io.netty.channel.socket.nio.NioServerSocketChannel.class))
          .option(ChannelOption.SO_BACKLOG, Integer.valueOf(100)))
          .handler(createLoggingHandler()))//初始化的时候执行
          .childHandler(this.nettyServerInitializer);//************当数据传输时，处理数据************
        
        ChannelFuture f = b.bind(ipAddress, portNum.intValue()).sync();

        f.channel().closeFuture().sync();
        
        logger.debug("Netty server start ok : " + ipAddress + " : " + port);
      } catch (InterruptedException ex) {
        logger.error("Netty server error occured when start netty server:", ex);
      } finally {
        
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
      } 
    }
    
    public void stop() {
      if (this.bossGroup != null && this.workerGroup != null) {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
      } 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\NettyServer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
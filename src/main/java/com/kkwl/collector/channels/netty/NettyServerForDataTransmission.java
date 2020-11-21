 package  com.kkwl.collector.channels.netty;
 
 import com.kkwl.collector.channels.netty.NettyServerForDataTransmission;
import com.kkwl.collector.channels.netty.handler.DataTransferSocketHandler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
 
 
 @Component
 public class NettyServerForDataTransmission
 {
    private static final Logger logger = LoggerFactory.getLogger(NettyServerForDataTransmission.class);
   //数据传输处理器
   @Autowired
   private DataTransferSocketHandler dataTransferSocketHandler;
 
   //日志
   @Bean
    public LoggingHandler createLoggingHandler() { return new LoggingHandler(LogLevel.INFO); }
 
   
    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;
   
   public void start(String ipAddress, String port) {
      Integer portNum = Integer.valueOf(Integer.parseInt(port));
      //第1步定义两个线程组，用来处理客户端通道的accept和读写事件
      //parentGroup用来处理accept事件，childgroup用来处理通道的读写事件
      //parentGroup获取客户端连接，连接接收到之后再将连接转发给childgroup去处理
      this.bossGroup = new NioEventLoopGroup();
      this.workerGroup = new NioEventLoopGroup();
     try {
        ServerBootstrap b = new ServerBootstrap();
        ((ServerBootstrap)((ServerBootstrap)((ServerBootstrap)b.group(this.bossGroup, this.workerGroup)
          .channel(io.netty.channel.socket.nio.NioServerSocketChannel.class))//第2步绑定服务端通道
          .option(ChannelOption.SO_BACKLOG, Integer.valueOf(100)))
          .handler(createLoggingHandler()))//发生在初始化的时候
          .childHandler(new ChannelInitializer<SocketChannel>()//第3步绑定handler，处理读写事件，ChannelInitializer是给通道初始化
                  {
              protected void initChannel(SocketChannel socketChannel) throws Exception {
                ChannelPipeline pipeline = socketChannel.pipeline();
                //pipeline.addLast(new ChannelHandler[] { NettyServerForDataTransmission.access$000(NettyServerForDataTransmission.this) });
                pipeline.addLast(new ChannelHandler[] { dataTransferSocketHandler });//数据传输处理器
              }
            });
        //负责绑定端口，当这个方法执行后，ServerBootstrap就可以接受指定端口上的socket连接
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


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\NettyServerForDataTransmission.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
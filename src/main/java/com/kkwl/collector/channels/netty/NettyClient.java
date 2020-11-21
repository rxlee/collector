  package  com.kkwl.collector.channels.netty;
  
  import com.kkwl.collector.channels.netty.NettyClient;
import com.kkwl.collector.channels.netty.handler.ClientSocketHandler;
import com.kkwl.collector.common.GlobalVariables;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
  
  
  
  
  
  public class NettyClient
  {
    private String sn;
    private EventLoopGroup eventLoopGroup;
    private Bootstrap bootstrap;
    private String serverIp;
    private int serverPort;
    private int reconnectDuration;
    private boolean stopThread;
    private static Logger logger = LoggerFactory.getLogger(NettyClient.class);
  
    
    public LoggingHandler createLoggingHandler() { return new LoggingHandler(LogLevel.INFO); }
  //nettyclient客户端
    public NettyClient(String sn, String serverIp, int serverPort, int reconnectDuration) {
      this.stopThread = false;
      this.sn = sn;
      this.serverIp = serverIp;
      this.serverPort = serverPort;
      this.reconnectDuration = reconnectDuration;
    }
  
    
    public void start() {
    	 GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.execute(new Runnable()
         {
           public void run() {
             NettyClient.this.connect();
           }
         });
    }
    
    public String getSn() { return this.sn; }
  
    /**
     * 停止连接
     */
    public void stop() { this.stopThread = true; }
  
    
    private void connect() {
      this.eventLoopGroup = new NioEventLoopGroup();
      if (this.stopThread) {
        return;
      }
      
      try {
        this.bootstrap = new Bootstrap();
        ((Bootstrap)((Bootstrap)((Bootstrap)((Bootstrap)this.bootstrap.group(this.eventLoopGroup))
          .channel(io.netty.channel.socket.nio.NioSocketChannel.class))
          .option(ChannelOption.TCP_NODELAY, Boolean.valueOf(true)))
          .handler(createLoggingHandler()))
          .handler(new ChannelInitializer<SocketChannel>()
                  { //初始化加上一个处理器
              protected void initChannel(SocketChannel socketChannel) throws Exception {
                socketChannel.pipeline().addLast(new ChannelHandler[] { new ClientSocketHandler() });
              }
            });

        ChannelFuture future = this.bootstrap.connect(this.serverIp, this.serverPort).sync();  /////
        future.addListener(new GenericFutureListener<Future<? super Void>>()
          {
            public void operationComplete(Future<? super Void> future) throws Exception {
              if (future.isSuccess()) {
                logger.info("Global configuration successfully connected with server = " + NettyClient.this.serverIp + " and port = " + NettyClient.this.serverPort);
              } else {
                logger.info("Global configuration connecting to server = " + NettyClient.this.serverIp + " and port = " + NettyClient.this.serverPort + " failed.");
              } 
            }
          });

        
        future.channel().closeFuture().sync();
      } catch (Exception e) {
        logger.error("Global configuration error occured during connection.", e);
      } finally {
        this.eventLoopGroup.shutdownGracefully();
        reconnect();
      } 
    }
    
    /**
     * 重新连接
     */
    private void reconnect() {
      try {
        this.eventLoopGroup = null;
        this.bootstrap = null;
        logger.warn("Global configuration trying to connect to server = " + this.serverIp + " and port = " + this.serverPort);
        Thread.sleep((this.reconnectDuration * 1000));
        connect();
      } catch (Exception e) {
        logger.error("Global configuration error occured when trying to connect to server = " + this.serverIp + " and port = " + this.serverPort, e);
      } 
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\NettyClient.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
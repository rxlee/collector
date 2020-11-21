  package  com.kkwl.collector.channels.netty;
  
  import com.kkwl.collector.channels.netty.NettyServerInitializer;
  import com.kkwl.collector.channels.netty.handler.ServerSocketHandler;
  import io.netty.channel.Channel;
  import io.netty.channel.ChannelHandler;
  import io.netty.channel.ChannelInitializer;
  import io.netty.channel.ChannelPipeline;
  import io.netty.channel.socket.SocketChannel;
  import org.springframework.beans.factory.annotation.Autowired;
  import org.springframework.stereotype.Component;
  
  @Component
  class NettyServerInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private ServerSocketHandler serverSocketHandler;

    protected void initChannel(SocketChannel socketChannel) throws Exception {
      ChannelPipeline pipeline = socketChannel.pipeline();  //ChannelPipeline将多个ChannelHandler链接在一起来让事件在其中传播处理
      pipeline.addLast(new ChannelHandler[] { this.serverSocketHandler });
    }
    

  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\NettyServerInitializer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
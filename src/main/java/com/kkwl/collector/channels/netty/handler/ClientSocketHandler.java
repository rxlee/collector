  package  com.kkwl.collector.channels.netty.handler;
  
  import com.kkwl.collector.channels.netty.handler.ClientSocketHandler;
import com.kkwl.collector.common.AccessType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.devices.communication.ServerDTU;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
  
  public class ClientSocketHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ClientSocketHandler.class);
  
  
    
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      String connectKey = ipAddress + ":" + port;
      
      logger.info("Socket handler " + connectKey + " connected.");
      
      boolean foundDtu = false;
      for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
        logger.info("Socket handler begin check server dtu ip = " + dtu.getIpAddress() + " port = " + dtu.getPort());
        if (dtu.getAccessType() != null && dtu.getAccessType() == AccessType.TCPCLIENT && dtu
          .getIpAddress().equals(ipAddress) && dtu.getPort().intValue() == port) {
          foundDtu = true;
  
          
          ChannelHandlerContext origCtx = (ChannelHandlerContext)GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.get(dtu.getSn());
          if (origCtx != null) {
            if (!origCtx.isRemoved()) {
              origCtx.close();
            }
            disconnectDevice(origCtx);
          } 
          
          dtu.setIpAddress(ipAddress);
          dtu.setPort(Integer.valueOf(port));
          GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.put(connectKey, dtu);
          GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.put(dtu.getSn(), ctx);
          dtu.setChannelHandlerContext(ctx);
          
          break;
        } 
      } 
      if (!foundDtu) {
        logger.warn("Socket handler can't find dtu configuration of ip = " + ipAddress + " and port = " + port);
        ctx.close();
      } 
    }
  
    //添加了Exception
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      logger.error("Socket handler socket handler exception : ", cause);
      
      disconnectDevice(ctx);
      ctx.close();
    }
  
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      ByteBuf buff = (ByteBuf)msg;
      byte[] frameBytes = new byte[buff.readableBytes()];
      buff.getBytes(0, frameBytes);
      ReferenceCountUtil.release(buff);
      
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      String connectKey = ipAddress + ":" + port;
      
      ServerDTU targetDtu = (ServerDTU)GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);
      targetDtu.handleReceivedData(frameBytes);
    }
  
  
    
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      logger.error("Socket handler socket handler closed.");
      disconnectDevice(ctx);
      ctx.close();
    }
    
    private void disconnectDevice(ChannelHandlerContext ctx) throws Exception {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      String connectKey = ipAddress + ":" + port;
      ServerDTU dtu = (ServerDTU)GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);
      if (dtu != null) {
        GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.remove(dtu.getSn());
      }
      if (dtu != null) {
        logger.info("Socket handler send disconnect message to all sub collector devices.");
        dtu.onDisconnected();
      } 
      GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.remove(connectKey);
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\handler\ClientSocketHandler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
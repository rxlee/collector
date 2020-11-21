  package  com.kkwl.collector.channels.netty.handler;
  import com.kkwl.collector.channels.netty.handler.ServerSocketHandler;
import com.kkwl.collector.common.AccessType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.devices.communication.ServerDTU;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
  
  @Component
  @Sharable
  public class ServerSocketHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ServerSocketHandler.class);

    @Value("${com.kkwl.collector.need_port_check}")
    private byte needPortCheck;
  
    //当通道有内容时激活 当Channel变成活跃状态时被调用；Channel是连接/绑定、就绪的
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      String connectKey = ipAddress + ":" + port;  //远程ip和端口 连接符
      
      boolean isRemoteHostConfigured = false;
      //遍历dtu
      for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {
        if (dtu.getIpAddress() == null || dtu.getPort() == null || !dtu.getIpAddress().equals(ipAddress) ||
			 ((dtu.getAccessType() == AccessType.FIXEDIP) ? (this.needPortCheck == 1 && dtu.getPort().intValue() != port) : (dtu.getPort().intValue() != port))) {
              //只要有一个条件成立就跳出此次循环
          continue;
        }
        isRemoteHostConfigured = true;
        logger.info("dtu sn = " + dtu.getSn() + " access type = " + dtu.getAccessType());

        ChannelHandlerContext origCtx = (ChannelHandlerContext)GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.get(dtu.getSn());
        if (origCtx != null) {
          if (!origCtx.isRemoved()) {
            origCtx.close();
          }
          disconnectDevice(origCtx);
        } 
        
        dtu.setAccessType(AccessType.FIXEDIP); //？？？为什么要设置成固定接入
        GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.put(connectKey, dtu);
      } 

      if (isRemoteHostConfigured) {
        logger.info("Socket handler " + connectKey + " connected.");
        kickStart(connectKey, ctx); //************通道启动工作**********
      } 
    }
  
  //加上Exception  
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      logger.error("Socket handler socket handler exception : ", cause);
      
      disconnectDevice(ctx);
      ctx.close();
    }
  
    //执行通道激活后，自动执行读取通道内容
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      ByteBuf buff = (ByteBuf)msg;
      byte[] frameBytes = new byte[buff.readableBytes()]; //定义收到的帧字节数组     buff.readableBytes()可读字节数
      buff.getBytes(0, frameBytes); //************把缓冲区的数据传到帧字节数组*******最开始收到的字节内容
      ReferenceCountUtil.release(buff);
      
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString(); //远程设备ip
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort(); //远程端口
      String connectKey = ipAddress + ":" + port;    //连接符：现场ip+port
      
      ServerDTU targetDtu = (ServerDTU)GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey); //由连接符获得dtu
      //如果targetDtu不存在，则匹配连接标识符，匹配成功则启动通道
      if (targetDtu == null) {
        logger.info("Socket handler can't find dtu with connect key = " + connectKey); //////////////////

        String identifier = new String(frameBytes); //***********************采集到的连接标识符**********************
        StringBuilder bytesSb = new StringBuilder();
        for (byte b : frameBytes) {
          bytesSb.append(String.format("%02X", new Object[] { Byte.valueOf(b) })); //%02X以十六进制形式输出 02 表示不足两位,前面补0输出
        } 
        String bytesIdentifier = bytesSb.toString(); //采集到的连接标识符的字符形式
        boolean foundDtu = false;
        for (ServerDTU dtu : GlobalVariables.GLOBAL_SERVER_DTUS) {//*********************遍历所有通道，进行匹配**************************
          logger.info("Begin trying matching dtu with identifier = " + dtu.getIdentifier() + " and tcp identifier = " + identifier + " | " + bytesIdentifier);
          if (dtu.getIdentifier() != null) {
            //判断
            if (dtu.getIdentifier().startsWith("0x")) {
              String identiferSub = dtu.getIdentifier().substring(2); //取标识符
              if (bytesIdentifier.equals(identiferSub)) { //标识符处理后判断：如果采集到的连接标识符和通道的相同
                logger.info("Socket handler found dtu with identifier = " + identiferSub);
                
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
  
                
                ByteBuf sendBuf = ctx.alloc().buffer(frameBytes.length);
                logger.info("Send " + bytesIdentifier + " to " + ipAddress + " and " + port);
                sendBuf.writeBytes(frameBytes); //
                ctx.writeAndFlush(sendBuf);
                kickStart(connectKey, ctx); //
                return;
              } 
              continue;
            } 
            if (dtu.getIdentifier().equals(identifier)) { //标识符直接判断
              logger.info("Socket handler found dtu with identifier = " + identifier);
  
              
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
              kickStart(connectKey, ctx); //************通道启动工作**********
              
              return;
            } 
          } 
        } 
        if (!foundDtu) {
          
          logger.warn("Socket handler can't find dtu with identifier = " + identifier + ", so disconnected!"); //////////////
          ctx.close();
        } 
      } 
      
      if (targetDtu != null) {
        targetDtu.handleReceivedData(frameBytes); //*****************处理接收到的帧字节数据*********************
      }
    }
     //启动，将（通道编号、通道内容）加到全局变量MAP
    private void kickStart(String connectKey, ChannelHandlerContext ctx) {
      ServerDTU dtu = (ServerDTU)GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);
      if (dtu != null && !GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.containsKey(dtu.getSn()))
      {
        GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.put(dtu.getSn(), ctx);
        dtu.setChannelHandlerContext(ctx); //**************通道添加处理内容*******************
        dtu.setLastReceivedPacketTime(Instant.now());
      } 
    }
  
  
  
     //和服务器断开，则触发
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      logger.error("Socket handler socket handler closed.");
      disconnectDevice(ctx);
      ctx.close();
    }
    //断开设备，从map中删除
    private void disconnectDevice(ChannelHandlerContext ctx) throws Exception {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      String connectKey = ipAddress + ":" + port;
      ServerDTU dtu = (ServerDTU)GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.get(connectKey);
      if (dtu != null) {
        GlobalVariables.GLOBAL_CHANNEL_HANDLER_CONTEXT_MAP.remove(dtu.getSn()); //删除
      }
      if (dtu != null) {
        logger.warn("Socket handler send onDisconnected message to all sub collector devices.");
        dtu.onDisconnected();
      } 
      GlobalVariables.GLOBAL_CONNECTKEY_SERVER_DTU_MAP.remove(connectKey);
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\handler\ServerSocketHandler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
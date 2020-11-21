  package  com.kkwl.collector.channels.netty.handler;
  
  import com.kkwl.collector.channels.netty.handler.DataTransferSocketHandler;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.utils.Caculator;
import com.kkwl.collector.utils.NumberUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
  
  @Component
  @Sharable
  public class DataTransferSocketHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(DataTransferSocketHandler.class);
  
    
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      logger.info("Global data transfer client ip = " + ipAddress + " port = " + port + " connected.");
    }
  
    
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      logger.warn("Global data transfer client ip = " + ipAddress + " port = " + port + " disconnected because of ", cause);
    }
  
    
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
      ByteBuf buff = (ByteBuf)msg;//传输的数据
      byte[] frameBytes = new byte[buff.readableBytes()];
      buff.getBytes(0, frameBytes);
      ReferenceCountUtil.release(buff);
      
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      String connectKey = ipAddress + ":" + port;
      
      StringBuilder hexStr = new StringBuilder();
      for (byte b : frameBytes) {
    	  //%02X 以16进制输出
        hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(b) }));
      } 
      logger.info("Data transfer socket handler received from " + connectKey + " : " + hexStr.toString());
      
      if (frameBytes.length != 8) {
        logger.warn("Data transfer socket handler discard received bytes because length != 8");
        
        return;
      } 
      
      List<Byte> sendbuf = new ArrayList<Byte>();
      int nRegLen = (frameBytes[4] & 0xFF) * 256 + (frameBytes[5] & 0xFF);
      int nRegAddr = (frameBytes[2] & 0xFF) * 256 + (frameBytes[3] & 0xFF);
      sendbuf.add(Byte.valueOf(frameBytes[0]));
      sendbuf.add(Byte.valueOf(frameBytes[1]));
      
      if (frameBytes[1] != 3) {
        logger.warn("Data transfer socket handler error register code");
        
        sendbuf.remove(1);
        sendbuf.add(Byte.valueOf((byte)(frameBytes[1] + 128 & 0xFF)));
        sendbuf.add(Byte.valueOf((byte)1));
  
        
        byte[] toBeCaculated = new byte[3];
        toBeCaculated[0] = ((Byte)sendbuf.get(0)).byteValue();
        toBeCaculated[1] = ((Byte)sendbuf.get(1)).byteValue();
        toBeCaculated[2] = ((Byte)sendbuf.get(2)).byteValue();
        int crc = Caculator.caculateCRC16(toBeCaculated);
        sendbuf.add(Byte.valueOf((byte)(crc % 256)));
        sendbuf.add(Byte.valueOf((byte)(crc / 256)));
      }
      else {
        
        int nMaxReqReg = nRegAddr + nRegLen;
        int nMaxReg = 0;
        
        List<DeviceVariable> analogDeviceVariables = new ArrayList<DeviceVariable>();
        for (DeviceVariable analogVariable : GlobalVariables.getDeviceVariables()) {
          if (analogVariable.getType().equals("Analog")) {
            analogDeviceVariables.add(analogVariable);
          }
        } 
        
        for (int i = 0; i < analogDeviceVariables.size(); i++) {
          nMaxReg += 2;
        }
  
  
        
        int nAnalogStartNo = 0;
        int nTmpRegAddr = 0;
        for (int i = 0; i < analogDeviceVariables.size(); i++) {
          if (nTmpRegAddr == nRegAddr) {
            nAnalogStartNo = i;
            break;
          } 
          nTmpRegAddr += 2;
        } 
  
        
        if (nRegLen > 125 || nMaxReqReg > nMaxReg) {
          
          sendbuf.remove(1);
          sendbuf.add(Byte.valueOf((byte)(frameBytes[1] + 128 & 0xFF)));
          sendbuf.add(Byte.valueOf((byte)2));
  
  
          
          byte[] toBeCaculated = new byte[3];
          toBeCaculated[0] = ((Byte)sendbuf.get(0)).byteValue();
          toBeCaculated[1] = ((Byte)sendbuf.get(1)).byteValue();
          toBeCaculated[2] = ((Byte)sendbuf.get(2)).byteValue();
          int crc = Caculator.caculateCRC16(toBeCaculated);
          sendbuf.add(Byte.valueOf((byte)(crc % 256)));
          sendbuf.add(Byte.valueOf((byte)(crc / 256)));
        } else {
          sendbuf.add(Byte.valueOf((byte)(nRegLen * 2)));
          
          int nAddr = 0;
          for (int i = 0; nAddr < nRegLen; i++) {
            float a = Float.valueOf(((DeviceVariable)analogDeviceVariables.get(nAnalogStartNo + i)).getRegisterValue()).floatValue();
            int v = Float.floatToIntBits(a);
            byte[] arr = NumberUtil.intToByte4(v);
            
            if (nRegLen * 2 > sendbuf.size() - 3) {
              sendbuf.add(Byte.valueOf(arr[2]));
              sendbuf.add(Byte.valueOf(arr[3]));
              nAddr++;
            } 
            
            if (nRegLen * 2 > sendbuf.size() - 3) {
              sendbuf.add(Byte.valueOf(arr[0]));
              sendbuf.add(Byte.valueOf(arr[1]));
              nAddr++;
            } 
          } 
  
          
          byte[] toBeCaculated = new byte[sendbuf.size()];
          for (int i = 0; i < sendbuf.size(); i++) {
            toBeCaculated[i] = ((Byte)sendbuf.get(i)).byteValue();
          }
          int crc = Caculator.caculateCRC16(toBeCaculated);
          sendbuf.add(Byte.valueOf((byte)(crc % 256)));
          sendbuf.add(Byte.valueOf((byte)(crc / 256)));
        } 
      } 
      
      byte[] writeBuf = new byte[sendbuf.size()];
      hexStr = new StringBuilder();
      for (int i = 0; i < sendbuf.size(); i++) {
        
        writeBuf[i] = ((Byte)sendbuf.get(i)).byteValue();
        hexStr.append(String.format("%02X ", new Object[] { sendbuf.get(i) }));
      } 
      logger.info("Data transfer socket handler write to querier : " + hexStr.toString());
      
      ByteBuf sendBuf = ctx.alloc().buffer(sendbuf.size());
      sendBuf.writeBytes(writeBuf);
      
      ChannelFuture writeFuture = ctx.writeAndFlush(sendBuf);
      writeFuture.addListener(new ChannelFutureListener()
        {
          public void operationComplete(ChannelFuture channelFuture) throws Exception {
            if (!writeFuture.isSuccess())
            {
              logger.warn("Data transfer socket handler failed to write to querier");
            }
          }
        });
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      String ipAddress = ((InetSocketAddress)ctx.channel().remoteAddress()).getHostString();
      int port = ((InetSocketAddress)ctx.channel().remoteAddress()).getPort();
      logger.info("Global data transfer client ip = " + ipAddress + " port = " + port + " disconnected.");
    }
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\channels\netty\handler\DataTransferSocketHandler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
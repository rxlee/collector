  package  com.kkwl.collector.devices.communication;
  
  import com.kkwl.collector.common.AccessType;
import com.kkwl.collector.common.FrameType;
import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.common.LogType;
import com.kkwl.collector.common.StatusType;
import com.kkwl.collector.devices.BaseDevice;
import com.kkwl.collector.devices.business.BaseBusinessDevice;
import com.kkwl.collector.devices.communication.ServerDTU;
import com.kkwl.collector.models.response.LogViewResponse;
import com.kkwl.collector.utils.LogTools;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.sql.Timestamp;
import java.time.Instant;
  import java.time.LocalDateTime;
  import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

  /**
 * @author Andy
 *
 */
public class ServerDTU extends BaseDevice
  {
    private String identifier; //注册码
    private String ipAddress;
    private Integer port;
    private int timeInterval;
    private int accumulatedTimeInterval;
    private AccessType accessType; //通道类型
    private static final Logger logger = LoggerFactory.getLogger(ServerDTU.class);
    private int deviceIndex; //设备序号
    private ReentrantLock deviceIndexLock;
    private List<BaseBusinessDevice> devices; //通道设备
    private ScheduledFuture businessDispatcher;
    private ScheduledFuture bytesSender;
    private ChannelHandlerContext ctx;  //通道内容
    
    //初始化通道服务端  周期性（handlingPeriod根据设备设置）采集
    public ServerDTU(String sn, String name, String parentIndex, String identifier, String type, String ipAddress, Integer port, int timeInterval, long handlingPeriod, byte shouldReportConnectionAlarm, short connectionTimeoutDuration, AccessType accessType, String offlineReportId, Timestamp offlineTime, StatusType initStatus)
    {
      this.deviceIndexLock = new ReentrantLock();
      this.devices = new ArrayList();
      this.sn = sn;
      this.name = name;
      this.parentIndex = parentIndex;
      this.identifier = identifier;
      this.type = type;
      this.ipAddress = ipAddress;
      this.port = port;
      this.timeInterval = timeInterval;
      this.deviceIndex = 0;
      this.shouldReportConnectionAlarm = shouldReportConnectionAlarm;
      this.connectionTimeoutDuration = connectionTimeoutDuration;
      this.status = initStatus;
      this.lastReceivedPacketTime = (initStatus == StatusType.OFFLINE && offlineTime != null) ? offlineTime.toInstant() : Instant.now();
      this.handlingPeriod = handlingPeriod;
      this.accessType = accessType;
      this.ctx = null;
      this.offlineReportId = offlineReportId;
      this.offlineTime = offlineTime;
      //scheduleWithFixedDelay以固定延迟（时间）来执行线程任务,它实际上是不管线程任务的执行时间的，每次都要把任务执行完成后再延迟固定时间后再执行下一次。
      //参数： 处理的业务逻辑，首次延迟的时间，从一次执行的终止到下一次执行的开始之间的延迟，单位
      this.businessDispatcher = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleWithFixedDelay(new Runnable()
        {
          public void run() {
            try {
              ServerDTU.this.dispatch();  //发送采集命令
            } catch (Exception e) {
              LogTools.log(logger, sn, LogType.WARN, "Dtu sn = " + sn + " call dispatch encountered error. ", e);
            } 
          }
        }, 0L, handlingPeriod, TimeUnit.MILLISECONDS);
    }

    public String getIdentifier() { return this.identifier; }

    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getIpAddress() { return this.ipAddress; }

    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getPort() { return this.port; }

    public void setPort(Integer port) { this.port = port; }

    public void setChannelHandlerContext(ChannelHandlerContext ctx) {
      if (this.ctx != null && !this.ctx.isRemoved()) {
        this.ctx.close();
      }
      this.ctx = ctx;
      if (this.businessDispatcher != null) {
        this.businessDispatcher.cancel(true);
        this.businessDispatcher = null;
      }
      for (BaseBusinessDevice device : this.devices) {
        device.onConnected();
      }
      //线程池延迟周期采集
      this.businessDispatcher = GlobalVariables.GLOBAL_SCHEDULED_SERVICE_POOL.scheduleWithFixedDelay(new Runnable()
        {
          public void run() {
            try {
              ServerDTU.this.dispatch(); //************调用-采集函数*****************
            } catch (Exception e) {
              LogTools.log(logger, ServerDTU.this.sn, LogType.WARN, "Dtu sn = " + ServerDTU.this.sn + " call dispatch encountered error. ", e);
            } 
          }
        }, 0L, this.handlingPeriod, TimeUnit.MILLISECONDS);
    }

    public void setDevices(List<BaseBusinessDevice> devices) { this.devices = devices; }
    //采集函数
    public void dispatch() {
        if (this.devices.isEmpty()) {
          return;
        }
        if (this.devices.isEmpty()) {
          return;
        }
      BaseBusinessDevice device = (BaseBusinessDevice)this.devices.get(this.deviceIndex);
      if (!device.isDeviceBeingUsed()) {
        return;
      }
      //对设备解析
      device.doParse(); //********************解析****************************
      //发送采集命令，并将采集命令存到websocket_queue
      if (device.hasDataToSend()) {
        while (device.getOutBytesBuffer().size() > 0) {
          Pair<FrameType, byte[]> pair = (Pair)device.getOutBytesBuffer().poll(); ///首次握手68 04 07 00 00 00
          FrameType frameType = (FrameType)pair.getValue0();
          byte[] buf = (byte[])pair.getValue1();//*************************要发送的内容***********************
          StringBuilder hexStr = new StringBuilder();  ///采集命令组装
          for (int i = 0; i < buf.length; i++) {
            hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(buf[i]) }));
          } 
          
          LogTools.log(logger, this.sn, LogType.INFO, "Dtu sn = " + this.sn + " send to ip = " + this.ipAddress + ", port = " + this.port + ": " + hexStr
              .toString());
          
          if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null && this.sn.equals(GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU) && this.port != null && this.ipAddress != null) {
            ///将采集指令存到GLOBAL_WEBSOCKET_MESSAGE_QUEUE.
            LogViewResponse response = new LogViewResponse();
            response.setDeviceSn(this.sn);
            response.setIpAddress(this.ipAddress);
            response.setPort(this.port.toString());
            response.setAction("SEND");
            response.setTime(LocalDateTime.now().toString());//zth新增2020427
            response.setContent(hexStr.toString());
            GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.add(response);
          } 
          
          if (this.ctx != null && !this.ctx.isRemoved()) {
            ByteBuf sendBuf = this.ctx.alloc().buffer(buf.length);
            sendBuf.writeBytes(buf);
            //**********************发送并刷新***********************
            ChannelFuture writeFuture = this.ctx.writeAndFlush(sendBuf);
            
            BaseBusinessDevice deviceForinner = device;
            writeFuture.addListener(new ChannelFutureListener()
              {
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                  if (!writeFuture.isSuccess())
                  {
                    
                    LogTools.log(logger, ServerDTU.this.sn, LogType.WARN, "Dtu sn = " + ServerDTU.this.sn + " failed to send message to ip = " + ServerDTU.this
                        .ipAddress + ", port = " + ServerDTU.this.port + ": " + hexStr.toString());
                  }
                  
                  deviceForinner.toggleFlag(frameType);
                }
              });
          } 
  
          if (device.getOutBytesBuffer().isEmpty()) {
            device.setHasDataToSend(false);
          }
        } 
      }
      
      if (this.accumulatedTimeInterval > this.timeInterval) {
        this.accumulatedTimeInterval = 0;
        //设备业务
        device.doBusiness();

        if (device.canSwitchToNext()) {
          lockDeviceLock();
          try {
            this.deviceIndex++;
            if (this.deviceIndex == this.devices.size())
            {
              this.deviceIndex = 0;
            }
            device.resetCanSwitchToNext();
          } finally {
            this.deviceIndexLock.unlock();
          } 
        } 
      } else {
        this.accumulatedTimeInterval = (int)(this.accumulatedTimeInterval + this.handlingPeriod);
      } 
    }
    //处理接受到的帧字节--将接受到的数据存到设备的InBytesBuffer，并封装后添加到wwebsocket_queue
    public void handleReceivedData(byte[] frameBytes) {
      if (this.devices.isEmpty()) {
        return;
      }
      BaseBusinessDevice device = (BaseBusinessDevice)this.devices.get(this.deviceIndex);
      if (device == null) {
        return;
      }
      if (!device.isDeviceBeingUsed()) {
        return;
      }

      device.addInInBytesBuffer(frameBytes); //***************存收到的数据*********************
      setLastReceivedPacketTime(Instant.now());
      device.setLastReceivedPacketTime(Instant.now());
      
      StringBuilder hexStr = new StringBuilder();///组装接收到的数据 hexStr
      for (byte b : frameBytes) {
        hexStr.append(String.format("%02X ", new Object[] { Byte.valueOf(b) }));
      } 
      
      LogTools.log(logger, this.sn, LogType.INFO, "Dtu sn = " + this.sn + " received data from ip = " + this.ipAddress + ", " + this.port + ": " + hexStr);
  
      
      if (GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU != null && this.sn.equals(GlobalVariables.GLOBAL_WEBSOCKET_BIND_DTU)) {
        LogViewResponse response = new LogViewResponse(); ///接受到的数据封装 --设备sn|ip|port|action|target|time|content
        response.setDeviceSn(this.sn);
        response.setIpAddress(this.ipAddress);
        response.setPort(this.port.toString());
        response.setAction("RECV");
        response.setTime(LocalDateTime.now().toString());//zth新增2020427
        response.setContent(hexStr.toString());
        GlobalVariables.GLOBAL_WEBSOCKET_MESSAGE_QUEUE.add(response); //*******************将接受到的数据封装后添加到websocket消息队列，供码流用*******************
      } 
    }
    
    public void addDevice(BaseBusinessDevice toBeAddedDevice) {
      lockDeviceLock();
      
      try {
        this.devices.add(toBeAddedDevice);
        this.deviceIndex = 0;
      } finally {
        this.deviceIndexLock.unlock();
      } 
    }
    
    public void delDevice(String toBeDeletedDeviceSn) {
      lockDeviceLock();
      
      try {
        Iterator<BaseBusinessDevice> deviceIterator = this.devices.iterator();
        while (deviceIterator.hasNext()) {
          BaseBusinessDevice tmpDevice = (BaseBusinessDevice)deviceIterator.next();
          if (tmpDevice.getSn().equals(toBeDeletedDeviceSn)) {
            tmpDevice.delete();
            deviceIterator.remove();
            break;
          } 
        } 
        this.deviceIndex = 0;
      } finally {
        this.deviceIndexLock.unlock();
      } 
    }

    public AccessType getAccessType() { return this.accessType; }

    public void setAccessType(AccessType accessType) { this.accessType = accessType; }

    public void onDisconnected() {
      for (BaseBusinessDevice businessDevice : this.devices) {
        businessDevice.onDisconnected();
      }
      
      if (this.businessDispatcher != null) {
        this.businessDispatcher.cancel(true);
        this.businessDispatcher = null;
      } 
      
      if (this.accessType.equals(AccessType.DIAL)) {
        this.ipAddress = null;
        this.port = null;
      } 
    }
    
    public void delete() {
      if (this.businessDispatcher != null) {
        this.businessDispatcher.cancel(true);
        this.businessDispatcher = null;
      } 
    }
    
    public void disconnect() {
      if (this.ctx != null && !this.ctx.isRemoved()) {
        this.ctx.channel().close();
      }
    }

    public Timestamp getLastDataPackteTime() { return Timestamp.from(this.lastReceivedPacketTime); }

    private void lockDeviceLock() {
      while (!this.deviceIndexLock.tryLock()) {
        boolean shouldReturning = false;
        try {
          Thread.sleep(5L);
        } catch (InterruptedException e) {
          LogTools.log(logger, this.sn, LogType.INFO, "Dtu sn = " + this.sn + " waiting for getting device index lock.");
          shouldReturning = true;
        } 
        
        if (shouldReturning)
          return; 
      } 
    }

	public int getTimeInterval() {
		return timeInterval;
	}

	public void setTimeInterval(int timeInterval) {
		this.timeInterval = timeInterval;
	}

	public int getAccumulatedTimeInterval() {
		return accumulatedTimeInterval;
	}

	public void setAccumulatedTimeInterval(int accumulatedTimeInterval) {
		this.accumulatedTimeInterval = accumulatedTimeInterval;
	}

	public int getDeviceIndex() {
		return deviceIndex;
	}

	public void setDeviceIndex(int deviceIndex) {
		this.deviceIndex = deviceIndex;
	}

	public ReentrantLock getDeviceIndexLock() {
		return deviceIndexLock;
	}

	public void setDeviceIndexLock(ReentrantLock deviceIndexLock) {
		this.deviceIndexLock = deviceIndexLock;
	}

	public ScheduledFuture getBusinessDispatcher() {
		return businessDispatcher;
	}

	public void setBusinessDispatcher(ScheduledFuture businessDispatcher) {
		this.businessDispatcher = businessDispatcher;
	}

	public ScheduledFuture getBytesSender() {
		return bytesSender;
	}

	public void setBytesSender(ScheduledFuture bytesSender) {
		this.bytesSender = bytesSender;
	}

	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	public List<BaseBusinessDevice> getDevices() {
		return devices;
	}
  }


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\devices\communication\ServerDTU.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
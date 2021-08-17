package com.kkwl.collector.models;

import com.kkwl.collector.models.CollectorDtu;

import java.sql.Timestamp;


public class CollectorDtu {
    private String sn;
    private String name;
    private String ipAddress;
    private Integer port;
    private String identifier;
    private int timeInterval;
    private Integer status;
    private String statusName;
    private String parentIndex; //站点
    private String accessType;
    private byte shouldReportConnectionAlarm;
    private short connectionTimeoutDuration;
    private String collectorIpAddress;
    private String collectorPort;
    private String lastOfflineReportId = null;
    private Timestamp lastOfflineReportTime = null;


    public String getSn() {
        return this.sn;
    }


    public void setSn(String sn) {
        this.sn = sn;
    }


    public String getName() {
        return this.name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getIpAddress() {
        return this.ipAddress;
    }


    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public Integer getPort() {
        return this.port;
    }


    public void setPort(Integer port) {
        this.port = port;
    }


    public String getIdentifier() {
        return this.identifier;
    }


    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }


    public int getTimeInterval() {
        return this.timeInterval;
    }


    public void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
    }


    public Integer getStatus() {
        return this.status;
    }


    public void setStatus(Integer status) {
        this.status = status;
    }


    public String getStatusName() {
        return this.statusName;
    }


    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }


    public String getParentIndex() {
        return this.parentIndex;
    }
  

    public void setParentIndex(String parentIndex) {
        this.parentIndex = parentIndex;
    }


    public byte getShouldReportConnectionAlarm() {
        return this.shouldReportConnectionAlarm;
    }


    public void setShouldReportConnectionAlarm(byte shouldReportConnectionAlarm) {
        this.shouldReportConnectionAlarm = shouldReportConnectionAlarm;
    }


    public short getConnectionTimeoutDuration() {
        return this.connectionTimeoutDuration;
    }


    public void setConnectionTimeoutDuration(short connectionTimeoutDuration) {
        this.connectionTimeoutDuration = connectionTimeoutDuration;
    }


    public String getCollectorIpAddress() {
        return this.collectorIpAddress;
    }


    public void setCollectorIpAddress(String collectorIpAddress) {
        this.collectorIpAddress = collectorIpAddress;
    }


    public String getCollectorPort() {
        return this.collectorPort;
    }


    public void setCollectorPort(String collectorPort) {
        this.collectorPort = collectorPort;
    }


    public String getAccessType() {
        return this.accessType;
    }


    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }


    public String getLastOfflineReportId() {
        return this.lastOfflineReportId;
    }


    public void setLastOfflineReportId(String lastOfflineReportId) {
        this.lastOfflineReportId = lastOfflineReportId;
    }


    public Timestamp getLastOfflineReportTime() {
        return this.lastOfflineReportTime;
    }


    public void setLastOfflineReportTime(Timestamp lastOfflineReportTime) {
        this.lastOfflineReportTime = lastOfflineReportTime;
    }
}


/* Location:              C:\Users\Andy\Desktop\EMS\server-2\kk\collector\collector-0.0.1-SNAPSHOT.jar!\BOOT-INF\classes\com\kkwl\collector\models\CollectorDtu.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.0.7
 */
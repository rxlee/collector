package com.kkwl.collector.models;

import java.io.Serializable;
import java.util.Date;


public class StationCharge implements Serializable {
    private Integer id;
    private String stationSn;
    private String type;
    private String startTime;
    private String pfvSettingF;
    private String pfvSettingR;
    private String deviceSettings;
    private String capacityCharge;
    private String cosCharge;
    private String additionCharge;
    private String extendJs;
    private Date createTime;
    private Date updateTime;
    private Integer enabled;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getStationSn() {
        return stationSn;
    }

    public void setStationSn(String stationSn) {
        this.stationSn = stationSn;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getPfvSettingF() {
        return pfvSettingF;
    }

    public void setPfvSettingF(String pfvSettingF) {
        this.pfvSettingF = pfvSettingF;
    }

    public String getPfvPfvSettingR() {
        return pfvSettingR;
    }

    public void setPfvPfvSettingR(String pfvPfvSettingR) {
        this.pfvSettingR = pfvPfvSettingR;
    }

    public String getDeviceSettings() {
        return deviceSettings;
    }

    public void setDeviceSettings(String deviceSettings) {
        this.deviceSettings = deviceSettings;
    }

    public String getCapacityCharge() {
        return capacityCharge;
    }

    public void setCapacityCharge(String capacityCharge) {
        this.capacityCharge = capacityCharge;
    }

    public String getCosCharge() {
        return cosCharge;
    }

    public void setCosCharge(String cosCharge) {
        this.cosCharge = cosCharge;
    }

    public String getAdditionCharge() {
        return additionCharge;
    }

    public void setAdditionCharge(String additionCharge) {
        this.additionCharge = additionCharge;
    }

    public String getExtendJs() {
        return extendJs;
    }

    public void setExtendJs(String extendJs) {
        this.extendJs = extendJs;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}

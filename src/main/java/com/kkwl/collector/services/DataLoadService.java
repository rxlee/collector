package com.kkwl.collector.services;

import com.kkwl.collector.common.GlobalVariables;
import com.kkwl.collector.dao.Configuration;
import com.kkwl.collector.models.CollectorDevice;
import com.kkwl.collector.models.CollectorDtu;
import com.kkwl.collector.models.DeviceVariable;
import com.kkwl.collector.services.DataLoadService;
import com.kkwl.collector.utils.DevicesTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataLoadService {
    private static final Logger logger = LoggerFactory.getLogger(DataLoadService.class);
    @Autowired
    private Configuration configuration;
    @Autowired
    private RedisService redisService;
    @Autowired
    private DeviceVariableService deviceVariableService;

    //根据通道加载设备  NettyComponent使用
    public void loadData(List<CollectorDtu> dtus) {
        if (dtus.isEmpty()) {
            logger.warn("Data load service dtu size = 0");

            return;
        }
        List<CollectorDevice> devices = loadDevices(dtus);  //********************初始化 根据当前通道获取采集设备******************
        if (devices == null) {
            logger.warn("Data load service device size = 0");
            return;
        }
        loadDevicesAndDeviceVariables(dtus, devices);  //*************初始化 加载所有设备和变量************
    }

    //加载数据（通道类型） KAFKA、MQTT使用
    public void loadData(String accessType) {
        logger.info("Data load service importing data from mysql with access type = " + accessType + " .");
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("access_type", accessType);

        List<CollectorDtu> dtus = this.configuration.getDtus(filter); //******通道******
        if (dtus.isEmpty()) {
            logger.warn("Data load service dtu size = 0");

            return;
        }
        List<CollectorDevice> devices = loadDevices(dtus); //*****根据当前通道获取采集设备*****
        if (devices == null) {
            logger.warn("Data load service device size = 0");

            return;
        }
        loadDevicesAndDeviceVariables(dtus, devices); //加载所有设备和变量（通道、采集设备）
    }

    //加载采集设备（通道）
    private List<CollectorDevice> loadDevices(List<CollectorDtu> dtus) {
        Set<String> dtuSns = new HashSet<String>();//通道sn
        for (CollectorDtu dtu : dtus) {
            dtuSns.add(dtu.getSn());
        }
        //查询条件
        Map<String, Object> filter = new HashMap<String, Object>();
        filter.put("collector_dtu_sns", dtuSns);
        List<CollectorDevice> devices = this.configuration.getDevices(filter);//初始化 查库查询所有的采集设备
        if (devices.isEmpty()) {
            logger.warn("Data load service collector devices size = 0");
            return null;
        }

        List<CollectorDevice> collectorDevices = new ArrayList<CollectorDevice>();
        List<String> collectorDeviceSns = new ArrayList<String>();
        for (CollectorDevice device : devices) {
            if (dtuSns.contains(device.getDtuSn())) {
                collectorDevices.add(device);
                collectorDeviceSns.add(device.getSn());
            }
        }
        //返回采集设备对象集合
        return collectorDevices;
    }

    //加载设备和设备变量
    private void loadDevicesAndDeviceVariables(List<CollectorDtu> dtus, List<CollectorDevice> devices) {
        List<String> deviceSns = new ArrayList<String>();//所有采集设备sn
        for (CollectorDevice device : devices) {
            deviceSns.add(device.getSn()); //采集设备的sn
        }
        List<DeviceVariable> deviceVariables = this.deviceVariableService.getDeviceVariablesByDeviceSns(deviceSns); //*******初始化 根据采集设备获取设备变量************
        GlobalVariables.addDeviceVariables(deviceVariables);  //****************初始化时 把变量添加到全局设备变量*****************
        DevicesTools.createAllComponents(dtus, devices);//**********绑定通道，通道对应的采集设备，全局添加通道客户端/通道客户端*************
    }
}


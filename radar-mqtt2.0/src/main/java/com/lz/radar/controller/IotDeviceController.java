package com.lz.radar.controller;

import com.lz.radar.common.TableDataInfo;
import com.lz.radar.service.IotDeviceService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/device")
public class IotDeviceController {

    @Autowired
    private IotDeviceService iotDeviceService;

    @GetMapping("/list")
    public TableDataInfo list(){
        return TableDataInfo.getDataTable(iotDeviceService.getAllDevices());
    }


}

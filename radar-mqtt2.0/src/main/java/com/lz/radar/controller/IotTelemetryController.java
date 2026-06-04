package com.lz.radar.controller;

import com.lz.radar.common.AjaxResult;
import com.lz.radar.domain.IotDevice;
import com.lz.radar.service.IotDeviceService;
import com.lz.radar.service.IotTelemetryService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/telemetry")
public class IotTelemetryController {

    @Autowired
    private IotTelemetryService iotTelemetryService;

    @Autowired
    private IotDeviceService iotDeviceService;

    @GetMapping("/device_or_telemetry_data/{deviceId}")
    public AjaxResult deviceOrTelemetryData(@PathVariable(value = "deviceId") Long deviceId){


        return AjaxResult.success(iotTelemetryService.selectDeviceOrTelemetryData());

    }

}

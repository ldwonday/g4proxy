package com.virjar.g4proxyserver.controller;


import com.virjar.g4proxyserver.ProxyInstanceHolder;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by virjar on 2019/2/23.
 */
@RestController
public class AvailablePortController {

    @GetMapping("portList")
    public String availablePortMapping() {

        return StringUtils.join(ProxyInstanceHolder.g4ProxyServer.getClientManager().getClientPortBiMap().values(), "\r\n");
    }

    @GetMapping("deviceMapping")
    @ResponseBody
    public Object deviceMapping() {
        return ProxyInstanceHolder.g4ProxyServer.getClientManager().getClientPortBiMap();
    }
}

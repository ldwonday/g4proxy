package com.virjar.g4proxyserver;

import com.virjar.g4proxy.server.G4ProxyServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class G4proxyServerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        ProxyInstanceHolder.g4ProxyServer = new G4ProxyServer(50000);
        ProxyInstanceHolder.g4ProxyServer.startUp();

        SpringApplication.run(G4proxyServerApplication.class, args);

    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(G4proxyServerApplication.class);
    }

}

package com.traderx;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Configuration;
import com.traderx.models.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@EnableScheduling
public class SocketServerApplication {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(SocketServerApplication.class, args);
    }

    @Bean
    public SocketIOServer socketIOServer() {
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(8001);
        config.setOrigin("*");

        return new SocketIOServer(config);
    }
}


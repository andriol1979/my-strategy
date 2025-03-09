package com.vut.mystrategy.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.Socket;

@Slf4j
@Component
public class RedisEmbeddedConfig {
    private final Jedis jedis;
    private final RedisServer redisServer;

    private final String redisHost;
    private final Integer redisPort;

    @Autowired
    public RedisEmbeddedConfig(@Qualifier("redisHost") String redisHost,
                               @Qualifier("redisPort") Integer redisPort) throws Exception {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisServer = new RedisServer(redisPort);
        this.jedis = new Jedis(redisHost, redisPort);
    }

    @PostConstruct
    public void startRedis() throws Exception {
        log.info("Starting Embedded Redis on host {} and port {}", redisHost, redisPort);
        if (isPortInUse(redisHost, redisPort)) {
            log.warn("Port {} is in use, waiting for it to free up...", redisPort);
            Thread.sleep(2000); // Đợi 2s
            if (isPortInUse(redisHost, redisPort)) {
                throw new RuntimeException("Port " + redisPort + " is already in use. Please free it or use a different port.");
            }
        }

        redisServer.start();
        log.info("Embedded Redis started on host {} and port {}", redisHost, redisPort);
    }

    @PreDestroy
    public void stopRedis() throws Exception {
        jedis.close(); // Đóng Jedis trước
        log.info("Jedis connection closed");
        if (redisServer.isActive()) {
            redisServer.stop();
            log.info("Embedded Redis stopped");
            Thread.sleep(500); // Đợi 500ms để đảm bảo port được giải phóng
        }
    }

    @Bean
    public Jedis jedis() {
        return jedis;
    }

    // Hàm kiểm tra port
    private boolean isPortInUse(String host, int port) {
        try (Socket ignored = new Socket(host, port)) {
            return true; // Port đang được dùng
        }
        catch (IOException e) {
            return false; // Port trống
        }
    }
}

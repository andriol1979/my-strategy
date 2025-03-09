package com.vut.mystrategy.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Supplier;

@Slf4j
@Component
public class RedisEmbeddedConfig {
    @Getter
    private Jedis jedis;
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

        // Thêm shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM shutting down, stopping Redis");
            stopRedis();
        }));
    }

    @PreDestroy
    public void stopRedis() {
        try {
            jedis.close();
            log.info("Jedis connection closed");
        } catch (Exception e) {
            log.error("Error closing Jedis", e);
        }

        try {
            redisServer.stop();
            log.info("Embedded Redis stopped");
        } catch (Exception e) {
            log.error("Error stopping RedisServer", e);
        }

        // Đợi và kiểm tra port
        try {
            Thread.sleep(3000); // Đợi 3s để process dừng
            if (isPortInUse(redisHost, redisPort)) {
                log.warn("Port {} still in use after stop, forcing kill", redisPort);
                killExistingRedisProcess();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void restartRedis() {
        log.info("Restarting embedded Redis due to pipeline error");
        try {
            stopRedis();
            startRedis();
        }
        catch (Exception e) {
            log.error("Failed to restart Redis {}", e.getMessage());
            throw new RuntimeException("Redis restart failed", e);
        }
    }

    // Method wrapper để retry và restart
    public <T> T executeWithRetry(Supplier<T> redisOperation) {
        int maxAttempts = 3;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                return redisOperation.get(); // Thực thi lệnh Redis
            }
            catch (JedisConnectionException e) {
                attempt++;
                log.error("JedisConnectionException on attempt {}: {}", attempt, e.getMessage());
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Failed after " + maxAttempts + " attempts", e);
                }
                if (attempt == 2) { // Restart sau lần retry thứ 2 thất bại
                    restartRedis();
                    jedis = new Jedis(redisHost, redisPort); // Tạo Jedis mới sau restart
                }
                try {
                    Thread.sleep(1000L * attempt); // Backoff: 1s, 2s
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RuntimeException("Unexpected failure in retry logic");
    }

    private void killExistingRedisProcess() {
        try {
            String command = String.format("lsof -i :%d -t | xargs kill -9", redisPort);
            Process process = Runtime.getRuntime().exec(new String[]{"bash", "-c", command});
            process.waitFor();
            log.info("Killed existing Redis process on port {}", redisPort);
        } catch (Exception e) {
            log.error("Failed to kill existing Redis process", e);
        }
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

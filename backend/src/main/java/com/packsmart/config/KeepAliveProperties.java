package com.packsmart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Optional keep-alive for free hosting that sleeps when idle. Off by default because always-on free services use up
 * the monthly free hours; turn it on for demo days. Hours are in IST (Asia/Kolkata), start inclusive, end exclusive.
 */
@Data
@ConfigurationProperties(prefix = "keepalive")
public class KeepAliveProperties {
    private boolean enabled = false;
    private String url;
    private int startHourIst = 8;
    private int endHourIst = 22;
}

package com.packsmart.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.packsmart.config.AppProperties;
import com.packsmart.config.KeepAliveProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Pings {@code FRONTEND_URL/api/health} on a schedule. The frontend proxies that call back to this backend, so both
 * free services receive traffic and stay awake. Only active when {@code KEEPALIVE_ENABLED=true} and inside the IST window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeepAliveService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final KeepAliveProperties props;
    private final AppProperties app;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    /** True when {@code hour} lies in [start, end) - also for windows that wrap past midnight (e.g. 20 to 6). */
    public static boolean inWindow(int hour, int start, int end) {
        if (start == end) {
            return true;
        }
        return start < end ? hour >= start && hour < end : hour >= start || hour < end;
    }

    @Scheduled(fixedDelayString = "${keepalive.interval-ms:600000}", initialDelayString = "${keepalive.initial-delay-ms:120000}")
    public void ping() {
        if (!props.isEnabled() || !inWindow(ZonedDateTime.now(IST).getHour(), props.getStartHourIst(), props.getEndHourIst())) {
            return;
        }
        String target = (props.getUrl() != null && !props.getUrl().isBlank() ? props.getUrl() : app.frontendBase() + "/api/health");
        try {
            HttpResponse<Void> r = http.send(HttpRequest.newBuilder(URI.create(target)).timeout(Duration.ofSeconds(90)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            log.debug("Keep-alive ping {} -> {}", target, r.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.info("Keep-alive ping to {} failed: {}", target, e.toString());
        }
    }
}

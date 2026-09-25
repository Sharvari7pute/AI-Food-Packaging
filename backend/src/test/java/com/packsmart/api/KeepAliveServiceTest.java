package com.packsmart.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.packsmart.service.KeepAliveService;
import org.junit.jupiter.api.Test;

class KeepAliveServiceTest {

    @Test
    void dayWindow() {
        assertThat(KeepAliveService.inWindow(8, 8, 22)).isTrue();
        assertThat(KeepAliveService.inWindow(21, 8, 22)).isTrue();
        assertThat(KeepAliveService.inWindow(22, 8, 22)).isFalse();
        assertThat(KeepAliveService.inWindow(3, 8, 22)).isFalse();
    }

    @Test
    void windowAcrossMidnight() {
        assertThat(KeepAliveService.inWindow(23, 20, 6)).isTrue();
        assertThat(KeepAliveService.inWindow(5, 20, 6)).isTrue();
        assertThat(KeepAliveService.inWindow(12, 20, 6)).isFalse();
    }

    @Test
    void equalBoundsMeansAllDay() {
        assertThat(KeepAliveService.inWindow(13, 0, 0)).isTrue();
    }
}

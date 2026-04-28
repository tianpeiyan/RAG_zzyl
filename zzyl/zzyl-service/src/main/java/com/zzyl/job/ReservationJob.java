package com.zzyl.job;

import com.zzyl.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author itheima
 */
@Component
@Slf4j
public class ReservationJob {

    @Autowired
    private ReservationService reservationService;

    /**
     * 预约过期修改<br>
     * 每分钟执行一次，检查并更新超过1小时未报道的预约
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateReservationStatus() {
        log.info("预约状态-过期修改-begin");
        // 获取当前时间减去1小时，只有预约时间在1小时前且仍为“待报道”的才会被置为过期
        reservationService.updateReservationStatus(LocalDateTime.now().minusHours(1));
        log.info("预约状态-过期修改-end");
    }
}

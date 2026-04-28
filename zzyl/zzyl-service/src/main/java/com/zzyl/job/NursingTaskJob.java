package com.zzyl.job;

import com.zzyl.entity.*;
import com.zzyl.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 护理任务定时生成 Job。
 * 根据老人护理等级对应的护理计划中的护理项目，按「任务生成逻辑规则」生成计划内任务。
 * 规则：每日（按开始时间与频次计算当日多时段）、每周（按入住星期与频次计算星期几）、每月（按入住日与频次计算每月几日）。
 */
@Component
public class NursingTaskJob {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private CheckInMapper checkInMapper;
    @Autowired
    private CheckInConfigMapper checkInConfigMapper;
    @Autowired
    private NursingLevelMapper nursingLevelMapper;
    @Autowired
    private NursingProjectPlanMapper nursingProjectPlanMapper;
    @Autowired
    private NursingTaskMapper nursingTaskMapper;
    @Autowired
    private NursingElderMapper nursingElderMapper;
    @Autowired
    private BedMapper bedMapper;

    /**
     * 每天 0 点 5 分执行，生成当日的计划内护理任务
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void generateDailyTasks() {
        List<CheckIn> activeCheckIns = checkInMapper.selectActiveList();
        if (activeCheckIns == null || activeCheckIns.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        int dayOfWeek = today.getDayOfWeek().getValue();
        int dayOfMonth = today.getDayOfMonth();
        int lastDayOfMonth = today.lengthOfMonth();

        for (CheckIn checkIn : activeCheckIns) {
            if (checkIn.getElderId() == null || checkIn.getCheckInCode() == null) {
                continue;
            }
            Elder elder = elderMapper.selectByPrimaryKey(checkIn.getElderId());
            if (elder == null || (elder.getStatus() != null && elder.getStatus() == 5)) {
                continue;
            }

            CheckInConfig config = checkInConfigMapper.selectByCheckInCode(checkIn.getCheckInCode());
            if (config == null || config.getNursingLevelId() == null) {
                continue;
            }
            LocalDate checkInDate;
            if (config.getCheckInStartTime() != null) {
                checkInDate = config.getCheckInStartTime().toLocalDate();
            } else if (checkIn.getCheckInTime() != null) {
                checkInDate = checkIn.getCheckInTime().toLocalDate();
            } else {
                checkInDate = today;
            }

            NursingLevel level = nursingLevelMapper.getById(config.getNursingLevelId());
            if (level == null || level.getPlanId() == null) {
                continue;
            }

            List<NursingProjectPlan> projectPlans = nursingProjectPlanMapper.listAllByPlanId(level.getPlanId());
            if (projectPlans == null || projectPlans.isEmpty()) {
                continue;
            }

            String bedNumber = getBedNumber(elder);
            Long nursingId = getPrimaryNursingId(elder.getId());

            for (NursingProjectPlan pp : projectPlans) {
                Integer cycle = pp.getExecuteCycle() != null ? pp.getExecuteCycle() : 0;
                int frequency = pp.getExecuteFrequency() != null && pp.getExecuteFrequency() > 0
                        ? pp.getExecuteFrequency() : 1;
                String executeTimeStr = pp.getExecuteTime();
                if (executeTimeStr == null || executeTimeStr.isEmpty()) {
                    continue;
                }

                LocalTime planStartTime;
                try {
                    planStartTime = LocalTime.parse(executeTimeStr, TIME_FORMAT);
                } catch (Exception e) {
                    try {
                        planStartTime = LocalTime.parse(executeTimeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
                    } catch (Exception e2) {
                        continue;
                    }
                }

                List<LocalDateTime> estimatedTimes = new ArrayList<>();

                if (cycle == 0) {
                    // 每日：任务生成日期=每日；期望服务时间间隔=(24-开始时间小时)/执行频次(取整)；多个时段递推
                    int startHour = planStartTime.getHour();
                    int intervalHours = (24 - startHour) / frequency;
                    if (intervalHours <= 0) {
                        intervalHours = 1;
                    }
                    for (int k = 0; k < frequency; k++) {
                        int hour = startHour + k * intervalHours;
                        if (hour >= 24) {
                            break;
                        }
                        LocalTime slot = LocalTime.of(hour, planStartTime.getMinute(), planStartTime.getSecond());
                        estimatedTimes.add(LocalDateTime.of(today, slot));
                    }
                } else if (cycle == 1) {
                    // 每周：任务生成日期间隔=7/执行频次(取整)；按入住日星期递推；期望服务时间=计划执行开始时间
                    int dateInterval = 7 / frequency;
                    if (dateInterval <= 0) {
                        dateInterval = 1;
                    }
                    int checkInDOW = checkInDate.getDayOfWeek().getValue();
                    Set<Integer> taskWeekdays = new HashSet<>();
                    for (int k = 0; k < frequency; k++) {
                        int d = (checkInDOW - 1 + k * dateInterval) % 7 + 1;
                        taskWeekdays.add(d);
                    }
                    if (taskWeekdays.contains(dayOfWeek)) {
                        estimatedTimes.add(LocalDateTime.of(today, planStartTime));
                    }
                } else if (cycle == 2) {
                    // 每月：任务生成日期间隔=30/执行频次(取整)；按入住日递推；期望服务时间=计划执行开始时间
                    int dateInterval = 30 / frequency;
                    if (dateInterval <= 0) {
                        dateInterval = 1;
                    }
                    int checkInDay = checkInDate.getDayOfMonth();
                    Set<Integer> taskDays = new HashSet<>();
                    for (int k = 0; k < frequency; k++) {
                        int d = checkInDay + k * dateInterval;
                        if (d <= lastDayOfMonth) {
                            taskDays.add(d);
                        }
                    }
                    if (taskDays.contains(dayOfMonth)) {
                        estimatedTimes.add(LocalDateTime.of(today, planStartTime));
                    }
                }

                for (LocalDateTime estimatedTime : estimatedTimes) {
                    if (nursingTaskMapper.countByElderIdAndProjectIdAndEstimatedTime(elder.getId(), pp.getProjectId(), estimatedTime) > 0) {
                        continue;
                    }
                    NursingTask task = new NursingTask();
                    task.setTaskType(2);
                    task.setStatus(1);
                    task.setElderId(elder.getId());
                    task.setProjectId(pp.getProjectId());
                    task.setBedNumber(bedNumber);
                    task.setNursingId(nursingId);
                    task.setEstimatedServerTime(estimatedTime);
                    task.setCreateTime(LocalDateTime.now());
                    task.setCreateBy(1L);
                    task.setRemark(cycle == 0 ? "每日任务" : (cycle == 1 ? "每周任务" : "每月任务"));
                    nursingTaskMapper.insert(task);
                }
            }
        }
    }

    private String getBedNumber(Elder elder) {
        if (elder == null) {
            return null;
        }
        if (elder.getBedNumber() != null && !elder.getBedNumber().isEmpty()) {
            return elder.getBedNumber();
        }
        if (elder.getBedId() == null) {
            return null;
        }
        Bed bed = bedMapper.getById(elder.getBedId());
        return bed == null ? null : bed.getBedNumber();
    }

    private Long getPrimaryNursingId(Long elderId) {
        if (elderId == null) {
            return null;
        }
        List<Long> nursingIds = nursingElderMapper.selectNursingIdsByElderId(elderId);
        if (nursingIds == null || nursingIds.isEmpty()) {
            return null;
        }
        return nursingIds.get(0);
    }
}

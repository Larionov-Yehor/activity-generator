package com.in6k;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Calendar;
import java.util.concurrent.ThreadLocalRandom;

import static java.time.temporal.ChronoUnit.DAYS;

@RestController
@RequestMapping("/")
public class PublicInterface {

    final int projectId = 1;
    final int processId = 2;
    final boolean isAutomatic = true;

    final String window_title = "window";
    final String timeZone = "Europe/Kiev";
    final int offset = 2;

    final int startOfWorkingDay = 9;
    final int endOfWorkingDay = 18;

    final int minMinute = 0;
    final int maxMinute = 59;

    final int minOnlineDuration = 2;
    final int maxOnlineDuration = 9;
    final int minOfflineDuration = 10;
    final int maxOfflineDuration = 40;

    final LocalTime dinnerStart = LocalTime.of(13, 0, 0);
    final LocalTime dinnerEnd = LocalTime.of(14, 0 , 0);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @RequestMapping("/")
    public String generateData(
          @RequestParam(value = "domain_id") Integer domainId,
          @RequestParam(value = "user_id") Integer userId,
          @RequestParam(value = "date_from") String dateFrom,
          @RequestParam(value = "date_to") String dateTo
    ) throws ParseException {

        Long days = DAYS.between(getDate(dateFrom).toLocalDate(), getDate(dateTo).toLocalDate());
        insertData(1, 1, dateFrom, dateTo);
        return "Activities were generated for domainId: " + domainId + "   userId: " + userId + "   from: " + dateFrom + "   to: " + dateTo + "   number of days: " + days;
    }

    private void insertData(Integer domainId, Integer userId, String dateFrom, String dateTo) throws ParseException {

        Long days = DAYS.between(getDate(dateFrom).toLocalDate(), getDate(dateTo).toLocalDate());
        int day = 0;

        Date date = getDate(dateFrom);

        while (day <= days) {

            int randomHour = ThreadLocalRandom.current().nextInt(startOfWorkingDay - 1, startOfWorkingDay + 1);
            int randomMinute = ThreadLocalRandom.current().nextInt(minMinute, maxMinute);
            Integer screenshot_id = 1;

            LocalTime timeBegin = LocalTime.of(randomHour, randomMinute, 0);

            while (timeBegin.getHour() < endOfWorkingDay) {

                boolean isOffline = Math.random() < 0.04;
                int duration = countActivityDuration(isOffline);
                LocalTime timeEnd = timeBegin.plusMinutes(duration);

                if (crossesDinner(timeBegin, timeEnd)) {
                    timeBegin = timeEnd;
                    continue;
                }

                Timestamp beginTimeStamp = getTimestamp(date, timeBegin);
                Timestamp endTimeStamp = getTimestamp(date, timeEnd);

                String INSERT_QUERY = "insert into application_activities(time_start,time_end,time_zone,offset,process_id," +
                        "domain_id,user_id,project_id,is_offline,is_automatic,duration,screenshot_id,window_title) " +
                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?); ";

                screenshot_id++;

                if(screenshot_id >= 65) {
                    screenshot_id = 1;
                }

                jdbcTemplate.update(INSERT_QUERY, beginTimeStamp, endTimeStamp, timeZone, offset, processId, domainId, userId,
                        projectId, isOffline, isAutomatic, duration, screenshot_id.toString(), window_title
                );

                timeBegin = timeEnd;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DATE, 1);
            date = new java.sql.Date(cal.getTimeInMillis());

            day++;
        }
    }

    private int countActivityDuration(boolean isOffline) {
        if (isOffline) {
            return ThreadLocalRandom.current().nextInt(minOfflineDuration, maxOfflineDuration + 1);
        }
        return ThreadLocalRandom.current().nextInt(minOnlineDuration, maxOnlineDuration + 1);
    }

    private Date getDate(String date) throws ParseException {
        String pattern = "yyyy-MM-dd";
        return new Date(new SimpleDateFormat(pattern).parse(date).getTime());
    }

    private Timestamp getTimestamp(Date d, LocalTime localTime) {
        return Timestamp.valueOf(LocalDateTime.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(d.getTime()),
                ZoneId.systemDefault()).toLocalDate(), localTime));
    }

    private boolean crossesDinner(LocalTime timeBegin, LocalTime timeEnd) {
        return (timeBegin.isAfter(dinnerStart) && timeBegin.isBefore(dinnerEnd)) ||
                (timeEnd.isAfter(dinnerStart) && timeEnd.isBefore(dinnerEnd)) ||
                (timeBegin.isBefore(dinnerStart) && timeEnd.isAfter(dinnerEnd)) ||
                timeBegin.equals(dinnerStart) ||
                timeEnd.equals(dinnerEnd);
    }
}

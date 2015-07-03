package org.jboss.test.singletonservice;

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * MyTimer should run only once in the whole cluster: started by TestingSingletonService, which runs only once in the whole cluster.
 */
@Singleton
public class MyTimerBean implements MyTimer {
    private static final Logger LOGGER = Logger.getLogger(MyTimerBean.class.getName());

    @Resource
    private TimerService timerService;

    @Timeout
    public void scheduler(Timer timer) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        LOGGER.info(timer.getInfo() + " " + df.format(new Date()));
    }

    @Override
    public void initialize(String info) {
        // every 1s
        ScheduleExpression scheduleExpression = new ScheduleExpression().hour("*").minute("*").second("0/1");
        timerService.createCalendarTimer(scheduleExpression, new TimerConfig(info, false));
    }

    @Override
    public void stop() {
        LOGGER.info("Stopping timer");
        for (Timer timer : timerService.getTimers()) {
            LOGGER.info("Stop timer: " + timer.getInfo());
            timer.cancel();
        }
    }
}

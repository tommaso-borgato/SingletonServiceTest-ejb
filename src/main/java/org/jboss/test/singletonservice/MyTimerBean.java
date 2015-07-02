package org.jboss.test.singletonservice;

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.logging.Level;
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
        LOGGER.log(Level.INFO, "MyTimer says: " + timer.getInfo());
    }

    @Override
    public void initialize(String info) {
        // every 1s
        ScheduleExpression scheduleExpression = new ScheduleExpression().hour("*").minute("*").second("0/1");
        timerService.createCalendarTimer(scheduleExpression, new TimerConfig(info, false));
    }

    @Override
    public void stop() {
        LOGGER.log(Level.INFO, "Stopping timer.");
        for (Timer timer : timerService.getTimers()) {
            LOGGER.log(Level.INFO, "Stop timer: " + timer.getInfo());
            timer.cancel();
        }
    }
}

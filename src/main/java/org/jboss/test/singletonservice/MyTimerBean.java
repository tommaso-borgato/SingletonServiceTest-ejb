package org.jboss.test.singletonservice;

import javax.annotation.Resource;
import javax.ejb.ScheduleExpression;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This is a known workaround for clustered timers. SingletonService will start this MyTimer.
 */
@Singleton
public class MyTimerBean implements MyTimer {
    private static final Logger LOGGER = Logger.getLogger(MyTimerBean.class.getName());
    @Resource
    private TimerService timerService;
    
    @Timeout
    public void scheduler(javax.ejb.Timer timer) {
        LOGGER.log(Level.INFO,"MyTimer says:"+timer.getInfo());
    }
    
    @Override
    public void initialize(String info) {
        ScheduleExpression scheduleExpression = new ScheduleExpression();
        //every 1s
        scheduleExpression.hour("*").minute("*").second("0/1");
        timerService.createCalendarTimer(scheduleExpression,new TimerConfig(info, false));
    }
    
    @Override
    public void stop() {
        LOGGER.log(Level.INFO,"Stopping timer.");
        for (javax.ejb.Timer timer : timerService.getTimers()) {
            LOGGER.log(Level.INFO,"Stop timer: "+timer.getInfo());
            timer.cancel();
        }
    }
}

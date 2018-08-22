package org.jboss.test.singletonservice;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.group.Group;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HaSingletonService implements Service {
    private static final Logger LOGGER = Logger.getLogger(HaSingletonService.class.getName());

    public static final ServiceName SINGLETON_SERVICE_NAME = ServiceName.parse("org.jboss.test.singletonservice.testing-singleton-service");

    private Supplier<Group> group;
    private Consumer<String> consumer;

    private AtomicBoolean started = new AtomicBoolean(false);

    public HaSingletonService(Supplier<Group> group, Consumer<String> consumer) {
        this.group = group;
        this.consumer = consumer;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        LOGGER.info(
                 "\n====================================================================\n" +
                        "=================== HA Singleton Service ===========================\n" +
                        "====================================================================\n" +
                        "HA Singleton Service '" + this.getClass().getName() + "' START on " + group.get().getLocalMember().getName() +
                     "'\n====================================================================");

        this.consumer.accept(group.get().getLocalMember().getName());

        LOGGER.info("\n====================================================================\n" +
                "HA Singleton Service '" + this.getClass().getName() + "' STARTED on " + group.get().getLocalMember().getName() +
                "\n====================================================================");

        try {
            LOGGER.info("\n==================================\n==================================\n==================================\n" +
                    "HA Singleton Service " + this.getClass().getName() + " starts " + MyTimerBean.class.getName() + " on node " + group.get().getLocalMember().getName() +
                    "\n==================================\n==================================\n==================================");
/*
2018-09-27 18:49:33,731 INFO  [org.jboss.as.ejb3.deployment] (MSC service thread 1-8) WFLYEJB0473: JNDI bindings for session bean named 'MyTimerBean' in deployment unit 'deployment "SStest-ejb-service-activator.jar"' are as follows:

	java:global/SStest-ejb/MyTimerBean!org.jboss.test.singletonservice.MyTimer
	java:app/SStest-ejb/MyTimerBean!org.jboss.test.singletonservice.MyTimer
	java:module/MyTimerBean!org.jboss.test.singletonservice.MyTimer
	ejb:SStest-ejb/MyTimerBean!org.jboss.test.singletonservice.MyTimer
	java:global/SStest-ejb/MyTimerBean
	java:app/SStest-ejb/MyTimerBean
	java:module/MyTimerBean
 */
            InitialContext ic = new InitialContext();
            MyTimer myTimer = (MyTimer) ic.lookup("java:global/SStest-ejb/MyTimerBean");
            myTimer.initialize("timer @" + group.get().getName());
        } catch (NamingException e) {
            throw new StartException("Could not initialize MyTimer", e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        if (!started.compareAndSet(true, false)) {
            LOGGER.info("The service '" + this.getClass().getName() + "' is not active!");
        } else {
            LOGGER.info("\n==================================\n" +
                    "HA Singleton Service '" + this.getClass().getName() + "' STOP on " + group.get().getLocalMember().getName() +
                    "\n==================================");

            try {
                InitialContext ic = new InitialContext();
                MyTimer myTimer = (MyTimer) ic.lookup("java:global/SStest-ejb/MyTimerBean");
                myTimer.stop();
            } catch (NamingException e) {
                LOGGER.log(Level.SEVERE, "Could not stop MyTimer", e);
            }
        }
    }
}

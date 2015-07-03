package org.jboss.test.singletonservice;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestingSingletonService implements Service<String> {
    private static final Logger LOGGER = Logger.getLogger(TestingSingletonService.class.getName());

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("test", "myservice");

    final InjectedValue<ServerEnvironment> env = new InjectedValue<ServerEnvironment>();

    private AtomicBoolean started = new AtomicBoolean(false);
    private String nodeName;

    @Override
    public String getValue() {
        if (!started.get()) {
            throw new IllegalStateException();
        }
        return this.nodeName;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        if (!started.compareAndSet(false, true)) {
            throw new StartException("The service is already started!");
        }

        LOGGER.info("Start service '" + this.getClass().getName() + "'");

        this.nodeName = this.env.getValue().getNodeName();

        LOGGER.info("Service started on " + this.getValue());

        try {
            InitialContext ic = new InitialContext();
            MyTimer myTimer = (MyTimer) ic.lookup("java:global/SStest-ejb/MyTimerBean");
            myTimer.initialize("timer @" + this.nodeName);
        } catch (NamingException e) {
            throw new StartException("Could not initialize MyTimer", e);
        }
    }

    @Override
    public void stop(StopContext stopContext) {
        if (!started.compareAndSet(true, false)) {
            LOGGER.info("The service '" + this.getClass().getName() + "' is not active!");
        } else {
            LOGGER.info("Stop service '" + this.getClass().getName() + "' on " + this.nodeName);

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

package org.jboss.test.singletonservice;

import org.jboss.as.clustering.singleton.SingletonService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class SingletonStartupBean {
    private static final Logger LOGGER = Logger.getLogger(SingletonStartupBean.class.getName());

    @PostConstruct
    protected void startup() {
        LOGGER.info("SingletonStartupBean will be activated");

        TestingSingletonService service = new TestingSingletonService();
        SingletonService<String> singleton = new SingletonService<String>(service, TestingSingletonService.SERVICE_NAME);
        ServiceController<String> controller = singleton.build(CurrentServiceContainer.getServiceContainer())
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.env)
                .install();

        controller.setMode(ServiceController.Mode.ACTIVE);
        try {
            wait(controller, EnumSet.of(ServiceController.State.DOWN, ServiceController.State.STARTING), ServiceController.State.UP);
            LOGGER.info("SingletonStartupBean has activated the TestingSingletonService");
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "TestingSingletonService not started", e);
        }
    }

    @PreDestroy
    protected void destroy() {
        LOGGER.info("TestingSingletonServiceBean will be removed");
        ServiceController controller = CurrentServiceContainer.getServiceContainer().getRequiredService(TestingSingletonService.SERVICE_NAME);
        controller.setMode(ServiceController.Mode.REMOVE);
        try {
            wait(controller, EnumSet.of(ServiceController.State.UP, ServiceController.State.STOPPING, ServiceController.State.DOWN), ServiceController.State.REMOVED);
            LOGGER.info("SingletonStartupBean has removed the TestingSingletonService");
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "TestingSingletonService not removed", e);
        }
    }

    private static <T> void wait(ServiceController<T> controller, Collection<ServiceController.State> expectedStates, ServiceController.State targetState) {
        if (controller.getState() != targetState) {
            ServiceListener<T> listener = new NotifyingServiceListener<T>();
            controller.addListener(listener);
            try {
                synchronized (controller) {
                    int maxRetry = 5;
                    while ((expectedStates.contains(controller.getState())) && (maxRetry > 0)) {
                        LOGGER.info("Service controller state is " + controller.getState() + ", waiting for transition to " + targetState);
                        controller.wait(5000L);
                        maxRetry--;
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.info("Waiting for transition to " + targetState + " interrupted");
                Thread.currentThread().interrupt();
            }
            controller.removeListener(listener);
            ServiceController.State state = controller.getState();
            LOGGER.info("Service controller state is now " + state);
            if (state != targetState)
                throw new IllegalStateException("Failed to wait for state to transition to " + targetState
                        + ". Current state is " + state + ". " + controller.getStartException());
        }
    }

    private static class NotifyingServiceListener<T> extends AbstractServiceListener<T> {
        public void transition(ServiceController<? extends T> controller, ServiceController.Transition transition) {
            synchronized (controller) {
                controller.notify();
            }
        }
    }
}

package org.jboss.test.singletonservice;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

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

    private static final String CACHE_CONTAINER_NAME = "server";
    private static final String CACHE_NAME = "default";

    @PostConstruct
    protected void startup() {
        LOGGER.info("SingletonStartupBean will be activated");

        TestingSingletonService service = new TestingSingletonService();
        ServiceContainer serviceContainer = CurrentServiceContainer.getServiceContainer();
        SingletonServiceBuilderFactory factory = (SingletonServiceBuilderFactory) serviceContainer
                .getRequiredService(SingletonServiceBuilderFactory.SERVICE_NAME.append(CACHE_CONTAINER_NAME, CACHE_NAME))
                .getValue();
        ServiceController<String> controller = factory.createSingletonServiceBuilder(TestingSingletonService.SERVICE_NAME, service)
                .electionPolicy(new SimpleSingletonElectionPolicy())
                .requireQuorum(1)
                .build(serviceContainer)
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
        LOGGER.info("TestingSingletonService will be removed");
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
                        + ". Current state is " + state + ". Possible start exception:" + controller.getStartException());
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

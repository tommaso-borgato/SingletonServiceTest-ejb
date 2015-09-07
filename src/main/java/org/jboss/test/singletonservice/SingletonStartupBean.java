package org.jboss.test.singletonservice;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StabilityMonitor;
import org.wildfly.clustering.singleton.SingletonServiceBuilderFactory;
import org.wildfly.clustering.singleton.SingletonServiceName;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Singleton
@Startup
public class SingletonStartupBean {
    private static final Logger LOGGER = Logger.getLogger(SingletonStartupBean.class.getName());

    private static final String CACHE_CONTAINER_NAME = "server";

    @PostConstruct
    protected void startup() {
        LOGGER.info("TestingSingletonService will be activated");

        TestingSingletonService service = new TestingSingletonService();
        ServiceContainer serviceContainer = CurrentServiceContainer.getServiceContainer();
        SingletonServiceBuilderFactory factory = (SingletonServiceBuilderFactory) serviceContainer
                .getRequiredService(SingletonServiceName.BUILDER.getServiceName(CACHE_CONTAINER_NAME))
                .getValue();
        ServiceController<String> controller = factory.createSingletonServiceBuilder(TestingSingletonService.SERVICE_NAME, service)
                .electionPolicy(new SimpleSingletonElectionPolicy())
                .requireQuorum(1)
                .build(serviceContainer)
                .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.env)
                .install();

        moveToStableState(controller, ServiceController.Mode.ACTIVE, ServiceController.State.UP);
    }

    @PreDestroy
    protected void destroy() {
        LOGGER.info("TestingSingletonService will be removed");
        ServiceController controller = CurrentServiceContainer.getServiceContainer().getService(TestingSingletonService.SERVICE_NAME);
        if (controller == null) {
            LOGGER.warning("TestingSingletonService not found, removed already?");
            return;
        }

        moveToStableState(controller, ServiceController.Mode.REMOVE, ServiceController.State.REMOVED);
    }

    private void moveToStableState(ServiceController controller, ServiceController.Mode targetMode, ServiceController.State targetState) {
        StabilityMonitor stabilityMonitor = new StabilityMonitor();
        stabilityMonitor.addController(controller);
        controller.setMode(targetMode);
        try {
            stabilityMonitor.awaitStability(10, TimeUnit.SECONDS);
            if (controller.getState() != targetState) {
                LOGGER.severe("TestingSingletonService didn't move to " + targetState);
            } else {
                LOGGER.info("TestingSingletonService moved to " + targetState);
            }
        } catch (InterruptedException e) {
            LOGGER.info("Waiting for transition to " + targetState + " interrupted");
            Thread.currentThread().interrupt();
        } finally {
            stabilityMonitor.removeController(controller);
        }
    }
}

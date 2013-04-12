package org.jboss.test.singletonservice;

import org.jboss.as.clustering.singleton.SingletonService;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceListener;


import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Collection;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Created with IntelliJ IDEA.
 * User: jkudrnac
 */
@Singleton
@Startup
public class SingletonStartupBean {
        private static final Logger LOGGER = Logger.getLogger(SingletonStartupBean.class.getName());

  @PostConstruct
  protected void startup() {
    LOGGER.info("SingletonStartupBean will be initialized!");

    TestingSingletonService service = new TestingSingletonService();
    SingletonService singleton = new SingletonService(service, TestingSingletonService.SERVICE_NAME);
    ServiceController controller = singleton.build(CurrentServiceContainer.getServiceContainer()).addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.env).install();

    controller.setMode(ServiceController.Mode.ACTIVE);
    try {
      wait(controller, EnumSet.of(ServiceController.State.DOWN, ServiceController.State.STARTING), ServiceController.State.UP);
      LOGGER.info("SingletonStartupBean has started the TestingSingletonService");
    } catch (IllegalStateException e) {
      LOGGER.info("Singleton Service not started!" + TestingSingletonService.SERVICE_NAME);
      LOGGER.log(Level.SEVERE,e.getMessage());

    }
  }

  @PreDestroy
  protected void destroy()
  {
    LOGGER.info("TestingSingletonServiceBean will be removed!");
    ServiceController controller = CurrentServiceContainer.getServiceContainer().getRequiredService(TestingSingletonService.SERVICE_NAME);
    controller.setMode(ServiceController.Mode.REMOVE);
    try {
      wait(controller, EnumSet.of(ServiceController.State.UP, ServiceController.State.STOPPING, ServiceController.State.DOWN), ServiceController.State.REMOVED);
    } catch (IllegalStateException e) {
      LOGGER.info("TestingSingleton Service " + TestingSingletonService.SERVICE_NAME + " has not be stopped correctly!");
    }
  }

  private static <T> void wait(ServiceController<T> controller, Collection<ServiceController.State> expectedStates, ServiceController.State targetState) { if (controller.getState() != targetState) {
      ServiceListener listener = new NotifyingServiceListener();
      controller.addListener(listener);
      try {
        synchronized (controller) {
          int maxRetry = 5;
          while ((expectedStates.contains(controller.getState())) && (maxRetry > 0)) {
            LOGGER.info("Service controller state is "+ controller.getState() +", waiting for transition to " + targetState );
            controller.wait(5000L);
            maxRetry--;
          }
        }
      } catch (InterruptedException e) {
        LOGGER.info("Wait on startup is interrupted!");
        Thread.currentThread().interrupt();
      }
      controller.removeListener(listener);
      ServiceController.State state = controller.getState();
      LOGGER.info("Service controller state is now " + state);
      if (state != targetState)
        throw new IllegalStateException(("Failed to wait for state to transition to " + targetState +"  Current state is " + state +"" + controller.getStartException()));
    }
  }

  private static class NotifyingServiceListener<T> extends AbstractServiceListener<T>
  {
    public void transition(ServiceController<? extends T> controller, ServiceController.Transition transition)
    {
      synchronized (controller) {
        controller.notify();
      }
    }
  }
}
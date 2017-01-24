package org.jboss.test.singletonservice;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceController;

import javax.ejb.Stateless;
import java.util.logging.Logger;

@Stateless
public class ServiceAccessBean implements ServiceAccess {
    private static final Logger LOGGER = Logger.getLogger(ServiceAccessBean.class.getName());

    public String getNodeNameOfService() {
        LOGGER.info("getNodeNameOfService() is called");
        ServiceController<?> service = CurrentServiceContainer.getServiceContainer()
                .getService(TestingSingletonService.SERVICE_NAME);
        LOGGER.fine("Service: " + service);

        if (service != null) {
            String serviceValue = null;
            try {
                serviceValue = (String) service.awaitValue();
            } catch (IllegalStateException e) {
                LOGGER.info("Service was either removed or failed to start: " + e.getMessage());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            return serviceValue;
        } else {
            throw new IllegalStateException("Service '" + TestingSingletonService.SERVICE_NAME + "' not found!");
        }
    }
}

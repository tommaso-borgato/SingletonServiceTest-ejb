package org.jboss.test.singletonservice;

import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb3.annotation.Clustered;
import org.jboss.msc.service.ServiceController;

import javax.ejb.Stateless;
import java.util.logging.Logger;

@Stateless
@Clustered
public class ServiceAccessBean implements ServiceAccess {
    private static final Logger LOGGER = Logger.getLogger(ServiceAccessBean.class.getName());

    public String getNodeNameOfService() {
        LOGGER.info("getNodeNameOfService() is called");
        ServiceController<?> service = CurrentServiceContainer.getServiceContainer().getService(
                TestingSingletonService.SERVICE_NAME);
        LOGGER.fine("Service: " + service);

        if (service != null) {
            return (String) service.getValue();
        } else {
            throw new IllegalStateException("Service '" + TestingSingletonService.SERVICE_NAME + "' not found!");
        }
    }
}

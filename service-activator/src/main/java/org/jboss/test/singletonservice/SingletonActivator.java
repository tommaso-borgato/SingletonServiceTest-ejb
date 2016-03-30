package org.jboss.test.singletonservice;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.wildfly.clustering.singleton.SingletonPolicy;

import java.util.logging.Logger;

public class SingletonActivator implements ServiceActivator {
    private static final Logger LOGGER = Logger.getLogger(SingletonActivator.class.getName());

    private static final String SINGLETON_POLICY_NAME = "default";

    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        LOGGER.info("TestingSingletonService will be activated");

        TestingSingletonService service = new TestingSingletonService();

        try {
            SingletonPolicy policy = (SingletonPolicy) context.getServiceRegistry()
                    .getRequiredService(ServiceName.parse(SingletonPolicy.CAPABILITY_NAME).append(SINGLETON_POLICY_NAME))
                    .awaitValue();

            policy.createSingletonServiceBuilder(TestingSingletonService.SERVICE_NAME, service)
                    .build(context.getServiceTarget())
                    .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, service.env)
                    .install();
        } catch (InterruptedException e) {
            throw new ServiceRegistryException(e);
        }
    }
}

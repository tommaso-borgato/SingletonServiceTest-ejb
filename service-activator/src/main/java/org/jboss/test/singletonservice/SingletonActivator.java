package org.jboss.test.singletonservice;

import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.service.ActiveServiceSupplier;
import org.wildfly.clustering.singleton.SingletonDefaultRequirement;
import org.wildfly.clustering.singleton.SingletonPolicy;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Refer to: https://github.com/wildfly/wildfly/tree/master/testsuite/integration/clustering/src/test/java/org/jboss/as/test/clustering/cluster/singleton/service
 */
public class SingletonActivator implements ServiceActivator {
    private static final Logger LOGGER = Logger.getLogger(SingletonActivator.class.getName());


    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        LOGGER.info(String.format(
                "\n=====================================================================\n" +
                        "ACTIVATOR: TestingSingletonService will be activated with service name '%s'" +
                        "\n=====================================================================", HaSingletonService.SINGLETON_SERVICE_NAME));
        installWithCustomElectionPolicy(context);
    }

    /**
     * New API
     * @param context
     */
    private void installWithCustomElectionPolicy(ServiceActivatorContext context) {

        ServiceTarget target = context.getServiceTarget();
        SingletonPolicy policy = new ActiveServiceSupplier<SingletonPolicy>(
                context.getServiceRegistry(),
                ServiceName.parse(SingletonDefaultRequirement.POLICY.getName())
        ).setTimeout(Duration.ofSeconds(60)).get();
        ServiceBuilder<?> builder = policy
                .createSingletonServiceConfigurator(HaSingletonService.SINGLETON_SERVICE_NAME)
                .build(target);
        Consumer<String> nodeName = builder.provides(HaSingletonService.SINGLETON_SERVICE_NAME);
        Supplier<Group> group = builder.requires(ServiceName.parse("org.wildfly.clustering.default-group"));
        Service service = new HaSingletonService(
                group,
                nodeName);
        builder
                .setInstance(service)
                .install();
    }
}

package org.jboss.test.singletonservice;

import org.wildfly.clustering.group.Group;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
public class ServiceAccessBean implements ServiceAccess {
    private static final Logger LOGGER = Logger.getLogger(ServiceAccessBean.class.getName());

    @Resource(lookup = "java:jboss/clustering/group/default")
    private Group channelGroup;

    @EJB
    private HaSingletonServiceInvoker haSingletonServiceInvoker;

    public String getNodeNameOfService() {
        //LOGGER.info("====================================================================");
        //LOGGER.info("getNodeNameOfService() is called");
        final Map<String, String> nodes;

        String serviceValue = haSingletonServiceInvoker.invokeLocal();
        if (serviceValue != null) {
            LOGGER.info(String.format("Service %s is on current node (%s)", HaSingletonService.SINGLETON_SERVICE_NAME, channelGroup.getLocalMember()));
            //LOGGER.info("====================================================================");
            return serviceValue;
        } else {
            /**
             * CommandDispatcher API from WildFly to invokeRemote all nodes for the service
             */
            nodes = haSingletonServiceInvoker.invokeRemote();
        }
        //LOGGER.info("====================================================================");

        if (
                nodes.entrySet().stream().filter(stringStringEntry -> stringStringEntry.getValue() != null).count() != 1
        ) {
            LOGGER.severe(
                    String.format("Service %s active on %d nodes: %s!",
                            HaSingletonService.SINGLETON_SERVICE_NAME,
                            nodes.entrySet().stream()
                                    .filter(stringStringEntry -> stringStringEntry.getValue() != null)
                                    .count(),
                            nodes.entrySet().stream()
                                    .filter(stringStringEntry -> stringStringEntry.getValue() != null)
                                    .map(stringStringEntry -> stringStringEntry.getValue())
                                    .collect(Collectors.joining(","))
                    ));
        }
        return nodes.isEmpty() ?
                null
                :
                nodes.entrySet().stream()
                        .filter(stringStringEntry -> stringStringEntry.getValue() != null)
                        .map(stringStringEntry -> stringStringEntry.getValue())
                        .collect(Collectors.joining(","));
    }
}

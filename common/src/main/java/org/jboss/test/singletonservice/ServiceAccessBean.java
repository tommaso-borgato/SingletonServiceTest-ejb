package org.jboss.test.singletonservice;

import org.jboss.as.server.CurrentServiceContainer;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.service.PassiveServiceSupplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

@Stateless
public class ServiceAccessBean implements ServiceAccess, LocalContext {
    private static final Logger LOGGER = Logger.getLogger(ServiceAccessBean.class.getName());

    @Resource(lookup = "java:jboss/clustering/group/default")
    private Group channelGroup;

    @Resource(lookup = "java:jboss/clustering/dispatcher/default") // <1>
    private CommandDispatcherFactory factory;
    private CommandDispatcher<LocalContext> dispatcher;

    @PostConstruct
    public void init() {
        /**
         * CommandDispatcher API from WildFly to query all nodes for the service
         */
        this.dispatcher = this.factory.createCommandDispatcher(this.getClass().getName(), this);
    }

    @PreDestroy
    public void destroy() {
        this.dispatcher.close();
    }

    @Override
    public String getServiceValue(String sourceNode){
        LOGGER.info(String.format("getServiceValue(%s)...",sourceNode));
        String serviceValue = new PassiveServiceSupplier<String>(
                CurrentServiceContainer.getServiceContainer(),
                HaSingletonService.SINGLETON_SERVICE_NAME).get();
        LOGGER.info(String.format("Responding to node %s: %s",sourceNode,serviceValue));
        return serviceValue;
    }

    public String getNodeNameOfService() {
        LOGGER.info("====================================================================");
        LOGGER.info("getNodeNameOfService() is called");
        final List<String> nodes = new ArrayList<>();

        String serviceValue = getServiceValue(channelGroup.getLocalNode().getName());
        if (serviceValue != null)
        {
            LOGGER.info(String.format("Service %s is on current node (%s)", HaSingletonService.SINGLETON_SERVICE_NAME, channelGroup.getLocalNode()));
            LOGGER.info("====================================================================");
            return serviceValue;
        } else {
            try {
                Map<Node, CompletionStage<String>> responses = this.dispatcher.executeOnGroup(
                        new TestCommand(
                                channelGroup.getLocalNode().getName()
                        ),
                        channelGroup.getLocalNode()
                );
                for (Map.Entry<Node, CompletionStage<String>> entry : responses.entrySet()) {
                    try {
                        String serviceName = entry.getValue().toCompletableFuture().join();
                        LOGGER.info(String.format("Response from node %s: service value on that node is <<%s>>", entry.getKey().getName(), serviceName));
                        nodes.add(serviceName);
                    } catch (Exception e) {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        LOGGER.severe(String.format("Error:\n%s", errors));
                    }
                }

            } catch (CommandDispatcherException e) {
                LOGGER.severe(String.format("Error in executeOnGroup: %s", e.getMessage()));
            }
        }
        LOGGER.info("====================================================================");

        if (nodes==null || nodes.isEmpty()) return null;

        for (String node: nodes){
            if (node!=null) return node;
        }

        return null;
    }
}

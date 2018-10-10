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
import javax.ejb.Singleton;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Logger;

@Singleton
public class HaSingletonServiceInvokerBean implements HaSingletonServiceInvoker, LocalContext {
    private static final Logger LOGGER = Logger.getLogger(HaSingletonServiceInvokerBean.class.getName());

    @Resource(lookup = "java:jboss/clustering/group/default")
    private Group channelGroup;

    @Resource(lookup = "java:jboss/clustering/dispatcher/default") // <1>
    private CommandDispatcherFactory factory;

    private CommandDispatcher<LocalContext> dispatcher;

    @Override
    public String invokeLocal() {
        return new PassiveServiceSupplier<String>(
                CurrentServiceContainer.getServiceContainer(),
                HaSingletonService.SINGLETON_SERVICE_NAME).get();
    }

    @PostConstruct
    public void postConstruct() {
        dispatcher = this.factory.createCommandDispatcher(
                this.getClass().getName(),
                this);
    }

    @PreDestroy
    public void preDestroy() {
        dispatcher.close();
    }

    @Override
    public String getServiceValue(String sourceNode) {
        //LOGGER.info(String.format("getServiceValue(%s)...", sourceNode));
        String serviceValue = invokeLocal();
        LOGGER.info(String.format("Responding to node %s: %s", sourceNode, serviceValue));
        return serviceValue;
    }

    @Override
    public Map<String, String> invokeRemote() {
        final Map<String, String> nodeResponses = new HashMap<>();
        try {
            Map<Node, CompletionStage<String>> responses = dispatcher.executeOnGroup(
                    new TestCommand(
                            channelGroup.getLocalNode().getName()
                    ),
                    channelGroup.getLocalNode()
            );
            for (Map.Entry<Node, CompletionStage<String>> entry : responses.entrySet()) {
                try {
                    CompletableFuture<String> future = entry.getValue().toCompletableFuture();
                    if (!future.isCancelled()) {
                        String serviceName = future.join();
                        LOGGER.info(String.format("Response from node %s: service value on that node is <<%s>>", entry.getKey().getName(), serviceName));
                        nodeResponses.put(entry.getKey().getName(), serviceName);
                    }
                } catch (CancellationException e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    LOGGER.severe(String.format("Error:\n%s", errors));
                } catch (Exception e) {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    LOGGER.severe(String.format("Error:\n%s", errors));
                }
            }

        } catch (CommandDispatcherException e) {
            LOGGER.severe(String.format("Error in executeOnGroup: %s", e.getMessage()));
        }
        return nodeResponses;
    }
}

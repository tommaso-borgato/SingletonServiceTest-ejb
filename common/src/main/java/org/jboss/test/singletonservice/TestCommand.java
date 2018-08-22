package org.jboss.test.singletonservice;

import org.wildfly.clustering.dispatcher.Command;

public class TestCommand implements Command<String, LocalContext> {
    private static final long serialVersionUID = -3405593925871250676L;
    public String sourceNode;

    public TestCommand(String sourceNode) {
        this.sourceNode = sourceNode;
    }

    @Override
    public String execute(LocalContext context) {
        return context.getServiceValue(sourceNode);
    }
}

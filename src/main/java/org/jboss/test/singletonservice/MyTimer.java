package org.jboss.test.singletonservice;

public interface MyTimer {

    public abstract void initialize(String info);

    public abstract void stop();

}
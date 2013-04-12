package org.jboss.test.singletonservice;

/**
 * Created with IntelliJ IDEA.
 * User: jkudrnac
 */
public interface MyTimer {

    public abstract void initialize(String info);

    public abstract void stop();

}
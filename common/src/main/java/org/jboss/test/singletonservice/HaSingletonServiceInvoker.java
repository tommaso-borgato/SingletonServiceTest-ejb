package org.jboss.test.singletonservice;

import javax.ejb.Local;
import java.util.Map;

@Local
public interface HaSingletonServiceInvoker {
    Map<String, String> invokeRemote();
    String invokeLocal();
}

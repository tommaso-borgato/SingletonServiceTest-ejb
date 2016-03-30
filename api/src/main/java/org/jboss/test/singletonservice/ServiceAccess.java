package org.jboss.test.singletonservice;

import javax.ejb.Remote;

@Remote
public interface ServiceAccess {
    /**
     * @return name of the cluster node where the service is running
     */
    String getNodeNameOfService();
}

package org.jboss.test.singletonservice.client;

import org.jboss.test.singletonservice.ServiceAccess;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;

public class Main {
    public static void main(String[] args) throws Exception {
        Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        Context ctx = new InitialContext(props);

        ServiceAccess bean = (ServiceAccess) ctx.lookup("ejb:/SStest-ejb/ServiceAccessBean!" + ServiceAccess.class.getName());
        System.out.println(bean.getNodeNameOfService());

        ctx.close();

        Thread.sleep(200);
    }
}

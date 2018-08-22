# SingletonServiceTest-ejb

## Background

### WildFly Configuration

WildFly is configured to support singleton services;
In `standalone-ha.xml` we find the `singleton` configuration that relies on some `replicated-cache` to track on which
nodes of the cluster the service is installed:

```    
    <subsystem xmlns="urn:jboss:domain:infinispan:7.0">
        <cache-container name="server" aliases="singleton cluster" default-cache="default" module="org.wildfly.clustering.server">
            <transport lock-timeout="60000"/>
            <replicated-cache name="default">
                <transaction mode="BATCH"/>
            </replicated-cache>
        </cache-container>
        ...
    </subsystem>
    ...
    <subsystem xmlns="urn:jboss:domain:singleton:1.0">
        <singleton-policies default="default">
            <singleton-policy name="default" cache-container="server">
                <simple-election-policy/>
            </singleton-policy>
        </singleton-policies>
    </subsystem>
```

### How it works

This application performs the following steps:

- WildFly reads `META-INF/services/org.jboss.msc.service.ServiceActivator` and starts `org.jboss.test.singletonservice.SingletonActivator`
- `org.jboss.test.singletonservice.SingletonActivator` installs `org.jboss.test.singletonservice.TestingSingletonService` on each node with some election policy
- if the current node is the elected one, then `org.jboss.test.singletonservice.TestingSingletonService` is started 
- `org.jboss.test.singletonservice.TestingSingletonService` starts the `org.jboss.test.singletonservice.MyTimerBean`

Once the `org.jboss.test.singletonservice.TestingSingletonService` is running on one node, it can be invoked via the `org.jboss.test.singletonservice.ServiceAccessBean` 
that is invoked remotely by some EJB Client Application.

## Description

Testing applications for HA singleton MSC services. Each of the applications will be deployed on all cluster nodes,
but the service will be active only on one node at any given time.

The project is structured into 4 main modules with 1 additional module for manual testing:

### Public API (`api`)

This is built with Java target 6, because of some internal implementation details.

- `ServiceAccess`, the `@Remote` interface exposed by `ServiceAccessBean` (see below) that provides the functionality
  of the singleton service

### Common Classes (`common`)

Classes that are shared between the `startup-ejb` and `service-activator` modules (see below).

- `TestingSingletonService`, which is an HA singleton service that provides current node name, which is the name
  of the cluster node where the service is currently running
- `MyTimerBean`, which is a `@Singleton` EJB that shows how to create a HA singleton EJB timer; it provides two methods:
    - `initialize`, which creates an EJB timer that logs a message each second; this method is called by the
      `TestingSingletonService` during activation
    - `stop`, which cancels the previously created EJB timer; this method is called by the
      `TestingSingletonService` during removal
- `ServiceAccessBean`, which is a `@Stateless` EJB that exposes the `TestingSingletonService` to EJB clients

### Startup EJB + Custom Singleton Policy (`startup-ejb`)

An application that activates the singleton service using a `@Singleton @Startup` EJB and uses a custom, in-place built
singleton policy.

- `SingletonStartupBean`, which is a `@Singleton @Startup` EJB that installs and activates the `TestingSingletonService`
  during `@PostConstruct` and removes it during `@PreDestroy`

### Service Activator + Policy from Singleton Subsystem (`service-activator`)

An application that activates the singleton service using `ServiceActivator` and uses a singleton policy defined
in the `singleton` subsystem.

- `SingletonActivator`, which is a `ServiceActivator` that installs and activates the `TestingSingletonService`

### Standalone Client for Manual Testing (`client`)

- `Main`, which is a Java application that uses JBoss EJB Client to invoke the `ServiceAccessBean` remotely;
  it is not used for automated tests, but can be easily used for manual testing when required

## Build

The standard Maven incantation is everything you need: `mvn clean install`. Maven 3 is assumed.

## Versions

Major version number is the generation number of JBoss EAP that is targeted by the applications.
Minor version number is specific to this application.

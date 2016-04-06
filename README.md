# SingletonServiceTest-ejb

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

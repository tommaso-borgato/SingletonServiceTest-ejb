# SingletonServiceTest-ejb #

Testing application for the HA singleton service feature. It contains:

- `TestingSingletonService`, which is a HA singleton service that provides current node name, which is the name
  of the cluster node where the service is currently running
- `SingletonStartupBean`, which is a `@Singleton @Startup` EJB that installs and activates the `TestingSingletonService`
  during `@PostConstruct` and removes it during `@PreDestroy`
- `ServiceAccessBean`, which is a `@Stateless` EJB that exposes the `TestingSingletonService` to EJB clients
- `MyTimerBean`, which is a `@Singleton` EJB that shows how to create a HA singleton EJB timer; it provides two methods:
    - `initialize`, which creates an EJB timer that logs a message each second; this method is called by the
      `TestingSingletonService` during activation
    - `stop`, which cancels the previously created EJB timer; this method is called by the
      `TestingSingletonService` during removal

Even though the application is deployed on all cluster nodes, the service is active on only one node at a time.

All quickstarts for the HA singleton service feature install the singleton service using `ServiceActivator`.
This application uses a `@Singleton @Startup` EJB because:

- this approach predates the entire `ServiceActivator` thing
- the `ServiceActivator` approach is tested with quickstarts testing

## Build ##

The standard Maven is everything you need: `mvn clean install`

## Versions

Major version number is the generation number of JBoss EAP that is targeted by the application.
Minor version number is specific to this application.

# SingletonServiceTest-ejb #

Testing application for singleton service feature, it contains `TestingSingletonService` and `ServiceAccessBean`: clients callings its method `getNodeNameOfService()` are given a node name, where the service is currently started.

Even though the application is deployed on all cluster nodes, the service is active on only one node at a time.

It has two flavors: base and timer.

## Base ##
Only `TestingSingletonService` is started.

## Timer ##
`TestingSingletonService` starts timer service `MyTimerBean`. It logs current time, every one second.

## Build ##
The standard maven is everything you need:
`mvn clean install`





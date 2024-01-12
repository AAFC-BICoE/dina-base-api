# DINA Messaging


## Documentation

RabbitMQ beans will be created if `isConsumer` or `isProducer` is set.

Queues are handled by the different projects using that library.

Messaging properties:

```
dina.messaging.isConsumer="true"
dina.messaging.isProducer="true"
```

RabbitMQ properties:
```
rabbitmq:
  host: localhost
  username: user
  password: password
  port: 15672
```

## Artifact

`dina-messaging` artifact is published on Maven Central.


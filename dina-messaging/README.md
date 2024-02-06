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

[![Maven Central](https://img.shields.io/maven-central/v/io.github.aafc-bicoe/dina-messaging.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.aafc-bicoe/dina-messaging/)


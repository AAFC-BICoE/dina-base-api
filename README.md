# Dina-Base-API

Base [DINA](https://www.dina-project.net) API package for Java built on [SpringBoot](https://spring.io/projects/spring-boot) and [Crnk](https://github.com/crnk-project/crnk-framework).

## Required

* Java 11
* Maven 3.6 (tested)

## Documentation

https://aafc-bicoe.github.io/dina-base-api/

Documentation can be generated using:

`mvn clean compile`

An `index.html` page will be generated in `target/generated-docs`

## Artifact
dina-base-api artifact is published on [BinTray](https://bintray.com/aafc-bice/BICoE/dina-base-api).

```
<dependency>
  <groupId>ca.gc.aafc</groupId>
  <artifactId>dina-base-api</artifactId>
  <version>0.35</version>
</dependency>
```

Test related classes are included in an artifact named `dina-test-support`.

Add BICoE BinTray repository :

```
<repository>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
  <id>bintray-aafc-bice-BICoE</id>
  <name>bintray</name>
  <url>https://dl.bintray.com/aafc-bice/BICoE</url>
</repository>
```

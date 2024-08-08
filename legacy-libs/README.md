Folder that contains libraries that are not available on Maven Central and that can be redistributed under Apache 2 license.
This is a temporary solution until the migration is completed.

These dependencies must be installed to your local maven in order to build the dina-base-api project.

```shell
cd legacy-libs
mvn install:install-file \
  -Dfile=crnk/crnk-client/crnk-client-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-client \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar
mvn install:install-file \
  -Dfile=crnk/crnk-data-jpa-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-data-jpa \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar
mvn install:install-file \
  -Dfile=crnk/crnk-operations-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-operations \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar
mvn install:install-file \
  -Dfile=crnk/crnk-setup-spring-boot2-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-setup-spring-boot2 \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar
```

You should be able to now perform a mvn clean install -DskipTests on the root of the project.

Crnk was developed in https://github.com/crnk-project/crnk-framework under Apache 2 license but is currently unmaintained.

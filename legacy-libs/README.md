Folder that contains libraries that are not available on Maven Central and that can be redistributed under Apache 2 license.
This is a temporary solution until the migration is completed.

These dependencies must be installed to your local maven in order to build the dina-base-api project. Run these commands at the root level of the dina-base-api project.

```shell
mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-client/crnk-client-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-client \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-data-jpa/crnk-data-jpa-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-data-jpa \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-operations/crnk-operations-3.2.20200419165537.jar \
  -DgroupId=io.crnk \
  -DartifactId=crnk-operations \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-setup-spring-boot/crnk-setup-spring-boot2-3.2.20200419165537.jar \
  -DpomFile=legacy-libs/crnk/crnk-setup-spring-boot/crnk-setup-spring-boot2-3.2.20200419165537.pom \
  -DgroupId=io.crnk \
  -DartifactId=crnk-setup-spring-boot2 \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-core/crnk-core-3.2.20200419165537.jar \
  -DpomFile=legacy-libs/crnk/crnk-core/crnk-core-3.2.20200419165537.pom \
  -DgroupId=io.crnk \
  -DartifactId=crnk-core \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-setup-servlet/crnk-setup-servlet-3.2.20200419165537.jar \
  -DpomFile=legacy-libs/crnk/crnk-setup-servlet/crnk-setup-servlet-3.2.20200419165537.pom \
  -DgroupId=io.crnk \
  -DartifactId=crnk-setup-servlet \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar

mvn install:install-file \
  -Dfile=legacy-libs/crnk/crnk-setup-spring/crnk-setup-spring-3.2.20200419165537.jar \
  -DpomFile=legacy-libs/crnk/crnk-setup-spring/crnk-setup-spring-3.2.20200419165537.pom \
  -DgroupId=io.crnk \
  -DartifactId=crnk-setup-spring \
  -Dversion=3.2.20200419165537 \
  -Dpackaging=jar
```

You should be able to now perform a mvn clean install -DskipTests on the root of the project.

Crnk was developed in https://github.com/crnk-project/crnk-framework under Apache 2 license but is currently unmaintained.

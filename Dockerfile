FROM openjdk:11
EXPOSE 8080
ADD target/lritws.war lritws.war
ENTRYPOINT ["java","-jar","/lritws.war"]

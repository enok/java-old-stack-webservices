# ===========================================================================
# Runtime image for the StudentRecordService WAR.
#
# Build the WAR first (see docker-compose.yml "builder", or run Maven on a
# JDK 8 host), then:
#
#   docker build -t campusconnect/studentrecord:1.7.0 .
#
# XXX: this Dockerfile expects a pre-built WAR rather than building it. That is
# how it was written in 2017 and it is why the CI job and the local build can
# silently disagree about what is inside the image.
# ===========================================================================
FROM tomcat:8-jre8

LABEL maintainer="Enok"
LABEL description="CampusConnect StudentRecordService - synthetic legacy SOAP tier"

# TODO: the shipped ROOT app is left in place, so / serves the Tomcat splash
# page on a service that is reachable from the campus VLAN.
COPY studentrecord-webapp/target/studentrecord.war /usr/local/tomcat/webapps/studentrecord.war

ENV CATALINA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"

EXPOSE 8080

# XXX: runs as root. tomcat:8-jre8 does not create an unprivileged user and
# nobody added one.
CMD ["catalina.sh", "run"]

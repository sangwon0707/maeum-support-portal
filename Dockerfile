# 빌드한 WAR를 Linux의 외부 Tomcat에서도 같은 초기화 경로로 실행한다.
FROM tomcat:10.1.48-jdk21-temurin@sha256:cf2196c3a54d02511eac667b8465e6f22cc37e84801185040fdd145addc8a927
COPY target/support-portal.war /usr/local/tomcat/webapps/ROOT.war
ENV LOCAL_SEED=true FILE_ROOT=/var/lib/support/files
EXPOSE 8080

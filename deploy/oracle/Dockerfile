FROM tomcat:10.1.48-jdk21-temurin@sha256:cf2196c3a54d02511eac667b8465e6f22cc37e84801185040fdd145addc8a927
# 호스트의 Caddy가 전달한 HTTPS 정보를 Servlet의 리다이렉트·세션 쿠키에 반영한다.
RUN sed -i 's#</Host>#<Valve className="org.apache.catalina.valves.RemoteIpValve" remoteIpHeader="x-forwarded-for" protocolHeader="x-forwarded-proto" /></Host>#' /usr/local/tomcat/conf/server.xml
# 기본 접근 로그는 일별로 순환하며 7일이 지나면 제거한다.
RUN sed -i 's#suffix=".txt"#suffix=".txt" maxDays="7"#' /usr/local/tomcat/conf/server.xml
COPY target/support-portal.war /usr/local/tomcat/webapps/ROOT.war
ENV FILE_ROOT=/var/lib/support/files
EXPOSE 8080

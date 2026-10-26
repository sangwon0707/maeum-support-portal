package kr.co.maeum.support.config;

import org.apache.catalina.startup.Tomcat;

import java.nio.file.Path;

/** WAR와 같은 웹 초기화 경로로 로컬 실행한다. 운영은 외부 Tomcat에 WAR를 배포한다. */
public final class LocalServer {
    private LocalServer() {}

    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Path.of(".local/tomcat").toAbsolutePath().toString());
        tomcat.setPort(Integer.parseInt(DatabaseConfig.env("PORT", "8087")));
        tomcat.getConnector();
        var context = tomcat.addWebapp("", Path.of("src/main/webapp").toAbsolutePath().toString());
        context.setParentClassLoader(LocalServer.class.getClassLoader());
        tomcat.start();
        tomcat.getServer().await();
    }
}

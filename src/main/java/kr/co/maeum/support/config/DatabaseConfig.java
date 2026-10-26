package kr.co.maeum.support.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.egovframe.rte.psl.dataaccess.mapper.MapperConfigurer;
import org.flywaydb.core.Flyway;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    @Bean
    public static MapperConfigurer mapperConfigurer() {
        var configurer = new MapperConfigurer();
        configurer.setBasePackage("kr.co.maeum.support");
        configurer.setSqlSessionFactoryBeanName("sqlSessionFactory");
        return configurer;
    }

    @Bean(destroyMethod = "close")
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        // 외부 Tomcat의 클래스 로더에서도 JDBC 드라이버를 명확하게 찾도록 지정한다.
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl(env("DB_URL", "jdbc:mariadb://127.0.0.1:13316/support_portal"));
        config.setUsername(env("DB_USER", "support_app"));
        config.setPassword(env("DB_PASSWORD", "local-support-password"));
        config.setMaximumPoolSize(8);
        // 장애 시 요청 스레드가 기본 30초 동안 대기하며 쌓이지 않도록 제한한다.
        config.setConnectionTimeout(5000);
        config.setValidationTimeout(2000);
        config.setConnectionInitSql("SET time_zone = '+00:00'");
        return new HikariDataSource(config);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource, Flyway flyway)
            throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath:mapper/*.xml"));
        org.apache.ibatis.session.Configuration config =
                new org.apache.ibatis.session.Configuration();
        config.setMapUnderscoreToCamelCase(true);
        config.setArgNameBasedConstructorAutoMapping(true);
        factory.setConfiguration(config);
        return factory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    public static String env(String key, String fallback) {
        return System.getProperty(key, System.getenv().getOrDefault(key, fallback));
    }
}

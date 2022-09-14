package gov.cms.bfd.pipeline.rda.insights.util;

import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

@Slf4j
@RequiredArgsConstructor
public class HibernateConnections {

    private final ConfigLoader configLoader;

    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            sessionFactory = createSessionFactory();
        }

        return sessionFactory;
    }

    public SessionFactory createSessionFactory() {
        try {
            Configuration configuration = new Configuration();
            configuration.setProperty("hibernate.connection.url", configLoader.stringValue("url"));
            configuration.setProperty("hibernate.connection.username", configLoader.stringValue("username"));
            configuration.setProperty("hibernate.connection.password", configLoader.stringValue("password"));
            configuration.setProperty("hibernate.connection.driver_class", configLoader.stringValue("driver"));
            configuration.setProperty("hibernate.dialect", configLoader.stringValue("dialect"));
            configuration.setProperty("hibernate.show_sql", configLoader.stringValue("show_sql", "false"));

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();

            return configuration.buildSessionFactory(serviceRegistry);
        } catch (Exception e) {
            log.error("Failed to create session factory", e);
            throw new ExceptionInInitializerError(e);
        }
    }

}

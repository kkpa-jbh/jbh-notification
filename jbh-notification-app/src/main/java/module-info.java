module jbh.notification.app {
  requires org.jboss.logging;
  requires jakarta.cdi;
  requires jakarta.inject;
  requires quarkus.core;
  requires org.eclipse.microprofile.config;
  requires java.net.http;
  requires com.fasterxml.jackson.databind;
  requires resteasy.reactive.common;
  requires jakarta.ws.rs;
  requires micrometer.core;

  opens com.jbh.notification.app to
      quarkus.core;
  opens com.jbh.notification.app.config to
      quarkus.core,
      com.fasterxml.jackson.databind;
}

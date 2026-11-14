package com.jbh.notification.app;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class BootstrapNotificationApp implements QuarkusApplication {

  private static final Logger LOG = Logger.getLogger(BootstrapNotificationApp.class);

  public static void main(final String... args) {
    LOG.info("Starting JBH Notification...");
    Quarkus.run(BootstrapNotificationApp.class, args);
  }

  @Override
  public int run(final String... args) throws Exception {
    LOG.info("JBH Notification started successfully!");

    Quarkus.waitForExit();
    return 0;
  }
}

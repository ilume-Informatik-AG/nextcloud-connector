package de.ilume.nextcloud.outbound;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Dev-only entry point (test scope, never packaged into the shipped jars). Always boots against
 * the local c8run + docker-compose Nextcloud setup (see application-c8run.yml) and auto-deploys
 * every example process/form under examples/ on startup, so IntelliJ can just Run/Debug this
 * class directly with no VM options and a ready-to-use process to click through.
 *
 * <p>Requires c8run and the docker-compose Nextcloud instance to already be running (see README
 * "Developing & Debugging in IntelliJ IDEA"). Working directory must be the module root
 * (nextcloud-connector-outbound), matching IntelliJ's default for a run config using this
 * module's classpath.
 */
@SpringBootApplication
@Deployment(resources = {"file:../examples/**/*.bpmn", "file:../examples/**/*.form"})
class LocalConnectorRuntime {
    public static void main(String[] args) {
        new SpringApplicationBuilder(LocalConnectorRuntime.class).profiles("c8run").run(args);
    }
}

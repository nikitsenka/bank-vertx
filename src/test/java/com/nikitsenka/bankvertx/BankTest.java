package com.nikitsenka.bankvertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ExtendWith(VertxExtension.class)
class BankTest {

    @Rule
    private static GenericContainer container = new GenericContainer(
            new ImageFromDockerfile()
                    .withFileFromFile("Dockerfile", new File("docker/db.dockerfile"))
                    .withFileFromFile("CreateDB.sql", new File("CreateDB.sql")))
            .withEnv("POSTGRES_PASSWORD", "test1234")
            .withExposedPorts(5432);
    private Bank bank;
    private JsonObject config;

    @BeforeEach
    void setUp() {
        container.start();
        config = new JsonObject()
                .put("port", container.getMappedPort(5432))
                .put("host", "localhost");
    }

    @AfterEach
    void tearDown() {
        container.stop();
    }

    @Test
    void testGetBalance(Vertx vertx, VertxTestContext testContext) throws Throwable {
        vertx.deployVerticle(new Bank(), new DeploymentOptions().setConfig(config), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(8080, "localhost", "/client/1/balance")
                    .as(BodyCodec.string())
                    .send(resp -> {
                        if (resp.failed()) {
                            testContext.failNow(resp.cause());
                        } else {
                            testContext.verify(() -> {
                                assertEquals("{\"balance\":0}", resp.result().body());
                                testContext.completeNow();
                            });
                        }
                    });
        }));

        assertTrue(testContext.awaitCompletion(25, TimeUnit.SECONDS));
        if (testContext.failed()) {
            throw testContext.causeOfFailure();
        }
    }
}
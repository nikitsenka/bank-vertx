package com.nikitsenka.bankvertx;

import com.nikitsenka.bankvertx.model.Balance;
import com.nikitsenka.bankvertx.model.Client;
import com.nikitsenka.bankvertx.model.Transaction;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class Bank extends AbstractVerticle {

    static Logger logger = LoggerFactory.getLogger(Bank.class);

    private BankRepository repo;

    @Override
    public void start(Future<Void> fut) {
        // Create a router object.
        Router router = Router.router(vertx);

        repo = new BankRepository(vertx, config());
        router.route().handler(BodyHandler.create()).method(HttpMethod.POST).path("/transaction").handler(this::createTransaction);
        router.get("/client/:id/balance").handler(this::getBalance);
        router.post("/client/new/:balance").handler(this::createClient);

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer()
                .requestHandler(router)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }

    private void getBalance(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8");
            repo.getBalance(id, res -> {
                if (res.succeeded()) {
                    routingContext.response().end(Json.encode(new Balance(res.result().iterator().next().getLong("balance"))));
                } else {
                    logger.error(res.cause());
                    routingContext.response().setStatusCode(500).end(res.cause().getMessage());
                }
            });
        }
    }

    private void createClient(RoutingContext routingContext) {
        final Integer balance = Integer.valueOf(routingContext.request().getParam("balance"));
        if (balance == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8");
            Client client = new Client(0, "", "", "");
            repo.createClient(client, res -> {
                if (res.succeeded()) {
                    Integer id = res.result().iterator().next().getInteger("id");
                    client.setId(id);
                    repo.createTransaction(new Transaction(0, 0, id, balance), trRes -> {
                        if (trRes.succeeded()) {
                            logger.info("transaction successfully created");
                        } else {
                            logger.error("transaction create failed", trRes.cause());
                        }
                    });
                    logger.info("client successfully created");
                    routingContext.response().end(Json.encode(client));
                } else {
                    logger.error("client create failed", res.cause());
                    routingContext.response().setStatusCode(500).end(res.cause().getMessage());
                }
            });

        }
    }

    private void createTransaction(RoutingContext routingContext) {
        Transaction transaction = Json.decodeValue(routingContext.getBodyAsString(), Transaction.class);
        if (transaction == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8");
            repo.createTransaction(transaction, res -> {
                if (res.succeeded()) {
                    transaction.setId(res.result().iterator().next().getInteger("id"));
                    routingContext.response().end(Json.encode(transaction));
                } else {
                    logger.error("failed to get balance", res.cause());
                    routingContext.response().setStatusCode(500).end(res.cause().getMessage());
                }
            });
        }
    }

}

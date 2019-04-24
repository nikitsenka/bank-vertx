package com.nikitsenka.bankvertx;

import com.nikitsenka.bankvertx.model.Client;
import com.nikitsenka.bankvertx.model.Transaction;
import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Tuple;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.List;

public class BankRepository {

    private PgClient pgClient;

    public BankRepository(PgClient client) {
        this.pgClient = client;
    }

    public BankRepository(Vertx vertx, JsonObject config) {
        PgPoolOptions options = new PgPoolOptions();
        options.setDatabase(config.getString("database", "postgres"));
        options.setHost(config.getString("host", "localhost"));
        options.setPort(config.getInteger("port", 5432));
        options.setUser(config.getString("username", "postgres"));
        options.setPassword(config.getString("password", "test1234"));
        options.setCachePreparedStatements(true);
        pgClient = PgClient.pool(vertx, new PgPoolOptions(options).setMaxSize(1));
    }

    public PgClient createClient(Client client, Handler<AsyncResult<PgRowSet>> responseHandler) {
        return pgClient.preparedBatch("INSERT INTO client(name, email, phone) VALUES ($1, $2, $3) RETURNING id",
                List.of(Tuple.of(client.getName(), client.getEmail(), client.getPhone())),
                responseHandler);
    }

    public PgClient getBalance(String id, Handler<AsyncResult<PgRowSet>> responseHandler) {
        return pgClient.preparedQuery("SELECT debit - credit as balance FROM (SELECT COALESCE(sum(amount), 0) AS debit FROM transaction WHERE to_client_id = $1 ) a, ( SELECT COALESCE(sum(amount), 0) AS credit FROM transaction WHERE from_client_id = $1 ) b;",
                Tuple.of(Integer.valueOf(id)),
                responseHandler);
    }

    public PgClient createTransaction(Transaction transaction, Handler<AsyncResult<PgRowSet>> responseHandler) {
        return pgClient.preparedBatch("INSERT INTO transaction(from_client_id, to_client_id, amount) VALUES ($1, $2, $3) RETURNING id",
                List.of(Tuple.of(transaction.getFromClientId(), transaction.getToClientId(), transaction.getAmount())),
                responseHandler);
    }
}

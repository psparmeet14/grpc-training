package com.parmeet.grpc.streaming;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class StockServer {

    private int port;
    private Server server;

    public StockServer(int port) {
        this.port = port;
        server = ServerBuilder.forPort(port)
                .addService(new StockServiceImpl())
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on port: " + port);
        Runtime.getRuntime()
            .addShutdownHook(new Thread(() -> {
                System.err.println("shutting down server");
                try {
                    StockServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("server shutted down");
            })
            );
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown()
                .awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        StockServer stockServer = new StockServer(8080);
        stockServer.start();
        if (stockServer.server != null) {
            stockServer.server.awaitTermination();
        }
    }

}

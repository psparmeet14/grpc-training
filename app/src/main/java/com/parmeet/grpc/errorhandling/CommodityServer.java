package com.parmeet.grpc.errorhandling;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CommodityServer {
    private final int port;
    private final Server server;

    public CommodityServer(int port) {
        this.port = port;
        server = ServerBuilder.forPort(port)
                .addService(new CommodityService())
                .build();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        CommodityServer commodityServer = new CommodityServer(8080);
        commodityServer.start();
        if (commodityServer.server != null) {
            commodityServer.server.awaitTermination();
        }
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on port " + port);
        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> {
                    System.out.println("shutting down server");
                    try {
                        CommodityServer.this.stop();
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }
                }));
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown()
                    .awaitTermination(30, TimeUnit.SECONDS);
        }
    }
}

package com.parmeet.grpc.helloservice.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.parmeet.grpc.HelloRequest;
import org.parmeet.grpc.HelloResponse;
import org.parmeet.grpc.HelloServiceGrpc;

public class GrpcClient {
    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        HelloServiceGrpc.HelloServiceBlockingStub stub = HelloServiceGrpc.newBlockingStub(channel);
        HelloResponse helloResponse = stub.hello(HelloRequest.newBuilder()
                .setFirstName("Parmeet")
                .setLastName("Singh")
                .build());

        System.out.println("Response received from server:\n" + helloResponse);
        channel.shutdown();
    }
}

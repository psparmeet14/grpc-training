package com.parmeet.grpc.errorhandling;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.Commodity;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.CommodityPriceProviderGrpc;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.CommodityQuote;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.StreamingCommodityQuote;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CommodityClient {
    private final CommodityPriceProviderGrpc.CommodityPriceProviderStub nonBlcokingStub;

    public CommodityClient(Channel channel) {
        nonBlcokingStub = CommodityPriceProviderGrpc.newStub(channel);
    }

    public void getBidirectionalCommodityPriceLists() throws InterruptedException {
        System.out.println("#######START EXAMPLE#######: BidirectionalStreaming - getCommodityPriceLists from list of commodities");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<StreamingCommodityQuote> responseObserver = new StreamObserver<StreamingCommodityQuote>() {

            @Override
            public void onNext(StreamingCommodityQuote streamingCommodityQuote) {
                switch (streamingCommodityQuote.getMessageCase()) {
                    case COMMODITYQUOTE -> {
                        CommodityQuote commodityQuote = streamingCommodityQuote.getCommodityQuote();
                        System.out.println("RESPONSE producer: " + commodityQuote.getProducerName() + " price: " + commodityQuote.getPrice());
                    }
                    case STATUS -> {
                        com.google.rpc.Status status = streamingCommodityQuote.getStatus();
                        System.out.println("RESPONSE status error: ");
                        System.out.println("Status code: " + Code.forNumber(status.getCode()));
                        System.out.println("Status message: " + status.getMessage());
                        for (Any any : status.getDetailsList()) {
                            if (any.is(ErrorInfo.class)) {
                                ErrorInfo errorInfo;
                                try {
                                    errorInfo = any.unpack(ErrorInfo.class);
                                    System.out.println("Reason: " + errorInfo.getReason());
                                    System.out.println("Domain: " + errorInfo.getDomain());
                                    System.out.println("Insert Token: " + errorInfo.getMetadataMap().get("insertToken"));
                                } catch (InvalidProtocolBufferException e) {
                                    System.out.println(e.getMessage());
                                }
                            }
                        }
                    }
                    default -> System.out.println("Unknown message case");
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("getBidirectionalCommodityPriceLists Failed: " + Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished getBidirectionalCommodityPriceLists");
                finishLatch.countDown();
            }
        };

        StreamObserver<Commodity> requestObserver = nonBlcokingStub.bidirectionalListOfPrices(responseObserver);
        try {
            for (int i = 1; i <= 2; i++) {
                Commodity request = Commodity.newBuilder()
                        .setCommodityName("Commodity" + i)
                        .setAccessToken(i + "23validToken")
                        .build();
                System.out.println("REQUEST - commodity: " + request.getCommodityName());
                requestObserver.onNext(request);
                Thread.sleep(200);
                if (finishLatch.getCount() == 0) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            requestObserver.onError(e);
        }
        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            System.out.println("getBidirectionalCommodityPriceLists can not finish within 1 minute");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:8080")
                .usePlaintext()
                .build();
        try {
            CommodityClient client = new CommodityClient(channel);
            client.getBidirectionalCommodityPriceLists();
        } finally {
            channel.shutdown()
                    .awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}

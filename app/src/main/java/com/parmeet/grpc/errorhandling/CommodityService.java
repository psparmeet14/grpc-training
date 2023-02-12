package com.parmeet.grpc.errorhandling;

import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CommodityService extends CommodityPriceProviderGrpc.CommodityPriceProviderImplBase {

    private static Map<String, Double> commodityLookupBasePrice;
    static {
        commodityLookupBasePrice = new ConcurrentHashMap<>();
        commodityLookupBasePrice.put("Commodity1", 5.0);
        commodityLookupBasePrice.put("Commodity2", 6.0);
    }

    @Override
    public void getBestCommodityPrice(Commodity request, StreamObserver<CommodityQuote> responseObserver) {
        if (commodityLookupBasePrice.get(request.getCommodityName()) == null) {
            var errorResponseKey = ProtoUtils.keyForProto(ErrorResponse.getDefaultInstance());
            var errorResponse = ErrorResponse.newBuilder()
                    .setCommodityName(request.getCommodityName())
                    .setAccessToken(request.getAccessToken())
                    .setExpectedValue("Only Commodity1, Commodity2 are supported")
                    .build();
            var metadata = new Metadata();
            metadata.put(errorResponseKey, errorResponse);
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("The commodity is not supported")
                    .asRuntimeException(metadata));
        } else if (!request.getAccessToken().equals("123validToken")) {
            com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                    .setCode(Code.NOT_FOUND.getNumber())
                    .setMessage("The access token not found")
                    .addDetails(Any.pack(ErrorInfo.newBuilder()
                            .setReason("Invalid token")
                            .setDomain("com.parmeet.grpc.errorhandling")
                            .putMetadata("insertToken", "123validToken")
                            .build()))
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        } else {
            var commodityQuote = CommodityQuote.newBuilder()
                    .setPrice(fetchBestPriceBid(request))
                    .setCommodityName(request.getCommodityName())
                    .setProducerName("Best producer with best price")
                    .build();
            responseObserver.onNext(commodityQuote);
            responseObserver.onCompleted();
        }
    }

    @Override
    public StreamObserver<Commodity> bidirectionalListOfPrices(StreamObserver<StreamingCommodityQuote> responseObserver) {
        return new StreamObserver<Commodity>() {
            @Override
            public void onNext(Commodity request) {
                System.out.println("Access token: " + request.getAccessToken());
                if (!request.getAccessToken().equals("123validToken")) {
                    com.google.rpc.Status status = com.google.rpc.Status.newBuilder()
                            .setCode(Code.NOT_FOUND.getNumber())
                            .setMessage("The access token not found")
                            .addDetails(Any.pack(ErrorInfo.newBuilder()
                                    .setReason("Invalid Token")
                                    .setDomain("com.parmeet.grpc.errorhandling")
                                    .putMetadata("insertToken", "123validToken")
                                    .build()))
                            .build();
                    StreamingCommodityQuote streamingCommodityQuote = StreamingCommodityQuote.newBuilder()
                            .setStatus(status)
                            .build();
                    responseObserver.onNext(streamingCommodityQuote);
                } else {
                    for (int i = 1; i <= 5; i++) {
                        CommodityQuote commodityQuote = CommodityQuote.newBuilder()
                                .setPrice(fetchProviderPriceBid(request, "producer" + i))
                                .setCommodityName(request.getCommodityName())
                                .setProducerName("producer:" + i)
                                .build();
                        StreamingCommodityQuote streamingCommodityQuote = StreamingCommodityQuote.newBuilder()
                                .setCommodityQuote(commodityQuote)
                                .build();
                        responseObserver.onNext(streamingCommodityQuote);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    private static double fetchBestPriceBid(Commodity commodity) {
        return commodityLookupBasePrice.get(commodity.getCommodityName()) + ThreadLocalRandom.current()
                .nextDouble(-0.2d, 0.2d);
    }

    private static double fetchProviderPriceBid(Commodity commodity, String providerName) {
        return commodityLookupBasePrice.get(commodity.getCommodityName()) + providerName.length() + ThreadLocalRandom.current()
                .nextDouble(-0.2d, 0.2d);
    }
}

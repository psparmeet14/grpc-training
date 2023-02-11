package com.parmeet.grpc.errorhandling;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.StreamObserver;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

        } else {
            var commodityQuote = CommodityQuote.newBuilder()
                    .setPrice(commodityLookupBasePrice.get(request.getCommodityName()))
                    .setCommodityName(request.getCommodityName())
                    .setProducerName("Best producer with best price")
                    .build();
            responseObserver.onNext(commodityQuote);
            responseObserver.onCompleted();
        }
    }

    @Override
    public StreamObserver<Commodity> bidirectionalListOfPrices(StreamObserver<StreamingCommodityQuote> responseObserver) {
        return null;
    }
}

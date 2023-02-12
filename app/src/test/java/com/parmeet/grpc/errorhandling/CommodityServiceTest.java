package com.parmeet.grpc.errorhandling;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.StatusProto;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.Commodity;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.CommodityPriceProviderGrpc;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.CommodityQuote;
import org.parmeet.grpc.errorhandling.commoditypricegrpc.ErrorResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CommodityServiceTest {

    CommodityPriceProviderGrpc.CommodityPriceProviderBlockingStub blockingStub;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @BeforeEach
    public void setup() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(new CommodityService())
                .build()
                .start());

        blockingStub = CommodityPriceProviderGrpc.newBlockingStub(
                grpcCleanup.register(
                        InProcessChannelBuilder.forName(serverName)
                                .directExecutor().build()));
    }

    @Test
    public void whenUsingValidRequest_thenReturnResponse() {
        CommodityQuote reply = blockingStub.getBestCommodityPrice(Commodity.newBuilder()
                .setCommodityName("Commodity1")
                .setAccessToken("123validToken")
                .build());
        assertEquals("Commodity1", reply.getCommodityName());
    }

    @Test
    public void whenUsingInvalidRequestToken_thenReturnExceptionGoogleRPCStatus() throws InvalidProtocolBufferException {
        Commodity request = Commodity.newBuilder()
                .setAccessToken("invalidToken")
                .setCommodityName("Commodity1")
                .build();

        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> blockingStub.getBestCommodityPrice(request));
        Status status = StatusProto.fromThrowable(thrown);
        assertNotNull(status);
        assertEquals("NOT_FOUND", Code.forNumber(status.getCode()).toString());
        assertEquals("The access token not found", status.getMessage());
        for (Any any : status.getDetailsList()) {
            if (any.is(ErrorInfo.class)) {
                ErrorInfo errorInfo = any.unpack(ErrorInfo.class);
                assertEquals("Invalid token", errorInfo.getReason());
                assertEquals("com.parmeet.grpc.errorhandling", errorInfo.getDomain());
                assertEquals("123validToken", errorInfo.getMetadataMap().get("insertToken"));
            }
        }

    }

    @Test
    public void whenUsingInvalidCommodityName_thenReturnExceptionIoGrpcStatus() {
        Commodity request = Commodity.newBuilder()
                .setCommodityName("Commodity5")
                .setAccessToken("123validToken")
                .build();

        StatusRuntimeException thrown = assertThrows(StatusRuntimeException.class, () -> blockingStub.getBestCommodityPrice(request));

        assertEquals("INVALID_ARGUMENT", thrown.getStatus().getCode().toString());
        assertEquals("INVALID_ARGUMENT: The commodity is not supported", thrown.getMessage());
        Metadata metadata = io.grpc.Status.trailersFromThrowable(thrown);
        ErrorResponse errorResponse = metadata.get(ProtoUtils.keyForProto(ErrorResponse.getDefaultInstance()));
        assertEquals("Commodity5", errorResponse.getCommodityName());
        assertEquals("123validToken", errorResponse.getAccessToken());
        assertEquals("Only Commodity1, Commodity2 are supported", errorResponse.getExpectedValue());
    }
}
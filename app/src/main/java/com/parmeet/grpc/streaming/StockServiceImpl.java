package com.parmeet.grpc.streaming;

import io.grpc.stub.StreamObserver;
import org.parmeet.stockquotegrpc.Stock;
import org.parmeet.stockquotegrpc.StockQuote;
import org.parmeet.stockquotegrpc.StockQuoteProviderGrpc;

import java.util.concurrent.ThreadLocalRandom;

public class StockServiceImpl extends StockQuoteProviderGrpc.StockQuoteProviderImplBase {

    @Override
    public void serverSideStreamingGetListStockQuotes(Stock request, StreamObserver<StockQuote> responseObserver) {

        for (int i = 1; i <= 5; i++) {
            StockQuote stockQuote = StockQuote.newBuilder()
                    .setPrice(fetchStockPriceBid(request))
                    .setOfferNumber(i)
                    .setDescription("Price for stock: " + request.getTickerSymbol())
                    .build();
            responseObserver.onNext(stockQuote);
        }
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<Stock> clientSideStreamingGetStatisticsOfStocks(StreamObserver<StockQuote> responseObserver) {
        return new StreamObserver<Stock>() {
            int count;
            double price = 0.0;
            StringBuffer sb = new StringBuffer();

            @Override
            public void onNext(Stock stock) {
                count++;
                price = +fetchStockPriceBid(stock);
                sb.append(":")
                        .append(stock.getTickerSymbol());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(StockQuote.newBuilder()
                        .setPrice(price / count)
                        .setDescription("Statistics-" + sb.toString())
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public StreamObserver<Stock> bidirectionalStreamingGetListsStockQuotes(StreamObserver<StockQuote> responseObserver) {
        return new StreamObserver<Stock>() {
            @Override
            public void onNext(Stock request) {
                for (int i = 1; i <= 5; i++) {
                    StockQuote stockQuote = StockQuote.newBuilder()
                            .setPrice(fetchStockPriceBid(request))
                            .setOfferNumber(1)
                            .setDescription("Price for stock: " + request.getTickerSymbol())
                            .build();
                    responseObserver.onNext(stockQuote);
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

    private static double fetchStockPriceBid(Stock stock) {
        return stock.getTickerSymbol().length()
                + ThreadLocalRandom.current().nextDouble(-0.1d, 0.1d);
    }
}

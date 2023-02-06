package com.parmeet.grpc.streaming;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.parmeet.stockquotegrpc.Stock;
import org.parmeet.stockquotegrpc.StockQuote;
import org.parmeet.stockquotegrpc.StockQuoteProviderGrpc;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StockClient {
    private StockQuoteProviderGrpc.StockQuoteProviderBlockingStub blockingStub;
    private StockQuoteProviderGrpc.StockQuoteProviderStub nonBlockingStub;
    private List<Stock> stocks;

    public StockClient(Channel channel) {
        blockingStub = StockQuoteProviderGrpc.newBlockingStub(channel);
        nonBlockingStub = StockQuoteProviderGrpc.newStub(channel);
        initializeStocks();
    }

    public void serverSideStreamingListOfStockPrices() {
        System.out.println("######START EXAMPLE######: ServerSideStreaming - list of Stock prices from a given stock");
        var request = Stock.newBuilder()
                .setTickerSymbol("AU")
                .setCompanyName("Austich")
                .setDescription("Server streaming example")
                .build();
        Iterator<StockQuote> stockQuotes;
        try {
            System.out.println("REQUEST - ticker symbol " + request.getTickerSymbol());
            stockQuotes = blockingStub.serverSideStreamingGetListStockQuotes(request);
            for (int i = 1; stockQuotes.hasNext(); i++) {
                var stockQuote = stockQuotes.next();
                System.out.println("RESPONSE - PRICE #" + i + ": " + stockQuote.getPrice());
            }
        } catch(StatusRuntimeException e) {
            System.out.println("RPC failed: " + e.getStatus());
        }
    }

    public void clientSideStreamingGetStatisticsOfStocks() throws InterruptedException {
        System.out.println("######START EXAMPLE######: ClientSideStreaming - getStatisticsOfStocks from a list of stocks");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<StockQuote> responseObserver = new StreamObserver<StockQuote>() {
            @Override
            public void onNext(StockQuote summary) {
                System.out.println("RESPONSE, got some statistics - Average Price: " + summary.getPrice() + ", description: " + summary.getDescription());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("Stock statistics failed, ERROR: " + Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished clientSideStreamingGetStatisticsOfStocks");
                finishLatch.countDown();
            }
        };

        StreamObserver<Stock> requestObserver = nonBlockingStub.clientSideStreamingGetStatisticsOfStocks(responseObserver);
        try {
            for (Stock stock : stocks) {
                System.out.println("REQUEST: " + stock.getTickerSymbol() + ", " + stock.getCompanyName());
                requestObserver.onNext(stock);
                if (finishLatch.getCount() == 0) {
                    return;
                }
            }
        } catch(RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();
        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            System.out.println("clientSideStreamingGetStatisticsOfStocks can not finish within 1 minute");
        }
    }

    public void bidirectionalStreamingGetListsStockQuotes() throws InterruptedException {
        System.out.println("#######START EXAMPLE#######: BidirectionalStreaming - getListsStockQuotes from list of stocks");
        final CountDownLatch finishLatch = new CountDownLatch(1);
        StreamObserver<StockQuote> responseObserver = new StreamObserver<StockQuote>() {
            @Override
            public void onNext(StockQuote stockQuote) {
                System.out.println("RESPONSE price#" + stockQuote.getOfferNumber() + " : " + stockQuote.getPrice() + ", description: " + stockQuote.getDescription());
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("bidirectionalStreamingGetListsStockQuotes failed: " + Status.fromThrowable(t));
                finishLatch.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("Finished bidirectionalStreamingGetListsStockQuotes");
                finishLatch.countDown();
            }
        };

        StreamObserver<Stock> requestObserver = nonBlockingStub.bidirectionalStreamingGetListsStockQuotes(responseObserver);
        try {
            for (Stock stock: stocks) {
                System.out.println("REQUEST: " + stock.getTickerSymbol() + ", " + stock.getCompanyName());
                requestObserver.onNext(stock);
                Thread.sleep(200);
                if (finishLatch.getCount() == 0) {
                    return;
                }
            }
        } catch(RuntimeException e) {
            requestObserver.onError(e);
            throw e;
        }
        requestObserver.onCompleted();

        if (!finishLatch.await(1, TimeUnit.MINUTES)) {
            System.out.println("bidirectionalStreamingGetListsStockQuotes can not finish within 1 minute");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String target = "localhost:8080";
        if (args.length > 0) {
            target = args[0];
        }

        var channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        try {
            StockClient client = new StockClient(channel);
            client.serverSideStreamingListOfStockPrices();
            client.clientSideStreamingGetStatisticsOfStocks();
            client.bidirectionalStreamingGetListsStockQuotes();
        } finally {
            channel.shutdown()
                    .awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void initializeStocks() {
        this.stocks = Arrays.asList(Stock.newBuilder().setTickerSymbol("AU").setCompanyName("Auburn Corp").setDescription("Aptitude Intel").build()
                , Stock.newBuilder().setTickerSymbol("BAS").setCompanyName("Bassel Corp").setDescription("Business Intel").build()
                , Stock.newBuilder().setTickerSymbol("COR").setCompanyName("Corvine Corp").setDescription("Corporate Intel").build()
                , Stock.newBuilder().setTickerSymbol("DIA").setCompanyName("Dialogic Corp").setDescription("Development Intel").build()
                , Stock.newBuilder().setTickerSymbol("EUS").setCompanyName("Euskaltel Corp").setDescription("English Intel").build());
    }

}

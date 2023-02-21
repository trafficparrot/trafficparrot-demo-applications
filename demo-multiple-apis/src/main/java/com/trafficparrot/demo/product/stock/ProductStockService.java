package com.trafficparrot.demo.product.stock;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.Optional;

import static com.trafficparrot.demo.product.stock.ProductStockRepository.productStock;
import static io.grpc.Status.NOT_FOUND;

@GrpcService
public class ProductStockService extends StockServiceGrpc.StockServiceImplBase {

    @Override
    public void queryStock(Stock.StockRequest request, StreamObserver<Stock.StockResponse> responseObserver) {
        Optional<ProductStock> productStock = productStock(request.getProductId());
        if (productStock.isPresent()) {
            responseObserver.onNext(Stock.StockResponse.newBuilder()
                    .setProductId(request.getProductId())
                    .setAvailable(productStock.get().stock)
                    .build());
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(NOT_FOUND.withDescription("Could not find product with id: " + request.getProductId()).asException());
        }
    }
}
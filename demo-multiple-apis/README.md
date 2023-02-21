# Product Details
curl -v -g \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"{ product(searchTerm: \"1\") { productId name description } }"}' \
  http://localhost:8085/graphql

# Product Price
docker run -d --hostname my-rabbit --name some-rabbit -p 15672:15672 -p 5672:5672 rabbitmq:3-management

price-request-queue
message PriceRequest {
  string productId = 1;
}

price-response-queue
message PriceResponse {
  string productId = 1;
  double price = 2;
}

# Product Stock
grpcurl -d '{"productId":"1"}' \
  -plaintext localhost:8086 \
  com.trafficparrot.demo.product.stock.StockService/queryStock

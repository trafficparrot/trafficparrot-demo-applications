# Build and run demo application

./mvnw clean install

java -jar target/demo-multiple-apis-*.jar

# Product Details
curl -v -g \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"{ product(searchTerm: \"1\") { productId name description } }"}' \
  http://localhost:8085/graphql

# Product Price
docker run -d --hostname my-rabbit --name some-rabbit -p 15672:15672 -p 5672:5672 rabbitmq:3-management

## Listen to queue: price-request-queue
message PriceRequest {
  string productId = 1;
}

## Respond to queue: request message RPC replyTo queue
message PriceResponse {
  oneof response {
    PriceResponseSuccess success = 1;
    PriceResponseError error = 2;
  }
}

message PriceResponseSuccess {
  string productId = 1;
  double price = 2;
}

message PriceResponseError {
  string message = 1;
}

# Product Stock
grpcurl -d '{"productId":"1"}' \
  -plaintext localhost:8086 \
  com.trafficparrot.demo.product.stock.StockService/queryStock

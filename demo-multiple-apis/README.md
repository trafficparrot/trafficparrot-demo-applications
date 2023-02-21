# Product Details
curl -v -g \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"{ product(searchTerm: \"1\") { productId name description } }"}' \
  http://localhost:8085/graphql

# Product Stock
grpcurl -d '{"productId":"1"}' \
  -plaintext localhost:8086 \
  com.trafficparrot.demo.product.stock.StockService/queryStock

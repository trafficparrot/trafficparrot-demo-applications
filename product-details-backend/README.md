# Sample Queries
- curl -v -g \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"{ product(searchTerm: \"1\") { productId name description } }"}' \
  http://localhost:8080/graphql


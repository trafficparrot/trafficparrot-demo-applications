# Build and run demo application

./mvnw clean install

java -jar target/demo-multiple-apis-*.jar

# Product Details
SERVER_HOST=localhost
SERVER_HTTP_PORT=8085
curl -v -g \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"query":"{ product(searchTerm: \"Headphones\") { productId name description } }"}' \
  http://${SERVER_HOST}:${SERVER_HTTP_PORT}/graphql

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
SERVER_HOST=localhost
SERVER_GRPC_PORT=8086
grpcurl -d '{"productId":"1"}' \
  -plaintext ${SERVER_HOST}:${SERVER_GRPC_PORT} \
  com.trafficparrot.demo.product.stock.StockService/queryStock

# Sample GCP deployment commands
PROJECT=fill-in-project-name
SERVICE_ACCOUNT=fill-in-account@developer.gserviceaccount.com
INSTANCE_NAME=fill-in-instance-name
SERVER_HTTP_PORT=8085
SERVER_GRPC_PORT=8086
NETWORK=default
ZONE=us-central1-a
MACHINE_TYPE=f1-micro
BOOT_IMAGE=projects/ubuntu-os-cloud/global/images/ubuntu-minimal-2204-jammy-v20230302

gcloud compute instances create ${INSTANCE_NAME} \
--project=${PROJECT} \
--zone=${ZONE} \
--machine-type=${MACHINE_TYPE} \
--network-interface=network-tier=STANDARD,subnet=${NETWORK} \
--maintenance-policy=MIGRATE \
--provisioning-model=STANDARD \
--service-account=${SERVICE_ACCOUNT} \
--scopes=https://www.googleapis.com/auth/devstorage.read_only,https://www.googleapis.com/auth/logging.write,https://www.googleapis.com/auth/monitoring.write,https://www.googleapis.com/auth/servicecontrol,https://www.googleapis.com/auth/service.management.readonly,https://www.googleapis.com/auth/trace.append \
--tags=${INSTANCE_NAME} \
--create-disk=auto-delete=yes,boot=yes,device-name=${INSTANCE_NAME},image=${BOOT_IMAGE},mode=rw,size=10,type=projects/${PROJECT}/zones/${ZONE}/diskTypes/pd-balanced \
--no-shielded-secure-boot \
--shielded-vtpm \
--shielded-integrity-monitoring \
--reservation-affinity=any

gcloud compute firewall-rules create ${INSTANCE_NAME} \
--project=${PROJECT} \
--direction=INGRESS \
--priority=1000 \
--network=${NETWORK} \
--action=ALLOW \
--rules=tcp:${SERVER_HTTP_PORT},tcp:${SERVER_GRPC_PORT} \
--source-ranges=0.0.0.0/0 \
--target-tags=${INSTANCE_NAME}

gcloud compute scp \
--project=${PROJECT} \
--zone=${ZONE} \
target/*.jar \
${INSTANCE_NAME}:~/application.jar

gcloud compute ssh \
--project=${PROJECT} \
--zone=${ZONE} \
${INSTANCE_NAME}

export DEBIAN_FRONTEND=noninteractive
sudo apt update
sudo apt install openjdk-8-jre

export RABBITMQ_HOST=fill-in-host
export RABBITMQ_VHOST=fill-in-vhost
export RABBITMQ_USERNAME=fill-in-username
export RABBITMQ_PASSWORD=fill-in-password
test -f process.pid && pkill -F process.pid && while ps -p $(cat process.pid); do sleep 1; done
nohup java -jar application.jar >nohup.out 2>&1 &
echo $! > process.pid

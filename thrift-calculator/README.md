Requirements:
* https://thrift.apache.org/docs/install/
* Java 11+ is required to build and run the demo application
* Traffic Parrot

Instructions:
1. ./mvnw clean install
2. unzip thrift-calculator-1.1.0-release.zip
3. cd thrift-calculator-1.1.0/
4. ./start-server.sh
5. ./start-client.sh
6. Start Traffic Parrot (you can [request a trial here](https://trafficparrot.com/trial.html?utm_source=thrift-calculator))
7. Traffic Parrot can now be used to record the real server localhost:5572
8. The client can either talk directly to the real server localhost:5572 or to Traffic Parrot on localhost:5562

[Contact us](https://trafficparrot.com/contact.html?utm_source=thrift-calculator) for more details

Requirements:
* https://thrift.apache.org/docs/install/
* Java 11+ is required to build and run the demo application
* Traffic Parrot

Instructions:
* ./mvnw clean install
* unzip thrift-calculator-1.1.0-release.zip
* cd thrift-calculator-1.1.0/
* ./start-server.sh
* ./start-client.sh
* Start Traffic Parrot (you can request a trial here https://trafficparrot.com/trial.html)
* Traffic Parrot can now be used to record the real server localhost:5572
* The client can either talk directly to the real server localhost:5572 or to Traffic Parrot on localhost:5562

* Contact us for more details https://trafficparrot.com/contact.html

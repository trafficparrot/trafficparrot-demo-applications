## Download this application

You can download this application here: https://github.com/trafficparrot/trafficparrot-demo-applications/releases/latest/download/mobile-onboarding.zip

## Setup

The zip does not contain IBM's MQ client. Download "com.ibm.mq.allclient.jar" from Maven Central
and save it as "mobile-onboarding/lib/com.ibm.mq.allclient.jar" in the unzipped application:
https://repo1.maven.org/maven2/com/ibm/mq/com.ibm.mq.allclient/9.4.1.0/com.ibm.mq.allclient-9.4.1.0.jar

The application uses the javax JMS API, so it needs com.ibm.mq.allclient, not the Jakarta client.

Start it with start.sh (start.cmd on Windows) from inside the unzipped directory, then open http://localhost:8383 to send provisioning requests.

Building from source needs the same jar at "mobile-onboarding/lib/com.ibm.mq.allclient.jar" before running Maven.

## Tutorial

This application is used in the IBM MQ passthrough tutorial: https://github.com/trafficparrot/trafficparrot-demo-applications/tree/master/mobile-network-hardware

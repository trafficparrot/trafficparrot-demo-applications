#!/bin/bash
java -cp .:./lib/* -Dpurchasing-microservice.properties=./purchasing-microservice.properties com.trafficparrot.example.PurchasingMicroservice

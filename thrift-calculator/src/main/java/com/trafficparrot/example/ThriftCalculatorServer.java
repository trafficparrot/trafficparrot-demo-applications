/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Based on https://thrift.apache.org/tutorial/java modifications copyright Traffic Parrot 2020-2022
 */
package com.trafficparrot.example;

import com.trafficparrot.example.Calculator.Processor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.trafficparrot.example.ThriftCalculatorServerProperties.loadProperties;

public class ThriftCalculatorServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftCalculatorServer.class);

    public static void main(String[] args) throws IOException, TTransportException {
        ThriftCalculatorServerProperties properties = loadProperties();
        int nonTlsPort = properties.nonTlsPort();

        Processor<Calculator.Iface> processor = new Processor<>(new ThriftCalculatorHandler());
        TServerTransport serverTransport = new TServerSocket(nonTlsPort);
        TServer server = new TSimpleServer(new TServer.Args(serverTransport)
                .transportFactory(new TFramedTransport.Factory())
                .processor(processor));
        LOGGER.info("Starting Thrift calculator server on non TLS port " + nonTlsPort);
        server.serve();
    }
}

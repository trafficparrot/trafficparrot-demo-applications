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
 * -----------
 * MIT License
 * Copyright (c) 2018 HenryBrown0
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Based on https://thrift.apache.org/tutorial/java modifications copyright Traffic Parrot 2020-2026
 * Based on https://github.com/HenryBrown0/simple-calculator modifications copyright Traffic Parrot 2020-2026
 */
package com.trafficparrot.example;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The calculator the web page shows: what each button does, and what the page displays. It calls the Thrift
 * calculator server for every calculation and keeps no history of its own beyond the history index.
 */
class CalculatorDisplay {
    private static final Logger LOGGER = LoggerFactory.getLogger(CalculatorDisplay.class);

    static final String DISPLAY_EQUAL = "=";
    static final String DISPLAY_DIVIDE = "/";
    static final String DISPLAY_MULTIPLY = "*";
    static final String DISPLAY_SUBTRACT = "-";
    static final String DISPLAY_ADD = "+";
    static final String DISPLAY_CANCEL = "c";
    static final String PREVIOUS = "previous";
    static final Set<String> KEYS = Set.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            DISPLAY_EQUAL, DISPLAY_DIVIDE, DISPLAY_MULTIPLY, DISPLAY_SUBTRACT, DISPLAY_ADD, DISPLAY_CANCEL, PREVIOUS);
    private static final int DEFAULT_PORT = 5572;

    private final AtomicInteger historySequence = new AtomicInteger();

    private int current;
    private int total;
    private String method;
    private String host;
    private int port;
    private String message;
    private boolean digitsDisabled;

    CalculatorDisplay() {
        current = 0;
        total = 0;
        method = "";
        host = "localhost";
        port = DEFAULT_PORT;
    }

    /**
     * One button press. A digit does nothing while the digits are disabled, as a disabled button cannot be pressed.
     */
    synchronized void press(String key) {
        switch (key) {
            case DISPLAY_DIVIDE:
            case DISPLAY_MULTIPLY:
            case DISPLAY_SUBTRACT:
            case DISPLAY_ADD:
                enableDigits();
                setMethod(key);
                break;
            case DISPLAY_CANCEL:
                enableDigits();
                setMethod(DISPLAY_CANCEL);
                calculate();
                break;
            case DISPLAY_EQUAL:
                disableDigits();
                calculate();
                break;
            case PREVIOUS:
                previous();
                break;
            default:
                if (!digitsDisabled) {
                    inputNumber(Integer.parseInt(key));
                }
        }
    }

    /**
     * The host and port field changed.
     */
    synchronized void hostAndPort(String newValue) {
        if (!newValue.contains(":")) {
            return;
        }
        String[] split = newValue.split(":");
        if (split.length == 2) {
            setHost(split[0].trim());
            setPort(Integer.parseInt(split[1].trim()));
        }
    }

    synchronized int total() {
        return total;
    }

    synchronized int current() {
        return current;
    }

    synchronized String message() {
        return message;
    }

    synchronized String host() {
        return host;
    }

    synchronized int port() {
        return port;
    }

    synchronized boolean digitsDisabled() {
        return digitsDisabled;
    }

    private void setHost(String host) {
        this.host = host;
    }

    private void setPort(int port) {
        this.port = port;
    }

    private void inputNumber(int input) {
        current = Integer.parseInt(current + "" + input);
    }

    private void setMethod(String method) {
        this.method = method;
        if (total == 0) {
            total = current;
        }
        current = 0;
    }

    private void calculate() {
        try {
            switch (method) {
                case DISPLAY_DIVIDE:
                    calculate(Operation.DIVIDE, method);
                    break;
                case DISPLAY_MULTIPLY:
                    calculate(Operation.MULTIPLY, method);
                    break;
                case DISPLAY_SUBTRACT:
                    calculate(Operation.SUBTRACT, method);
                    break;
                case DISPLAY_ADD:
                    calculate(Operation.ADD, method);
                    break;
                case DISPLAY_CANCEL:
                    reset();
                    message = "0";
                    break;
            }
        } catch (InvalidOperation e) {
            message = "Error with " + renderOperation(Operation.findByValue(e.whatOp)) + " " + e.getWhy();
        } catch (TException e) {
            LOGGER.error("Problem communicating with Thrift calculator server {}:{}", host, port, e);
            message = e.getMessage();
        }
    }

    private void calculate(Operation operation, String method) throws TException {
        message = total + " " + method + " " + current + " = ";
        total = calculateOnServer(total, operation, current);
        message += total;
        current = 0;
    }

    private String renderOperation(Operation operation) {
        if (operation == null) {
            throw new UnsupportedOperationException("Unknown operation");
        }
        switch (operation) {
            case ADD:
                return DISPLAY_ADD;
            case SUBTRACT:
                return DISPLAY_SUBTRACT;
            case MULTIPLY:
                return DISPLAY_MULTIPLY;
            case DIVIDE:
                return DISPLAY_DIVIDE;
            default:
                throw new UnsupportedOperationException(operation.name());
        }
    }

    private void previous() {
        int original = historySequence.get();
        try {
            if (original == 0) {
                message = "No previous total";
            } else {
                int previous = historySequence.decrementAndGet();
                total = historyOnServer(previous);
                message = "Moved to previous total " + total;
                LOGGER.info("History index " + historySequence.get() + ": " + total);
            }
        } catch (TException e) {
            LOGGER.error("Problem communicating with Thrift calculator server {}:{}", host, port, e);
            message = e.getMessage();
            historySequence.set(original);
        }
        if (historySequence.get() == 0) {
            enableDigits();
        }
    }

    private int historyOnServer(int key) throws TException {
        try (TSocket socket = new TSocket(host, port)) {
            if (!socket.isOpen()) {
                socket.open();
            }
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(socket));
            Calculator.Client client = new Calculator.Client(protocol);
            return Integer.parseInt(client.getStruct(key).value);
        }
    }

    private int calculateOnServer(int num1, Operation operation, int num2) throws TException {
        try (TSocket socket = new TSocket(host, port)) {
            if (!socket.isOpen()) {
                socket.open();
            }
            TProtocol protocol = new TBinaryProtocol(new TFramedTransport(socket));
            Calculator.Client client = new Calculator.Client(protocol);
            int result = client.calculate(new Work(num1, num2, operation));
            historySequence.incrementAndGet();
            LOGGER.info("History index " + historySequence.get() + ": " + result);
            return result;
        }
    }

    private void reset() {
        current = 0;
        total = 0;
        method = "";
        historySequence.set(1);
        previous();
    }

    private void disableDigits() {
        digitsDisabled = true;
    }

    private void enableDigits() {
        digitsDisabled = false;
    }
}

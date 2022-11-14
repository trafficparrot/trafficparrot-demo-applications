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
 * Based on https://thrift.apache.org/tutorial/java modifications copyright Traffic Parrot 2020-2022
 * Based on https://github.com/HenryBrown0/simple-calculator modifications copyright Traffic Parrot 2020-2022
 */
package com.trafficparrot.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ThriftCalculatorClient extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftCalculatorClient.class);

    private final Display display = new Display();
    private final TextField hostAndPortField = new TextField();
    private final Label messageLabel = new Label();
    private final Label currentLabel = new Label();
    private final Label totalLabel = new Label();
    private final Button[] digits = new Button[10];

    public static void main(String[] args) {
        LOGGER.info("Starting Thrift calculator client");
        launch(args);
    }

    @Override
    public void start(Stage window) {
        window.setTitle("Thrift Remote Calculator Client");
        window.setScene(layoutScene());
        display.paint();
        window.show();
    }

    private Scene layoutScene() {
        VBox layout = new VBox();
        GridPane numberPad = addNumberButtons();

        layout.setPadding(new Insets(5, 0, 0, 0));
        layout.getChildren().addAll(totalLabel, currentLabel, numberPad, messageLabel, hostAndPortField, previousButton());

        VBox.setVgrow(numberPad, Priority.ALWAYS);

        Scene scene = new Scene(layout, 300, 500);
        scene.getStylesheets().add("thrift.calculator.client.css");
        return scene;
    }

    public Button previousButton() {
        Button button = new Button("Previous");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> {
            display.previous();
            display.paint();
        });
        return button;
    }

    private GridPane addNumberButtons() {
        GridPane layout = new GridPane();
        RowConstraints[] rowConst = new RowConstraints[5];
        ColumnConstraints[] colConst = new ColumnConstraints[4];

        // Set row constraints making rows responsive
        for (int i = 0; i <= 4; i++) {
            rowConst[i] = new RowConstraints();
            rowConst[i].setPercentHeight(20);
        }
        layout.getRowConstraints().addAll(rowConst[0], rowConst[1], rowConst[2], rowConst[3], rowConst[4]);

        // Set column constraints making columns responsive
        for (int i = 0; i <= 3; i++) {
            colConst[i] = new ColumnConstraints();
            colConst[i].setPercentWidth(25);
        }
        layout.getColumnConstraints().addAll(colConst[0], colConst[1], colConst[2], colConst[3]);

        // Create 0-9 buttons
        for (int i = 0; i <= 9; i++) {
            int value = i;
            digits[i] = new Button(Integer.toString(i));
            digits[i].setMaxWidth(Double.MAX_VALUE);
            digits[i].setMaxHeight(Double.MAX_VALUE);
            digits[i].setOnAction(e -> {
                display.inputNumber(value);
                display.paint();
            });
        }

        // Place 0-9 buttons
        layout.add(digits[7], 0, 1);
        layout.add(digits[8], 1, 1);
        layout.add(digits[9], 2, 1);
        layout.add(digits[4], 0, 2);
        layout.add(digits[5], 1, 2);
        layout.add(digits[6], 2, 2);
        layout.add(digits[1], 0, 3);
        layout.add(digits[2], 1, 3);
        layout.add(digits[3], 2, 3);
        layout.add(digits[0], 0, 4);
        GridPane.setColumnSpan(digits[0], 3);

        // Divide button
        Button divide = new Button(Display.DISPLAY_DIVIDE);
        divide.setMaxWidth(Double.MAX_VALUE);
        divide.setMaxHeight(Double.MAX_VALUE);
        divide.setOnAction(e -> {
            display.enableDigits();
            display.setMethod(Display.DISPLAY_DIVIDE);
            display.paint();
        });
        layout.add(divide, 1, 0);

        // Multiple button
        Button multiple = new Button(Display.DISPLAY_MULTIPLY);
        multiple.setMaxWidth(Double.MAX_VALUE);
        multiple.setMaxHeight(Double.MAX_VALUE);
        multiple.setOnAction(e -> {
            display.enableDigits();
            display.setMethod(Display.DISPLAY_MULTIPLY);
            display.paint();
        });
        layout.add(multiple, 2, 0);

        // Minus button
        Button minus = new Button(Display.DISPLAY_SUBTRACT);
        minus.setMaxWidth(Double.MAX_VALUE);
        minus.setMaxHeight(Double.MAX_VALUE);
        minus.setOnAction(e -> {
            display.enableDigits();
            display.setMethod(Display.DISPLAY_SUBTRACT);
            display.paint();
        });
        layout.add(minus, 3, 1);

        // Add button
        Button add = new Button(Display.DISPLAY_ADD);
        add.setMaxWidth(Double.MAX_VALUE);
        add.setMaxHeight(Double.MAX_VALUE);
        add.setOnAction(e -> {
            display.enableDigits();
            display.setMethod(Display.DISPLAY_ADD);
            display.paint();
        });
        layout.add(add, 3, 0);

        // Clear button
        Button clear = new Button(Display.DISPLAY_CANCEL);
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setMaxHeight(Double.MAX_VALUE);
        clear.setOnAction(e -> {
            display.enableDigits();
            display.setMethod(Display.DISPLAY_CANCEL);
            display.calculate();
            display.paint();
        });
        layout.add(clear, 0, 0);

        // Equals button
        Button equal = new Button(Display.DISPLAY_EQUAL);
        equal.setMaxWidth(Double.MAX_VALUE);
        equal.setMaxHeight(Double.MAX_VALUE);
        equal.setOnAction(e -> {
            display.disableDigits();
            display.calculate();
            display.paint();
        });
        layout.add(equal, 3, 2);
        GridPane.setRowSpan(equal, 4);

        // Host and port field
        hostAndPortField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.contains(":")) {
                return;
            }
            String[] split = newValue.split(":");
            if (split.length == 2) {
                display.setHost(split[0].trim());
                display.setPort(Integer.parseInt(split[1].trim()));
            }
        });
        return layout;
    }

    private class Display {
        private static final String DISPLAY_EQUAL = "=";
        private static final String DISPLAY_DIVIDE = "/";
        private static final String DISPLAY_MULTIPLY = "*";
        private static final String DISPLAY_SUBTRACT = "-";
        private static final String DISPLAY_ADD = "+";
        private static final String DISPLAY_CANCEL = "c";
        private static final int DEFAULT_PORT = 5572;

        private final AtomicInteger historySequence = new AtomicInteger();

        private int current;
        private int total;
        private String method;
        private String host;
        private int port;
        private String message;

        public Display() {
            current = 0;
            total = 0;
            method = "";
            host = "localhost";
            port = DEFAULT_PORT;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void inputNumber(int input) {
            current = Integer.parseInt(current + "" + input);
        }

        public void setMethod(String method) {
            this.method = method;
            if (total == 0) {
                total = current;
            }
            current = 0;
        }

        public void calculate() {
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

        public void previous() {
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

        public void paint() {
            paintMessage();
            paintCurrent();
            paintTotal();
            paintHostAndPort();
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

        private void paintMessage() {
            messageLabel.setText(message);
        }

        private void paintCurrent() {
            currentLabel.setText("Current: " + Long.toString(current));
        }

        private void paintTotal() {
            totalLabel.setText("Total: " + Long.toString(total));
        }

        private void paintHostAndPort() {
            if (host.isEmpty()) {
                hostAndPortField.setPromptText("Enter host:port of Thrift calculator server");
            } else {
                hostAndPortField.setText(host + ":" + port);
            }
        }

        public void disableDigits() {
            for (Button digit : digits) {
                digit.setDisable(true);
            }
        }

        public void enableDigits() {
            for (Button digit : digits) {
                digit.setDisable(false);
            }
        }
    }
}

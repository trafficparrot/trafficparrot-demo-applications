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
 * Based on https://thrift.apache.org/tutorial/java modifications copyright Traffic Parrot 2020
 * Based on https://github.com/HenryBrown0/simple-calculator modifications copyright Traffic Parrot 2020
 */
package com.trafficparrot.example;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Node;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ThriftCalculatorClient extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThriftCalculatorClient.class);

    private Stage window;
    private Display display;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        window = primaryStage;
        display = new Display();

        window.setTitle("Thrift Remote Calculator Client");
        paint();
        window.show();
    }

    private void paint() {
        VBox layout = new VBox();
        GridPane numberPad = addNumberButtons();

        layout.setPadding(new Insets(5, 0, 0, 0));
        layout.getChildren().addAll(display.getTotal(), display.getCurrent(), numberPad, display.getMessage(), display.hostAndPortField(), previousButton());

        VBox.setVgrow(numberPad, Priority.ALWAYS);

        Scene scene = new Scene(layout, 300, 500);
        scene.getStylesheets().add("thrift.calculator.client.css");

        window.setScene(scene);
    }

    public Button previousButton() {
        Button button = new Button("Previous");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> {
            display.previous();
            this.paint();
        });
        return button;
    }

    private GridPane addNumberButtons() {
        GridPane layout = new GridPane();
        Button[] btnArray = new Button[10];
        RowConstraints[] rowConst = new RowConstraints[5];
        ColumnConstraints[] colConst = new ColumnConstraints[4];

        // Set row constraints making rows responsive
        for(int i = 0; i <= 4; i ++) {
            rowConst[i] = new RowConstraints();
            rowConst[i].setPercentHeight(20);
        }
        layout.getRowConstraints().addAll(rowConst[0], rowConst[1], rowConst[2], rowConst[3], rowConst[4]);

        // Set column constraints making columns responsive
        for(int i = 0; i <= 3; i ++) {
            colConst[i] = new ColumnConstraints();
            colConst[i].setPercentWidth(25);
        }
        layout.getColumnConstraints().addAll(colConst[0], colConst[1], colConst[2], colConst[3]);

        // Create 0-9 buttons
        for(int i = 0; i <= 9; i++) {
            int value = i;
            btnArray[i] = new Button(Integer.toString(i));
            btnArray[i].setMaxWidth(Double.MAX_VALUE);
            btnArray[i].setMaxHeight(Double.MAX_VALUE);
            btnArray[i].setOnAction(e -> {
                display.inputNumber(value);
                this.paint();
            });
        }

        // Place 0-9 buttons
        layout.add(btnArray[7], 0, 1);
        layout.add(btnArray[8], 1, 1);
        layout.add(btnArray[9], 2, 1);
        layout.add(btnArray[4], 0, 2);
        layout.add(btnArray[5], 1, 2);
        layout.add(btnArray[6], 2, 2);
        layout.add(btnArray[1], 0, 3);
        layout.add(btnArray[2], 1, 3);
        layout.add(btnArray[3], 2, 3);
        layout.add(btnArray[0], 0, 4);
        GridPane.setColumnSpan(btnArray[0], 3);

        // Divide button
        Button divide = new Button("/");
        divide.setMaxWidth(Double.MAX_VALUE);
        divide.setMaxHeight(Double.MAX_VALUE);
        divide.setOnAction(e -> {
            display.setMethod("/");
            this.paint();
        });
        layout.add(divide, 1, 0);

        // Multiple button
        Button multiple = new Button("*");
        multiple.setMaxWidth(Double.MAX_VALUE);
        multiple.setMaxHeight(Double.MAX_VALUE);
        multiple.setOnAction(e -> {
            display.setMethod("*");
            this.paint();
        });
        layout.add(multiple, 2, 0);

        // Minus button
        Button minus = new Button("-");
        minus.setMaxWidth(Double.MAX_VALUE);
        minus.setMaxHeight(Double.MAX_VALUE);
        minus.setOnAction(e -> {
            display.setMethod("-");
            this.paint();
        });
        layout.add(minus, 3, 1);

        // Add button
        Button add = new Button("+");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setMaxHeight(Double.MAX_VALUE);
        add.setOnAction(e -> {
            display.setMethod("+");
            this.paint();
        });
        layout.add(add, 3, 0);

        // Clear button
        Button clear = new Button("c");
        clear.setMaxWidth(Double.MAX_VALUE);
        clear.setMaxHeight(Double.MAX_VALUE);
        clear.setOnAction(e -> {
            display.setMethod("c");
            display.calculate();
            this.paint();
        });
        layout.add(clear, 0, 0);

        // Equals button
        Button equal = new Button("=");
        equal.setMaxWidth(Double.MAX_VALUE);
        equal.setMaxHeight(Double.MAX_VALUE);
        equal.setOnAction(e -> {
            display.calculate();
            this.paint();
        });
        layout.add(equal, 3, 1);
        GridPane.setRowSpan(equal, 4);

        return layout;
    }

    private static class Display {
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
            current = input;
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

        private void calculate(Operation operation, String s) throws TException {
            message = total + " " + s + " " + current + " = ";
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
            try {
                int previous = historySequence.decrementAndGet();
                if (previous > 0) {
                    total = historyOnServer(previous);
                    message = "Moved to previous total "  + total;
                } else {
                    message = "No previous total";
                }
            } catch (TException e) {
                LOGGER.error("Problem communicating with Thrift calculator server {}:{}", host, port, e);
                message = e.getMessage();
                historySequence.incrementAndGet();
            }
        }

        private int historyOnServer(int key) throws TException {
            try (TSocket socket = new TSocket(host, port)) {
                if (!socket.isOpen()) {
                    socket.open();
                }
                TProtocol protocol = new TBinaryProtocol(socket);
                Calculator.Client client = new Calculator.Client(protocol);
                return Integer.parseInt(client.getStruct(key).value);
            }
        }

        private int calculateOnServer(int num1, Operation operation, int num2) throws TException {
            try (TSocket socket = new TSocket(host, port)) {
                if (!socket.isOpen()) {
                    socket.open();
                }
                TProtocol protocol = new TBinaryProtocol(socket);
                Calculator.Client client = new Calculator.Client(protocol);
                return client.calculate(historySequence.incrementAndGet(), new Work(num1, num2, operation));
            }
        }

        private void reset() {
            current = 0;
            total = 0;
            method = "";
        }

        public Label getMessage() {
            return new Label(message);
        }

        public Label getCurrent() {
            return new Label("Current: " + Long.toString(current));
        }

        public Label getTotal() {
            return new Label("Total: " + Long.toString(total));
        }

        private TextField hostAndPortField() {
            TextField hostAndPort = new TextField();
            if (host.isEmpty()) {
                hostAndPort.setPromptText("Enter host:port of Thrift calculator server");
            } else {
                hostAndPort.setText(host + ":" + port);
            }
            hostAndPort.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue.contains(":")) {
                    return;
                }
                String[] split = newValue.split(":");
                if (split.length == 2) {
                    setHost(split[0].trim());
                    setPort(Integer.parseInt(split[1].trim()));
                }
            });
            return hostAndPort;
        }
    }
}

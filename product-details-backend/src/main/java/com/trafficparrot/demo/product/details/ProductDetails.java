package com.trafficparrot.demo.product.details;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.csv.CSVFormat.DEFAULT;

public class ProductDetails {

    private static final String DATA_FILE_NAME = "data.csv";
    public final String productId;
    public final String name;
    public final String description;

    private ProductDetails(String productId, String name, String description) {
        this.productId = productId;
        this.name = name;
        this.description = description;
    }

    public static Optional<ProductDetails> searchProductDetails(String searchTerm) {
        return loadProductDetails().stream()
                .filter(productDetails -> productDetails.name.contains(searchTerm) || productDetails.description.contains(searchTerm))
                .findFirst();
    }

    private static List<ProductDetails> loadProductDetails() {
        CSVFormat csvFormat = DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
        try (CSVParser csvParser = csvFormat.parse(csvReader())) {
            return csvParser.stream()
                    .map(strings -> new ProductDetails(strings.get("productId"), strings.get("name"), strings.get("description")))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Problem loading: " + DATA_FILE_NAME, e);
        }
    }

    private static Reader csvReader() throws FileNotFoundException {
        Path csvFile = Paths.get(DATA_FILE_NAME);
        if (Files.exists(csvFile)) {
            return new FileReader(csvFile.toFile());
        } else {
            return new InputStreamReader(requireNonNull(ProductDetails.class.getClassLoader().getResourceAsStream(DATA_FILE_NAME)));
        }
    }
}
package com.trafficparrot.demo.product;

import com.trafficparrot.demo.product.details.ProductDetails;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;

public class ProductDataRepository {

    private static final String DATA_FILE_NAME = "data.csv";

    public static List<ProductData> loadProductData() {
        CSVFormat csvFormat = DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
        try (CSVParser csvParser = csvFormat.parse(csvReader())) {
            return csvParser.stream()
                    .map(strings -> new ProductData(
                        strings.get("productId"),
                        strings.get("name"),
                        strings.get("description"),
                        Double.parseDouble(strings.get("price")),
                        Integer.parseInt(strings.get("stock"))
                    ))
                    .collect(toList());
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

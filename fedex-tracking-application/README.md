# FedEx Tracking Demo Application
## Requirements
- This project runs on Java 8, it has not been tested with higher versions
- Set up `passwords.properties` file with a line like `apikey=password` using your FedEx test API credentials for `https://apis-sandbox.fedex.com`
- Put the corresponding API key name in `FedExApiUsers.FED_EX_API_USER`
- Run the Maven build using `./mvnw clean install`
- See the test run report `target/allure-report/index.html` in your browser:
  ![Sample Test Report](sample-report.png)
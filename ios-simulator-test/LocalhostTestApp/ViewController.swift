import UIKit

class ViewController: UIViewController {
    
    @IBOutlet weak var urlTextField: UITextField!
    @IBOutlet weak var responseTextView: UITextView!
    @IBOutlet weak var sendRequestButton: UIButton!
    @IBOutlet weak var statusLabel: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        // Set default URL
        urlTextField.text = "http://localhost:8081/test"
        
        // Configure UI
        responseTextView.layer.borderColor = UIColor.lightGray.cgColor
        responseTextView.layer.borderWidth = 1.0
        responseTextView.layer.cornerRadius = 5.0
        responseTextView.isEditable = false
        responseTextView.font = UIFont.monospacedSystemFont(ofSize: 12, weight: .regular)
        responseTextView.backgroundColor = UIColor.systemGray6
        responseTextView.textColor = UIColor.label
        responseTextView.alwaysBounceVertical = true
        responseTextView.showsVerticalScrollIndicator = true
        responseTextView.contentInset = UIEdgeInsets(top: 5, left: 5, bottom: 5, right: 5)
        
        sendRequestButton.layer.cornerRadius = 5.0
        
        // Add keyboard dismiss on tap
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard))
        tapGesture.cancelsTouchesInView = false
        view.addGestureRecognizer(tapGesture)
    }
    
    @objc private func dismissKeyboard() {
        view.endEditing(true)
    }
    
    @IBAction func sendRequestTapped(_ sender: UIButton) {
        guard let urlString = urlTextField.text,
              !urlString.isEmpty,
              let url = URL(string: urlString) else {
            showAlert(title: "Error", message: "Please enter a valid URL")
            return
        }
        
        // Disable button during request
        sendRequestButton.isEnabled = false
        statusLabel.text = "Sending request..."
        statusLabel.textColor = .systemBlue
        responseTextView.text = ""
        
        // Log request
        NSLog("LocalhostTestApp: Sending GET request to \(urlString)")
        
        // Create URL session with no caching
        let config = URLSessionConfiguration.default
        config.requestCachePolicy = .reloadIgnoringLocalCacheData
        config.urlCache = nil
        let session = URLSession(configuration: config)
        
        // Create request
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 30
        
        // Send request
        let task = session.dataTask(with: request) { [weak self] data, response, error in
            DispatchQueue.main.async {
                self?.sendRequestButton.isEnabled = true
                
                if let error = error {
                    self?.statusLabel.text = "Error"
                    self?.statusLabel.textColor = .systemRed
                    self?.responseTextView.text = "Error: \(error.localizedDescription)"
                    return
                }
                
                guard let httpResponse = response as? HTTPURLResponse else {
                    self?.statusLabel.text = "Error"
                    self?.statusLabel.textColor = .systemRed
                    self?.responseTextView.text = "Invalid response"
                    return
                }
                
                self?.statusLabel.text = "Status: \(httpResponse.statusCode)"
                self?.statusLabel.textColor = httpResponse.statusCode == 200 ? .systemGreen : .systemOrange
                
                var responseText = "=== HTTP STATUS ===\n"
                responseText += "Status Code: \(httpResponse.statusCode)\n"
                
                // Add headers
                responseText += "\n=== HEADERS ===\n"
                for (key, value) in httpResponse.allHeaderFields {
                    responseText += "\(key): \(value)\n"
                }
                
                // Add body with better formatting
                responseText += "\n=== BODY ===\n"
                if let data = data, data.count > 0 {
                    responseText += "Body size: \(data.count) bytes\n"
                    
                    // Debug logging
                    NSLog("LocalhostTestApp: Response size = \(data.count) bytes")
                    
                    if let bodyString = String(data: data, encoding: .utf8) {
                        // Limit display to prevent UI freezing
                        let maxDisplayLength = 10000 // 10KB max for display
                        if bodyString.count > maxDisplayLength {
                            let truncated = String(bodyString.prefix(maxDisplayLength))
                            responseText += truncated
                            responseText += "\n\n... [TRUNCATED - showing first \(maxDisplayLength) characters of \(bodyString.count) total]"
                            
                            // Log first 500 chars to console
                            NSLog("LocalhostTestApp: Body preview = \(String(bodyString.prefix(500)))")
                        } else {
                            responseText += bodyString
                            // Log entire body if small
                            if bodyString.count < 1000 {
                                NSLog("LocalhostTestApp: Full body = \(bodyString)")
                            } else {
                                NSLog("LocalhostTestApp: Body preview = \(String(bodyString.prefix(500)))")
                            }
                        }
                        
                        // Ensure there's a newline at the end if not present
                        if !responseText.hasSuffix("\n") {
                            responseText += "\n"
                        }
                    } else {
                        // Try other encodings
                        responseText += "(Unable to decode as UTF-8 text)\n"
                        // Show hex preview of first 100 bytes
                        let preview = data.prefix(100)
                        let hexString = preview.map { String(format: "%02x", $0) }.joined(separator: " ")
                        responseText += "Hex preview: \(hexString)"
                        NSLog("LocalhostTestApp: Non-UTF8 data, hex preview = \(hexString)")
                    }
                } else {
                    responseText += "(No content - 0 bytes)"
                    NSLog("LocalhostTestApp: Empty response body")
                }
                
                self?.responseTextView.text = responseText
                
                // Scroll to top
                self?.responseTextView.scrollRangeToVisible(NSRange(location: 0, length: 0))
            }
        }
        
        task.resume()
    }
    
    private func showAlert(title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default))
        present(alert, animated: true)
    }
}
package com.example.localhosttestapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    
    private lateinit var urlEditText: TextInputEditText
    private lateinit var sendRequestButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var responseTextView: TextView
    
    companion object {
        private const val TAG = "LocalhostTestApp"
        private const val MAX_DISPLAY_LENGTH = 10000 // 10KB max for display
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
    }
    
    private fun initViews() {
        urlEditText = findViewById(R.id.urlEditText)
        sendRequestButton = findViewById(R.id.sendRequestButton)
        statusTextView = findViewById(R.id.statusTextView)
        responseTextView = findViewById(R.id.responseTextView)
    }
    
    private fun setupClickListeners() {
        sendRequestButton.setOnClickListener {
            val urlString = urlEditText.text?.toString()?.trim()
            if (urlString.isNullOrEmpty()) {
                showToast(getString(R.string.error_invalid_url))
                return@setOnClickListener
            }
            
            sendHttpRequest(urlString)
        }
    }
    
    private fun sendHttpRequest(urlString: String) {
        Log.d(TAG, "Sending GET request to: $urlString")
        
        // Update UI for request start
        sendRequestButton.isEnabled = false
        statusTextView.text = getString(R.string.status_sending)
        statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_info))
        responseTextView.text = ""
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 30000 // 30 seconds
                    readTimeout = 30000 // 30 seconds
                    useCaches = false
                    setRequestProperty("User-Agent", "LocalhostTestApp/1.0")
                }
                
                val responseCode = connection.responseCode
                val responseMessage = connection.responseMessage ?: ""
                
                Log.d(TAG, "Response code: $responseCode")
                
                // Read response body
                val inputStream = if (responseCode >= 400) {
                    connection.errorStream ?: connection.inputStream
                } else {
                    connection.inputStream
                }
                
                val responseBody = inputStream?.bufferedReader()?.use(BufferedReader::readText) ?: ""
                val bodySize = responseBody.toByteArray().size
                
                Log.d(TAG, "Response size: $bodySize bytes")
                if (responseBody.length < 1000) {
                    Log.d(TAG, "Full body: $responseBody")
                } else {
                    Log.d(TAG, "Body preview: ${responseBody.take(500)}")
                }
                
                // Format response text
                val responseText = buildString {
                    append("=== HTTP STATUS ===\n")
                    append("Status Code: $responseCode\n")
                    if (responseMessage.isNotEmpty()) {
                        append("Status Message: $responseMessage\n")
                    }
                    
                    append("\n=== HEADERS ===\n")
                    connection.headerFields.forEach { (key, values) ->
                        val headerKey = key ?: "Status-Line"
                        values.forEach { value ->
                            append("$headerKey: $value\n")
                        }
                    }
                    
                    append("\n=== BODY ===\n")
                    append("Body size: $bodySize bytes\n")
                    
                    if (responseBody.isNotEmpty()) {
                        if (responseBody.length > MAX_DISPLAY_LENGTH) {
                            append(responseBody.take(MAX_DISPLAY_LENGTH))
                            append("\n\n... [TRUNCATED - showing first $MAX_DISPLAY_LENGTH characters of ${responseBody.length} total]")
                        } else {
                            append(responseBody)
                        }
                        
                        if (!responseBody.endsWith("\n")) {
                            append("\n")
                        }
                    } else {
                        append("(No content - 0 bytes)")
                    }
                }
                
                withContext(Dispatchers.Main) {
                    updateUIWithResponse(responseCode, responseText)
                }
                
                connection.disconnect()
                
            } catch (e: Exception) {
                Log.e(TAG, "Request failed", e)
                withContext(Dispatchers.Main) {
                    updateUIWithError(e.message ?: "Unknown error")
                }
            }
        }
    }
    
    private fun updateUIWithResponse(statusCode: Int, responseText: String) {
        sendRequestButton.isEnabled = true
        statusTextView.text = "Status: $statusCode"
        
        val statusColor = when {
            statusCode == 200 -> R.color.status_success
            statusCode in 400..499 -> R.color.status_warning
            statusCode >= 500 -> R.color.status_error
            else -> R.color.status_info
        }
        statusTextView.setTextColor(ContextCompat.getColor(this, statusColor))
        
        responseTextView.text = responseText
        
        // Scroll to top
        findViewById<View>(R.id.responseScrollView).scrollTo(0, 0)
    }
    
    private fun updateUIWithError(errorMessage: String) {
        sendRequestButton.isEnabled = true
        statusTextView.text = getString(R.string.status_error)
        statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_error))
        responseTextView.text = getString(R.string.error_network, errorMessage)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
package com.example.webhook.service;

import com.example.webhook.model.GenerateWebhookResponse;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void executeFlow() {
        // 1. Call generateWebhook API
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> body = new HashMap<>();
        body.put("name", "John Doe");
        body.put("regNo", "REG12347");
        body.put("email", "john@example.com");

        ResponseEntity<GenerateWebhookResponse> response =
                restTemplate.postForEntity(url, body, GenerateWebhookResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String webhookUrl = response.getBody().getWebhook();
            String accessToken = response.getBody().getAccessToken();

            // 2. Solve SQL (Question 1 → odd regNo)
            String finalQuery = """
                    SELECT 
                        p.AMOUNT AS SALARY,
                        CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME,
                        TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE,
                        d.DEPARTMENT_NAME
                    FROM PAYMENTS p
                    JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID
                    JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
                    WHERE DAY(p.PAYMENT_TIME) <> 1
                    ORDER BY p.AMOUNT DESC
                    LIMIT 1;
                    """;

            // 3. Send answer to webhook
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            Map<String, String> payload = new HashMap<>();
            payload.put("finalQuery", finalQuery);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> submitResponse =
                    restTemplate.postForEntity(webhookUrl, entity, String.class);

            System.out.println("Submit Response: " + submitResponse.getBody());
        } else {
            System.err.println("Failed to generate webhook!");
        }
    }
}

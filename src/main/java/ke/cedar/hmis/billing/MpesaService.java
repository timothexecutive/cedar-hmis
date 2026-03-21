package ke.cedar.hmis.billing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

@ApplicationScoped
public class MpesaService {

    @ConfigProperty(name = "mpesa.consumer.key",
                    defaultValue = "sandbox_key")
    String consumerKey;

    @ConfigProperty(name = "mpesa.consumer.secret",
                    defaultValue = "sandbox_secret")
    String consumerSecret;

    @ConfigProperty(name = "mpesa.shortcode",
                    defaultValue = "174379")
    String shortCode;

    @ConfigProperty(name = "mpesa.passkey",
                    defaultValue = "bfb279f9aa9bdbcf158e97dd71a467cd2e0c893059b10f78e6b72ada1ed2c919")
    String passKey;

    @ConfigProperty(name = "mpesa.callback.url",
                    defaultValue = "https://hmis.cedarhospital.co.ke/api/billing/mpesa/callback")
    String callbackUrl;

    @ConfigProperty(name = "mpesa.sandbox",
                    defaultValue = "true")
    boolean sandbox;

    private String getBaseUrl() {
        return sandbox
            ? "https://sandbox.safaricom.co.ke"
            : "https://api.safaricom.co.ke";
    }

    // Get OAuth token from Safaricom
    public String getAccessToken() {
        try {
            String credentials = Base64.getEncoder()
                .encodeToString((consumerKey + ":" + consumerSecret)
                    .getBytes());

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() +
                    "/oauth/v1/generate?grant_type=client_credentials"))
                .header("Authorization", "Basic " + credentials)
                .GET()
                .build();

            HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

            // Extract token from response
            String body = response.body();
            String token = body.split("\"access_token\":\"")[1]
                .split("\"")[0];
            return token;

        } catch (Exception e) {
            throw new WebApplicationException(
                Response.status(500)
                    .entity("{\"error\":\"Failed to get M-Pesa token: "
                        + e.getMessage() + "\"}")
                    .build());
        }
    }

    // Initiate STK Push
    public String initiateSTKPush(String phone, int amount,
            String accountRef, String description) {
        try {
            String token     = getAccessToken();
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password  = Base64.getEncoder()
                .encodeToString((shortCode + passKey + timestamp)
                    .getBytes());

            // Format phone — ensure it starts with 254
            String formattedPhone = phone
                .replaceAll("^0", "254")
                .replaceAll("^\\+", "");

            String payload = "{"
                + "\"BusinessShortCode\":\"" + shortCode + "\","
                + "\"Password\":\"" + password + "\","
                + "\"Timestamp\":\"" + timestamp + "\","
                + "\"TransactionType\":\"CustomerPayBillOnline\","
                + "\"Amount\":" + amount + ","
                + "\"PartyA\":\"" + formattedPhone + "\","
                + "\"PartyB\":\"" + shortCode + "\","
                + "\"PhoneNumber\":\"" + formattedPhone + "\","
                + "\"CallBackURL\":\"" + callbackUrl + "\","
                + "\"AccountReference\":\"" + accountRef + "\","
                + "\"TransactionDesc\":\"" + description + "\""
                + "}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getBaseUrl() +
                    "/mpesa/stkpush/v1/processrequest"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = client
                .send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            throw new WebApplicationException(
                Response.status(500)
                    .entity("{\"error\":\"STK Push failed: "
                        + e.getMessage() + "\"}")
                    .build());
        }
    }
}
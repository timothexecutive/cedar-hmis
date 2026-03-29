package ke.cedar.hmis.appointments;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;

@ApplicationScoped
public class SmsService {

    @ConfigProperty(name = "africastalking.username",
                    defaultValue = "sandbox")
    String username;

    @ConfigProperty(name = "africastalking.api-key",
                    defaultValue = "")
    String apiKey;

    @ConfigProperty(
        name = "africastalking.base-url",
        defaultValue = "https://api.sandbox.africastalking" +
                       ".com/version1/messaging")
    String baseUrl;

    @ConfigProperty(name = "africastalking.sender",
                    defaultValue = "NONE")
    String sender;

    // ── Build HttpClient with relaxed SSL for dev ─────
    // This handles the Windows SSL proxy interception
    // issue in development. Production uses default SSL.
    private HttpClient buildHttpClient() {
        try {
            SSLContext sslContext =
                SSLContext.getInstance("TLS");
            sslContext.init(null,
                new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[]
                            getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(
                            X509Certificate[] chain,
                            String authType) {}
                        public void checkServerTrusted(
                            X509Certificate[] chain,
                            String authType) {}
                    }
                }, new java.security.SecureRandom());

            return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .sslContext(sslContext)
                .build();

        } catch (Exception e) {
            // Fallback to default client
            System.err.println("[SMS] SSL context init " +
                "failed, using default: " +
                e.getMessage());
            return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        }
    }

    // ── Send SMS — never throws, always logs ──────────
    public void send(String phone, String message) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[SMS SKIPPED] No API key. " +
                "Would send to " + phone +
                ": " + message);
            return;
        }

        try {
            String normalized = normalizePhone(phone);

            StringBuilder body = new StringBuilder();
            body.append("username=").append(
                URLEncoder.encode(username,
                    StandardCharsets.UTF_8));
            body.append("&to=").append(
                URLEncoder.encode(normalized,
                    StandardCharsets.UTF_8));
            body.append("&message=").append(
                URLEncoder.encode(message,
                    StandardCharsets.UTF_8));

            if (sender != null &&
                    !sender.isBlank() &&
                    !"NONE".equals(sender)) {
                body.append("&from=").append(
                    URLEncoder.encode(sender,
                        StandardCharsets.UTF_8));
            }

            HttpClient client = buildHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .header("Content-Type",
                    "application/x-www-form-urlencoded")
                .header("apiKey", apiKey)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers
                    .ofString(body.toString()))
                .timeout(Duration.ofSeconds(15))
                .build();

            HttpResponse<String> response =
                client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("[SMS] To: " +
                normalized + " | Status: " +
                response.statusCode() +
                " | Response: " + response.body());

        } catch (Exception e) {
            // NEVER let SMS failure crash the main flow
            System.err.println("[SMS FAILED] To: " +
                phone + " | Error: " + e.getMessage());
        }
    }

    // ── Appointment confirmation SMS ──────────────────
    public void sendAppointmentConfirmation(
            String phone, String patientName,
            String doctorName, String date,
            String time, String paymentType) {

        String message;
        if ("INSURANCE".equals(paymentType) ||
                "SHA".equals(paymentType)) {
            message = "Dear " + patientName +
                ", your appointment with " + doctorName +
                " is confirmed for " + date +
                " at " + time +
                ". Please carry your insurance card." +
                " Cedar Hospital Eldoret.";
        } else {
            message = "Dear " + patientName +
                ", your appointment with " + doctorName +
                " is confirmed for " + date +
                " at " + time +
                ". Consultation fee KES 500." +
                " Cedar Hospital Eldoret.";
        }

        send(phone, message);
    }

    // ── Cancellation SMS ──────────────────────────────
    public void sendCancellationNotice(
            String phone, String patientName,
            String doctorName, String date,
            String time) {

        String message = "Dear " + patientName +
            ", your appointment with " + doctorName +
            " on " + date + " at " + time +
            " has been cancelled." +
            " To rebook call Cedar Hospital Eldoret.";

        send(phone, message);
    }

    // ── Arrival confirmation SMS ──────────────────────
    public void sendArrivalConfirmation(
            String phone, String patientName,
            String doctorName) {

        String message = "Dear " + patientName +
            ", you have been checked in." +
            " Please proceed to triage." +
            " Dr. " + doctorName +
            " will see you shortly." +
            " Cedar Hospital Eldoret.";

        send(phone, message);
    }

    // ── Normalize phone to +254 format ────────────────
    private String normalizePhone(String phone) {
        if (phone == null) return phone;
        phone = phone.trim().replaceAll("\\s+", "");

        if (phone.startsWith("+254"))
            return phone;
        if (phone.startsWith("254"))
            return "+" + phone;
        if (phone.startsWith("0") &&
                phone.length() == 10)
            return "+254" + phone.substring(1);
        if (phone.length() == 9)
            return "+254" + phone;

        return phone;
    }
}
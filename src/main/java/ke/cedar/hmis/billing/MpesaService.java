package ke.cedar.hmis.billing;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Optional;

@ApplicationScoped
public class MpesaService {

    @ConfigProperty(name = "mpesa.consumer.key", defaultValue = "SANDBOX")
    String consumerKey;

    @ConfigProperty(name = "mpesa.shortcode", defaultValue = "174379")
    String shortcode;

    public String initiateSTKPush(String phone, int amount,
            String accountRef, String description) {

        // ── DEV MODE: simulate STK push ───────────────
        // Real Daraja credentials not configured yet
        // Returns a fake CheckoutRequestID for testing
        if (consumerKey.equals("SANDBOX") || consumerKey.isBlank()) {
            System.out.println("[M-PESA SANDBOX] STK Push simulated:");
            System.out.println("  Phone: " + phone);
            System.out.println("  Amount: " + amount);
            System.out.println("  Ref: " + accountRef);
            return "ws_CO_SANDBOX_" + System.currentTimeMillis();
        }

        // ── PRODUCTION: real Daraja call goes here ────
        // Will be implemented when real credentials are available
        throw new RuntimeException("Production M-Pesa not configured yet");
    }
}

package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mpesa_callbacks")
public class MpesaCallback extends PanacheEntity {

    @Column(name = "checkout_request_id")
    public String checkoutRequestId;

    @Column(name = "merchant_request_id")
    public String merchantRequestId;

    @Column(name = "result_code")
    public Integer resultCode;

    @Column(name = "result_desc")
    public String resultDesc;

    @Column(name = "mpesa_receipt")
    public String mpesaReceipt;

    public BigDecimal amount;

    @Column(name = "phone_number")
    public String phoneNumber;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    public String rawPayload;

    public Boolean processed = false;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
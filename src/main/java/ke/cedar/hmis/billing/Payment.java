package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
public class Payment extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    public Invoice invoice;

    // MPESA, CASH, INSURANCE, CARD
    @Column(name = "payment_method", nullable = false)
    public String paymentMethod;

    @Column(nullable = false)
    public BigDecimal amount;

    @Column(name = "reference_no")
    public String referenceNo;

    @Column(name = "mpesa_receipt")
    public String mpesaReceipt;

    @Column(name = "phone_number")
    public String phoneNumber;

    public Boolean verified = false;

    public String cashier;
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<Payment> findByInvoice(Long invoiceId) {
        return find("invoice.id", invoiceId).list();
    }
}
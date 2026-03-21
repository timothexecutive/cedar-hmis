package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    public Invoice invoice;

    @Column(nullable = false)
    public String description;

    // CONSULTATION, LAB, PHARMACY, BED, PROCEDURE, OTHER
    public String category;

    public Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    public BigDecimal unitPrice;

    @Column(nullable = false)
    public BigDecimal total;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        total     = unitPrice.multiply(
            BigDecimal.valueOf(quantity));
    }

    public static List<InvoiceItem> findByInvoice(Long invoiceId) {
        return find("invoice.id", invoiceId).list();
    }
}
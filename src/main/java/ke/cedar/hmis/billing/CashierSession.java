package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cashier_sessions")
public class CashierSession extends PanacheEntity {

    @Column(nullable = false)
    public String cashier;

    @Column(name = "opening_float")
    public BigDecimal openingFloat = BigDecimal.ZERO;

    @Column(name = "expected_cash")
    public BigDecimal expectedCash = BigDecimal.ZERO;

    @Column(name = "actual_cash")
    public BigDecimal actualCash   = BigDecimal.ZERO;

    public BigDecimal variance     = BigDecimal.ZERO;

    // OPEN, CLOSED
    public String status = "OPEN";

    @Column(name = "opened_at")
    public LocalDateTime openedAt;

    @Column(name = "closed_at")
    public LocalDateTime closedAt;

    public String notes;

    @PrePersist
    public void onCreate() {
        openedAt = LocalDateTime.now();
    }

    public static List<CashierSession> findOpen() {
        return find("status", "OPEN").list();
    }

    public static CashierSession findOpenByName(String cashier) {
        return find("cashier = ?1 AND status = 'OPEN'",
                cashier).firstResult();
    }
}
package ke.cedar.hmis.inventory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class InventoryService {

    // ── Create inventory item ─────────────────────────
    @Transactional
    public InventoryItem createItem(
            InventoryItem request,
            Long userId, String userName) {

        if (InventoryItem.find("code",
                request.code).firstResult() != null) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Item code " +
                            "already exists\"}")
                    .build());
        }

        request.createdBy = userName;
        request.persist();

        AuditTrail.log(
            userId, userName, "ADMIN",
            "INVENTORY_ITEM_CREATED", "INVENTORY",
            "InventoryItem", String.valueOf(request.id),
            "Item created: " + request.name +
            " | Code: " + request.code +
            " | Category: " + request.category +
            " | Reorder level: " + request.reorderLevel +
            " | By: " + userName);

        return request;
    }

    // ── Create supplier ───────────────────────────────
    @Transactional
    public Supplier createSupplier(Supplier request,
            Long userId, String userName) {

        if (Supplier.find("code",
                request.code).firstResult() != null) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Supplier code " +
                            "already exists\"}")
                    .build());
        }

        request.persist();

        AuditTrail.log(
            userId, userName, "ADMIN",
            "SUPPLIER_CREATED", "INVENTORY",
            "Supplier", String.valueOf(request.id),
            "Supplier created: " + request.name +
            " | Code: " + request.code +
            " | Type: " + request.supplierType +
            " | By: " + userName);

        return request;
    }

    // ── Raise purchase order ──────────────────────────
    @Transactional
    public PurchaseOrder raisePurchaseOrder(
            Long supplierId,
            PurchaseOrder request,
            List<Map<String, Object>> items,
            Long userId, String userName) {

        Supplier supplier = Supplier.findById(supplierId);
        if (supplier == null || !supplier.isActive) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Supplier " +
                            "not found\"}")
                    .build());
        }

        String lpoNo = "LPO-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                PurchaseOrder.count() + 1);

        request.lpoNo    = lpoNo;
        request.supplier = supplier;
        request.raisedBy = userName;
        request.status   = "SUBMITTED";
        request.persist();

        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> item : items) {
            InventoryItem invItem =
                InventoryItem.findById(Long.parseLong(
                    item.get("itemId").toString()));
            if (invItem == null) continue;

            PurchaseOrderItem poi =
                new PurchaseOrderItem();
            poi.purchaseOrder = request;
            poi.item          = invItem;
            poi.quantity      = Integer.parseInt(
                item.get("quantity").toString());
            poi.unitCost      = new BigDecimal(
                item.get("unitCost").toString());
            poi.total         = poi.unitCost.multiply(
                BigDecimal.valueOf(poi.quantity));
            poi.persist();
            total = total.add(poi.total);
        }

        request.totalAmount = total;
        request.persist();

        AuditTrail.log(
            userId, userName, "ADMIN",
            "LPO_RAISED", "INVENTORY",
            "PurchaseOrder", String.valueOf(request.id),
            "LPO raised: " + lpoNo +
            " | Supplier: " + supplier.name +
            " | Total: KES " + total +
            " | Items: " + items.size() +
            " | By: " + userName);

        return request;
    }

    // ── Approve purchase order ────────────────────────
    @Transactional
    public PurchaseOrder approvePurchaseOrder(
            Long poId, boolean approved,
            String reason,
            Long userId, String userName) {

        PurchaseOrder po = PurchaseOrder.findById(poId);
        if (po == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Purchase order " +
                            "not found\"}")
                    .build());
        }

        if (!po.status.equals("SUBMITTED")) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Only submitted " +
                            "orders can be approved\"}")
                    .build());
        }

        String oldStatus = po.status;
        po.status     = approved ? "APPROVED" : "REJECTED";
        po.approvedBy = userName;
        po.approvedAt = LocalDateTime.now();
        if (!approved) {
            if (reason == null || reason.isBlank()) {
                throw new WebApplicationException(
                    Response.status(400)
                        .entity("{\"error\":\"Rejection " +
                                "reason is mandatory\"}")
                        .build());
            }
            po.rejectionReason = reason;
        }
        po.persist();

        AuditTrail.logChange(
            userId, userName, "ADMIN",
            approved ? "LPO_APPROVED" : "LPO_REJECTED",
            "INVENTORY",
            "PurchaseOrder", String.valueOf(poId),
            oldStatus, po.status,
            "LPO " + po.lpoNo + " " + po.status +
            " | Supplier: " + po.supplier.name +
            " | Total: KES " + po.totalAmount +
            (reason != null ?
                " | Reason: " + reason : "") +
            " | By: " + userName);

        return po;
    }

    // ── Receive goods (GRN) ───────────────────────────
    @Transactional
    public GoodsReceived receiveGoods(
            Long supplierId, Long poId,
            GoodsReceived request,
            List<Map<String, Object>> items,
            Long userId, String userName) {

        Supplier supplier = Supplier.findById(supplierId);
        if (supplier == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Supplier " +
                            "not found\"}")
                    .build());
        }

        String grnNo = "GRN-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                GoodsReceived.count() + 1);

        request.grnNo       = grnNo;
        request.supplier    = supplier;
        request.receivedBy  = userName;
        request.status      = "RECEIVED";

        if (poId != null) {
            PurchaseOrder po =
                PurchaseOrder.findById(poId);
            if (po != null) {
                request.purchaseOrder = po;
                po.status = "DELIVERED";
                po.persist();
            }
        }

        request.persist();

        int totalItemsReceived = 0;
        for (Map<String, Object> item : items) {
            InventoryItem invItem =
                InventoryItem.findById(Long.parseLong(
                    item.get("itemId").toString()));
            if (invItem == null) continue;

            Integer qty = Integer.parseInt(
                item.get("quantity").toString());
            BigDecimal unitCost =
                item.get("unitCost") != null
                ? new BigDecimal(
                    item.get("unitCost").toString())
                : BigDecimal.ZERO;
            String batchNo =
                item.get("batchNo") != null
                ? item.get("batchNo").toString()
                : "BATCH-" + grnNo;
            LocalDate expiryDate =
                item.get("expiryDate") != null
                ? LocalDate.parse(
                    item.get("expiryDate").toString())
                : null;

            // Create inventory batch — FIFO
            InventoryBatch batch = new InventoryBatch();
            batch.item               = invItem;
            batch.batchNo            = batchNo;
            batch.supplier           = supplier;
            batch.quantityReceived   = qty;
            batch.quantityRemaining  = qty;
            batch.unitCost           = unitCost;
            batch.expiryDate         = expiryDate;
            batch.receivedBy         = userName;
            batch.receivedDate       = LocalDate.now();
            if (item.get("storageLocation") != null) {
                batch.storageLocation =
                    item.get("storageLocation").toString();
            }
            batch.persist();

            // Record GRN line item
            GoodsReceivedItem grnItem =
                new GoodsReceivedItem();
            grnItem.goodsReceived = request;
            grnItem.item          = invItem;
            grnItem.batch         = batch;
            grnItem.quantity      = qty;
            grnItem.unitCost      = unitCost;
            grnItem.total         = unitCost.multiply(
                BigDecimal.valueOf(qty));
            grnItem.batchNo       = batchNo;
            grnItem.expiryDate    = expiryDate;
            grnItem.persist();

            totalItemsReceived += qty;

            AuditTrail.log(
                userId, userName, "ADMIN",
                "GOODS_RECEIVED", "INVENTORY",
                "InventoryBatch",
                String.valueOf(batch.id),
                "Goods received: " + invItem.name +
                " | Qty: " + qty +
                " | Batch: " + batchNo +
                " | Expiry: " + expiryDate +
                " | GRN: " + grnNo +
                " | By: " + userName);
        }

        AuditTrail.log(
            userId, userName, "ADMIN",
            "GRN_CREATED", "INVENTORY",
            "GoodsReceived", String.valueOf(request.id),
            "GRN: " + grnNo +
            " | Supplier: " + supplier.name +
            " | Total items: " + totalItemsReceived +
            " | By: " + userName);

        return request;
    }

    // ── Issue stock to department ─────────────────────
    @Transactional
    public StockIssuance issueStock(
            String department, String purpose,
            Long patientId,
            List<Map<String, Object>> items,
            Long userId, String userName) {

        if (items == null || items.isEmpty()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"No items to " +
                            "issue\"}")
                    .build());
        }

        String issuanceNo = "ISS-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                StockIssuance.count() + 1);

        StockIssuance issuance = new StockIssuance();
        issuance.issuanceNo  = issuanceNo;
        issuance.department  = department;
        issuance.issuedBy    = userName;
        issuance.purpose     = purpose;
        issuance.status      = "ISSUED";

        if (patientId != null) {
            ke.cedar.hmis.reception.Patient patient =
                ke.cedar.hmis.reception.Patient
                    .findById(patientId);
            if (patient != null)
                issuance.patient = patient;
        }

        issuance.persist();

        for (Map<String, Object> item : items) {
            Long itemId = Long.parseLong(
                item.get("itemId").toString());
            Integer qty = Integer.parseInt(
                item.get("quantity").toString());

            InventoryItem invItem =
                InventoryItem.findById(itemId);
            if (invItem == null) continue;

            // Check total available stock
            int totalStock = invItem.getCurrentStock();
            if (totalStock < qty) {
                throw new WebApplicationException(
                    Response.status(409)
                        .entity("{\"error\":\"Insufficient " +
                                "stock for " +
                                invItem.name +
                                ". Available: " +
                                totalStock + "\"}")
                        .build());
            }

            // FIFO deduction
            List<InventoryBatch> batches =
                InventoryBatch.findFIFO(itemId);
            int remaining = qty;

            for (InventoryBatch batch : batches) {
                if (remaining <= 0) break;

                int deduct = Math.min(
                    remaining,
                    batch.quantityRemaining);
                int qtyBefore = batch.quantityRemaining;
                batch.quantityRemaining -= deduct;

                if (batch.quantityRemaining == 0) {
                    batch.isActive = false;
                }
                batch.persist();
                remaining -= deduct;

                // Record issuance item per batch
                StockIssuanceItem si =
                    new StockIssuanceItem();
                si.issuance  = issuance;
                si.item      = invItem;
                si.batch     = batch;
                si.quantity  = deduct;
                si.unitCost  = batch.unitCost;
                si.total     = batch.unitCost != null
                    ? batch.unitCost.multiply(
                        BigDecimal.valueOf(deduct))
                    : BigDecimal.ZERO;
                si.persist();

                AuditTrail.log(
                    userId, userName, "ADMIN",
                    "STOCK_ISSUED", "INVENTORY",
                    "InventoryBatch",
                    String.valueOf(batch.id),
                    "Stock issued: " + invItem.name +
                    " | Qty: " + deduct +
                    " | Batch: " + batch.batchNo +
                    " | To: " + department +
                    " | ISS: " + issuanceNo +
                    " | Stock before: " + qtyBefore +
                    " | Stock after: " +
                    batch.quantityRemaining +
                    " | By: " + userName);
            }
        }

        AuditTrail.log(
            userId, userName, "ADMIN",
            "ISSUANCE_COMPLETED", "INVENTORY",
            "StockIssuance",
            String.valueOf(issuance.id),
            "Issuance: " + issuanceNo +
            " | Dept: " + department +
            " | Items: " + items.size() +
            " | By: " + userName);

        return issuance;
    }

    // ── Stock adjustment ──────────────────────────────
    @Transactional
    public StockAdjustment adjustStock(
            Long itemId, Long batchId,
            String adjustmentType,
            Integer quantityChange,
            String reason,
            Long userId, String userName) {

        InventoryItem item =
            InventoryItem.findById(itemId);
        if (item == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Item not found\"}")
                    .build());
        }

        if (reason == null || reason.isBlank()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Adjustment " +
                            "reason is mandatory\"}")
                    .build());
        }

        InventoryBatch batch = null;
        int qtyBefore        = 0;

        if (batchId != null) {
            batch = InventoryBatch.findById(batchId);
            if (batch == null) {
                throw new WebApplicationException(
                    Response.status(404)
                        .entity("{\"error\":\"Batch " +
                                "not found\"}")
                        .build());
            }
            qtyBefore = batch.quantityRemaining;
        } else {
            qtyBefore = item.getCurrentStock();
        }

        int qtyAfter = qtyBefore + quantityChange;
        if (qtyAfter < 0) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Adjustment " +
                            "would result in negative stock\"}")
                    .build());
        }

        // Apply to batch if specified
        if (batch != null) {
            batch.quantityRemaining = qtyAfter;
            if ("EXPIRY_REMOVAL".equals(adjustmentType)
                    || "WRITE_OFF".equals(adjustmentType)) {
                batch.isExpired =
                    "EXPIRY_REMOVAL".equals(adjustmentType);
                batch.isActive  = false;
            }
            batch.persist();
        }

        String adjNo = "ADJ-" +
            LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyyMMdd"))
            + "-" + String.format("%04d",
                StockAdjustment.count() + 1);

        StockAdjustment adjustment = new StockAdjustment();
        adjustment.adjustmentNo  = adjNo;
        adjustment.item          = item;
        adjustment.batch         = batch;
        adjustment.adjustmentType = adjustmentType;
        adjustment.quantityBefore = qtyBefore;
        adjustment.quantityChange = quantityChange;
        adjustment.quantityAfter  = qtyAfter;
        adjustment.reason         = reason;
        adjustment.adjustedBy     = userName;
        adjustment.approvedBy     = userName;
        adjustment.persist();

        AuditTrail.logChange(
            userId, userName, "ADMIN",
            "STOCK_ADJUSTED", "INVENTORY",
            "InventoryItem", String.valueOf(itemId),
            String.valueOf(qtyBefore),
            String.valueOf(qtyAfter),
            "Adjustment: " + adjNo +
            " | Item: " + item.name +
            " | Type: " + adjustmentType +
            " | Change: " + quantityChange +
            " | Reason: " + reason +
            " | By: " + userName);

        return adjustment;
    }

    // ── Flag expired batches ──────────────────────────
    @Transactional
    public int flagExpiredBatches(
            Long userId, String userName) {
        List<InventoryBatch> batches =
            InventoryBatch.find(
                "expiryDate < ?1 " +
                "AND isExpired = false " +
                "AND isActive = true",
                LocalDate.now()).list();

        int count = 0;
        for (InventoryBatch batch : batches) {
            batch.isExpired = true;
            batch.persist();
            count++;

            AuditTrail.log(
                userId, userName, "SYSTEM",
                "BATCH_EXPIRED", "INVENTORY",
                "InventoryBatch",
                String.valueOf(batch.id),
                "Batch expired: " +
                batch.item.name +
                " | Batch: " + batch.batchNo +
                " | Expiry: " + batch.expiryDate +
                " | Qty wasted: " +
                batch.quantityRemaining);
        }
        return count;
    }

    // ── Queries ───────────────────────────────────────
    public List<Supplier> getAllSuppliers() {
        return Supplier.findAllActive();
    }

    public List<InventoryItem> getAllItems() {
        return InventoryItem.findAllActive();
    }

    public List<InventoryItem> getItemsByCategory(
            String category) {
        return InventoryItem.findByCategory(category);
    }

    public List<InventoryItem> searchItems(String q) {
        return InventoryItem.search(q);
    }

    public List<InventoryBatch> getItemBatches(
            Long itemId) {
        return InventoryBatch.findByItem(itemId);
    }

    public List<InventoryBatch> getExpiring(int days) {
        return InventoryBatch.findExpiring(days);
    }

    public List<InventoryBatch> getExpired() {
        return InventoryBatch.findExpired();
    }

    public List<InventoryItem> getLowStockItems() {
        return InventoryItem.findAllActive().stream()
            .filter(InventoryItem::isBelowReorderLevel)
            .collect(java.util.stream.Collectors
                .toList());
    }

    public List<PurchaseOrder> getPurchaseOrders(
            String status) {
        return status != null
            ? PurchaseOrder.findByStatus(status)
            : PurchaseOrder.findAll().list();
    }

    public List<PurchaseOrderItem> getPOItems(Long poId) {
        return PurchaseOrderItem.findByPO(poId);
    }

    public List<PurchaseOrder> getPendingApproval() {
        return PurchaseOrder.findPendingApproval();
    }

    public List<GoodsReceived> getGRNs(String status) {
        return status != null
            ? GoodsReceived.findByStatus(status)
            : GoodsReceived.findAll().list();
    }

    public List<GoodsReceivedItem> getGRNItems(
            Long grnId) {
        return GoodsReceivedItem.findByGRN(grnId);
    }

    public List<StockIssuance> getIssuancesByDept(
            String dept) {
        return StockIssuance.findByDepartment(dept);
    }

    public List<StockIssuanceItem> getIssuanceItems(
            Long issuanceId) {
        return StockIssuanceItem.findByIssuance(
            issuanceId);
    }

    public List<StockAdjustment> getAdjustments(
            Long itemId) {
        return itemId != null
            ? StockAdjustment.findByItem(itemId)
            : StockAdjustment.findRecent();
    }
}
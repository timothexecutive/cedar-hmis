package ke.cedar.hmis.inventory;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import java.util.List;
import java.util.Map;

@Path("/api/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @Inject InventoryService inventoryService;
    @Inject JsonWebToken     jwt;

    // ── Helper: safe JWT userId extraction ───────────
    private Long getUserId() {
        Object claim = jwt.getClaim("userId");
        return claim != null ?
            Long.parseLong(claim.toString()) : null;
    }

    // ── Suppliers ─────────────────────────────────────
    @GET
    @Path("/suppliers")
    @RolesAllowed({"ADMIN","PHARMACIST"})
    public List<Supplier> getSuppliers() {
        return inventoryService.getAllSuppliers();
    }

    @POST
    @Path("/suppliers")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response createSupplier(Supplier request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        return Response.status(201)
            .entity(inventoryService.createSupplier(
                request, userId, userName))
            .build();
    }

    // ── Inventory Items ───────────────────────────────
    @GET
    @Path("/items")
    @RolesAllowed({"ADMIN","PHARMACIST","NURSE",
                   "DOCTOR","LAB_TECH"})
    public List<InventoryItem> getAllItems() {
        return inventoryService.getAllItems();
    }

    @GET
    @Path("/items/search")
    @RolesAllowed({"ADMIN","PHARMACIST","NURSE",
                   "DOCTOR","LAB_TECH"})
    public List<InventoryItem> searchItems(
            @QueryParam("q") String query) {
        return inventoryService.searchItems(query);
    }

    @GET
    @Path("/items/category/{category}")
    @RolesAllowed({"ADMIN","PHARMACIST","NURSE",
                   "DOCTOR","LAB_TECH"})
    public List<InventoryItem> getByCategory(
            @PathParam("category") String category) {
        return inventoryService
            .getItemsByCategory(category);
    }

    @GET
    @Path("/items/low-stock")
    @RolesAllowed({"ADMIN","PHARMACIST"})
    public List<InventoryItem> getLowStock() {
        return inventoryService.getLowStockItems();
    }

    @POST
    @Path("/items")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response createItem(InventoryItem request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        return Response.status(201)
            .entity(inventoryService.createItem(
                request, userId, userName))
            .build();
    }

    // ── Batches ───────────────────────────────────────
    @GET
    @Path("/items/{itemId}/batches")
    @RolesAllowed({"ADMIN","PHARMACIST"})
    public List<InventoryBatch> getItemBatches(
            @PathParam("itemId") Long itemId) {
        return inventoryService.getItemBatches(itemId);
    }

    @GET
    @Path("/batches/expiring/{days}")
    @RolesAllowed({"ADMIN","PHARMACIST"})
    public List<InventoryBatch> getExpiring(
            @PathParam("days") int days) {
        return inventoryService.getExpiring(days);
    }

    @GET
    @Path("/batches/expired")
    @RolesAllowed({"ADMIN","PHARMACIST"})
    public List<InventoryBatch> getExpired() {
        return inventoryService.getExpired();
    }

    @POST
    @Path("/batches/flag-expired")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response flagExpired() {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        int count = inventoryService
            .flagExpiredBatches(userId, userName);
        return Response.ok(Map.of(
            "message", count + " expired batches flagged",
            "count", count)).build();
    }

    // ── Purchase Orders ───────────────────────────────
    @GET
    @Path("/purchase-orders")
    @RolesAllowed({"ADMIN"})
    public List<PurchaseOrder> getPurchaseOrders(
            @QueryParam("status") String status) {
        return inventoryService.getPurchaseOrders(status);
    }

    @GET
    @Path("/purchase-orders/pending-approval")
    @RolesAllowed({"ADMIN"})
    public List<PurchaseOrder> getPendingApproval() {
        return inventoryService.getPendingApproval();
    }

    @GET
    @Path("/purchase-orders/{poId}/items")
    @RolesAllowed({"ADMIN"})
    public List<PurchaseOrderItem> getPOItems(
            @PathParam("poId") Long poId) {
        return inventoryService.getPOItems(poId);
    }

    @POST
    @Path("/purchase-orders/supplier/{supplierId}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    @SuppressWarnings("unchecked")
    public Response raisePO(
            @PathParam("supplierId") Long supplierId,
            Map<String, Object> request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();

        PurchaseOrder po = new PurchaseOrder();
        po.expectedDate  = request.get("expectedDate")
            != null ? java.time.LocalDate.parse(
                request.get("expectedDate").toString())
            : null;
        po.paymentTerms  =
            (String) request.get("paymentTerms");
        po.notes         =
            (String) request.get("notes");

        List<Map<String, Object>> items =
            (List<Map<String, Object>>)
            request.get("items");

        return Response.status(201)
            .entity(inventoryService.raisePurchaseOrder(
                supplierId, po, items,
                userId, userName))
            .build();
    }

    @PUT
    @Path("/purchase-orders/{poId}/approve")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response approvePO(
            @PathParam("poId") Long poId,
            Map<String, Object> request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();
        boolean approved = Boolean.parseBoolean(
            request.get("approved").toString());
        String reason =
            (String) request.get("reason");
        return Response.ok(
            inventoryService.approvePurchaseOrder(
                poId, approved, reason,
                userId, userName))
            .build();
    }

    // ── Goods Received ────────────────────────────────
    @GET
    @Path("/grn")
    @RolesAllowed({"ADMIN"})
    public List<GoodsReceived> getGRNs(
            @QueryParam("status") String status) {
        return inventoryService.getGRNs(status);
    }

    @GET
    @Path("/grn/{grnId}/items")
    @RolesAllowed({"ADMIN"})
    public List<GoodsReceivedItem> getGRNItems(
            @PathParam("grnId") Long grnId) {
        return inventoryService.getGRNItems(grnId);
    }

    @POST
    @Path("/grn/supplier/{supplierId}")
    @Transactional
    @RolesAllowed({"ADMIN"})
    @SuppressWarnings("unchecked")
    public Response receiveGoods(
            @PathParam("supplierId") Long supplierId,
            Map<String, Object> request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();

        GoodsReceived grn = new GoodsReceived();
        grn.invoiceNo   =
            (String) request.get("invoiceNo");
        grn.invoiceDate = request.get("invoiceDate")
            != null ? java.time.LocalDate.parse(
                request.get("invoiceDate").toString())
            : null;
        grn.notes       =
            (String) request.get("notes");

        Long poId = request.get("poId") != null
            ? Long.parseLong(
                request.get("poId").toString()) : null;

        List<Map<String, Object>> items =
            (List<Map<String, Object>>)
            request.get("items");

        return Response.status(201)
            .entity(inventoryService.receiveGoods(
                supplierId, poId, grn,
                items, userId, userName))
            .build();
    }

    // ── Stock Issuance ────────────────────────────────
    @GET
    @Path("/issuances")
    @RolesAllowed({"ADMIN"})
    public List<StockIssuance> getIssuances(
            @QueryParam("department") String dept) {
        return inventoryService.getIssuancesByDept(dept);
    }

    @GET
    @Path("/issuances/{issuanceId}/items")
    @RolesAllowed({"ADMIN","PHARMACIST","NURSE"})
    public List<StockIssuanceItem> getIssuanceItems(
            @PathParam("issuanceId") Long issuanceId) {
        return inventoryService
            .getIssuanceItems(issuanceId);
    }

    @POST
    @Path("/issuances")
    @Transactional
    @RolesAllowed({"ADMIN","PHARMACIST"})
    @SuppressWarnings("unchecked")
    public Response issueStock(
            Map<String, Object> request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();

        String department =
            (String) request.get("department");
        String purpose    =
            (String) request.get("purpose");
        Long patientId = request.get("patientId") != null
            ? Long.parseLong(
                request.get("patientId").toString())
            : null;

        List<Map<String, Object>> items =
            (List<Map<String, Object>>)
            request.get("items");

        return Response.status(201)
            .entity(inventoryService.issueStock(
                department, purpose,
                patientId, items,
                userId, userName))
            .build();
    }

    // ── Stock Adjustments ─────────────────────────────
    @GET
    @Path("/adjustments")
    @RolesAllowed({"ADMIN"})
    public List<StockAdjustment> getAdjustments(
            @QueryParam("itemId") Long itemId) {
        return inventoryService.getAdjustments(itemId);
    }

    @POST
    @Path("/adjustments")
    @Transactional
    @RolesAllowed({"ADMIN"})
    public Response adjustStock(
            Map<String, Object> request) {
        Long   userId   = getUserId();
        String userName = jwt.getName();

        if (request.get("reason") == null ||
                request.get("reason").toString().isBlank()) {
            throw new WebApplicationException(
                Response.status(400)
                    .entity("{\"error\":\"Adjustment " +
                            "reason is mandatory\"}")
                    .build());
        }

        return Response.status(201)
            .entity(inventoryService.adjustStock(
                Long.parseLong(
                    request.get("itemId").toString()),
                request.get("batchId") != null
                    ? Long.parseLong(
                        request.get("batchId").toString())
                    : null,
                (String) request.get("adjustmentType"),
                Integer.parseInt(
                    request.get("quantityChange")
                    .toString()),
                (String) request.get("reason"),
                userId, userName))
            .build();
    }
}
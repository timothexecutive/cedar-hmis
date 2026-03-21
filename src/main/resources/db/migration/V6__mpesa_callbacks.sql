-- V6: Fix sequences and add missing indexes

-- Fix payments sequence to avoid conflicts
SELECT setval('payments_id_seq',
    GREATEST((SELECT MAX(id) FROM payments), 1) + 50);

-- Fix invoices sequence
SELECT setval('invoices_id_seq',
    GREATEST((SELECT MAX(id) FROM invoices), 1) + 50);

-- Fix invoice_items sequence
SELECT setval('invoice_items_id_seq',
    GREATEST((SELECT MAX(id) FROM invoice_items), 1) + 50);

-- Fix insurance_claims sequence
SELECT setval('insurance_claims_id_seq',
    GREATEST((SELECT MAX(id) FROM insurance_claims), 1) + 50);

-- Add index on mpesa_callbacks if not exists
CREATE INDEX IF NOT EXISTS idx_mpesa_callbacks_checkout
    ON mpesa_callbacks(checkout_request_id);
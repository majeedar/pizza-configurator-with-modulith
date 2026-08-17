package com.example.pizzaconfigurator.pricing.application;

import com.example.pizzaconfigurator.admin.api.AuditEntry;
import com.example.pizzaconfigurator.admin.api.AuditLog;
import com.example.pizzaconfigurator.pricing.domain.ItemType;
import com.example.pizzaconfigurator.pricing.domain.PriceDefinition;
import com.example.pizzaconfigurator.pricing.infrastructure.persistence.PriceDefinitionRepository;
import com.example.pizzaconfigurator.shared.CurrentStaffActor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Admin write operations for price configuration (agent.md §7.10, §9.3).
 * Not a published module API — see the CatalogAdminService/RuleAdminService
 * precedent.
 */
@Service
@Transactional
public class PriceAdminService {

    private final PriceDefinitionRepository prices;
    private final AuditLog auditLog;
    private final JsonMapper jsonMapper;

    PriceAdminService(PriceDefinitionRepository prices, AuditLog auditLog, JsonMapper jsonMapper) {
        this.prices = prices;
        this.auditLog = auditLog;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public List<PriceDefinition> findAll() {
        return prices.findAll();
    }

    @Transactional(readOnly = true)
    public PriceDefinition getPrice(UUID priceId) {
        return prices.findById(priceId).orElseThrow(() -> new PriceAdminNotFoundException(priceId));
    }

    public PriceDefinition createPrice(
        ItemType itemType, String itemId, BigDecimal amount, String currency, boolean active, Instant validFrom, Instant validTo
    ) {
        PriceDefinition price = prices.save(new PriceDefinition(itemType, itemId, amount, currency, active, validFrom, validTo));
        audit("PRICE_CREATED", price.getPriceId(), null, price);
        return price;
    }

    public PriceDefinition updatePrice(
        UUID priceId, BigDecimal amount, String currency, boolean active, Instant validFrom, Instant validTo
    ) {
        PriceDefinition price = getPrice(priceId);
        String before = jsonMapper.writeValueAsString(price);
        price.update(amount, currency, active, validFrom, validTo);
        audit("PRICE_UPDATED", price.getPriceId(), before, price);
        return price;
    }

    private void audit(String action, UUID priceId, String beforeJson, PriceDefinition after) {
        auditLog.record(new AuditEntry(
            CurrentStaffActor.username(), CurrentStaffActor.role(), action, "PriceDefinition", priceId.toString(),
            beforeJson, jsonMapper.writeValueAsString(after)));
    }
}

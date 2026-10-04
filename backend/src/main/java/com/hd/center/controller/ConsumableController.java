package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.entity.Consumable;
import com.hd.center.entity.ConsumableTxn;
import com.hd.center.entity.Enums.TxnType;
import com.hd.center.repository.ConsumableRepository;
import com.hd.center.repository.ConsumableTxnRepository;
import com.hd.center.service.ConsumableService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/consumables")
@RequiredArgsConstructor
public class ConsumableController {

    private final ConsumableRepository consumableRepo;
    private final ConsumableTxnRepository txnRepo;
    private final ConsumableService consumableService;

    @GetMapping
    public List<Consumable> list() {
        return consumableRepo.findAllByOrderByCode();
    }

    @PostMapping
    public Consumable create(@RequestBody Consumable consumable) {
        consumable.setId(null);
        return consumableRepo.save(consumable);
    }

    @PutMapping("/{id}")
    public Consumable update(@PathVariable Long id, @RequestBody Consumable body) {
        Consumable c = get(id);
        c.setName(body.getName());
        c.setSpec(body.getSpec());
        c.setUnit(body.getUnit());
        c.setSafeStock(body.getSafeStock());
        c.setSelfPay(body.getSelfPay());
        c.setPrice(body.getPrice());
        return consumableRepo.save(c);
    }

    public record TxnRequest(@NotNull TxnType type, @NotNull Integer qty, Long sessionId,
                             String patientName, String note) {}

    /** 耗材出入库（使用/归还/损耗/入库/预领） */
    @PostMapping("/{id}/txn")
    public ConsumableTxn transact(@PathVariable Long id, @RequestBody TxnRequest request,
                                  @AuthenticationPrincipal CurrentUser current) {
        Consumable c = get(id);
        return consumableService.transact(c, request.type(), request.qty(), request.sessionId(),
                request.patientName(), current.name(), request.note());
    }

    @GetMapping("/txns")
    public List<ConsumableTxn> txns(@RequestParam(required = false) Long sessionId) {
        if (sessionId != null) {
            return txnRepo.findBySessionId(sessionId);
        }
        return txnRepo.findTop100ByOrderByCreatedAtDesc();
    }

    /** 库存预警：低于安全库存的耗材 */
    @GetMapping("/warnings")
    public List<Map<String, Object>> warnings() {
        return consumableRepo.findAllByOrderByCode().stream()
                .filter(c -> c.getStock() <= c.getSafeStock())
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "name", c.getName(),
                        "stock", c.getStock(),
                        "safeStock", c.getSafeStock(),
                        "unit", c.getUnit() == null ? "" : c.getUnit()))
                .toList();
    }

    private Consumable get(Long id) {
        return consumableRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "耗材不存在"));
    }
}

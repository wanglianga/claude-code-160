package com.hdms.service;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.SecurityUtils;
import com.hdms.model.*;
import com.hdms.model.enums.ExceptionType;
import com.hdms.model.enums.TxnType;
import com.hdms.repo.ConsumableRepo;
import com.hdms.repo.ConsumableTxnRepo;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsumableService {
    private final ConsumableRepo consumableRepo;
    private final ConsumableTxnRepo txnRepo;
    private final ExceptionService exceptionService;

    public ConsumableService(ConsumableRepo consumableRepo, ConsumableTxnRepo txnRepo,
                             @Lazy ExceptionService exceptionService) {
        this.consumableRepo = consumableRepo;
        this.txnRepo = txnRepo;
        this.exceptionService = exceptionService;
    }

    @Transactional(readOnly = true)
    public List<Consumable> list() {
        return consumableRepo.findAllByOrderByCode();
    }

    @Transactional(readOnly = true)
    public List<ConsumableTxn> recentTxns() {
        return txnRepo.findTop100ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ConsumableTxn> txnsOfSchedule(Long scheduleId) {
        return txnRepo.findByScheduleId(scheduleId);
    }

    /**
     * 耗材出入库：PRE_DRAW 预领 / USE 使用 / RETURN 退回 / LOSS 损耗 / RESTOCK 入库
     */
    @Transactional
    public ConsumableTxn transact(Long consumableId, Long scheduleId, TxnType type, Integer quantity, String note,
                                  Schedule scheduleRef) {
        if (quantity == null || quantity <= 0) throw new BizException("数量必须为正数");
        Consumable c = consumableRepo.findById(consumableId).orElseThrow(() -> new BizException("耗材不存在"));
        int delta = switch (type) {
            case PRE_DRAW, USE, LOSS -> -quantity;
            case RETURN, RESTOCK -> quantity;
        };
        int newStock = c.getStock() + delta;
        if (newStock < 0) throw new BizException(c.getName() + " 库存不足，当前库存 " + c.getStock());
        c.setStock(newStock);
        consumableRepo.save(c);

        ConsumableTxn txn = new ConsumableTxn();
        txn.setConsumable(c);
        txn.setSchedule(scheduleRef);
        txn.setQuantity(quantity);
        txn.setType(type);
        txn.setNote(note);
        try {
            txn.setOperator(SecurityUtils.currentUser());
        } catch (Exception ignored) {}
        txnRepo.save(txn);

        // 库存低于安全库存 → 自动上报耗材不足异常
        if (c.getStock() < c.getSafetyStock()) {
            exceptionService.create(ExceptionType.CONSUMABLE_SHORTAGE, scheduleRef, null, null, null,
                    c.getName() + " 库存 " + c.getStock() + " 低于安全库存 " + c.getSafetyStock());
        }
        return txn;
    }

    /** 排班取消/患者未到时，把该排班预领且未使用的耗材退回 */
    @Transactional
    public void returnPreDrawn(Schedule schedule, String reason) {
        List<ConsumableTxn> preDrawn = txnRepo.findByScheduleIdAndType(schedule.getId(), TxnType.PRE_DRAW);
        List<ConsumableTxn> used = txnRepo.findByScheduleIdAndType(schedule.getId(), TxnType.USE);
        int usedCount = used.stream().mapToInt(ConsumableTxn::getQuantity).sum();
        for (ConsumableTxn t : preDrawn) {
            Consumable c = t.getConsumable();
            c.setStock(c.getStock() + t.getQuantity());
            consumableRepo.save(c);
            ConsumableTxn back = new ConsumableTxn();
            back.setConsumable(c);
            back.setSchedule(schedule);
            back.setQuantity(t.getQuantity());
            back.setType(TxnType.RETURN);
            back.setNote(reason);
            try {
                back.setOperator(SecurityUtils.currentUser());
            } catch (Exception ignored) {}
            txnRepo.save(back);
        }
    }

    /** 治疗完成时，把该排班的预领耗材转为已使用 */
    @Transactional
    public void consumePreDrawn(Schedule schedule) {
        List<ConsumableTxn> preDrawn = txnRepo.findByScheduleIdAndType(schedule.getId(), TxnType.PRE_DRAW);
        for (ConsumableTxn t : preDrawn) {
            ConsumableTxn use = new ConsumableTxn();
            use.setConsumable(t.getConsumable());
            use.setSchedule(schedule);
            use.setQuantity(t.getQuantity());
            use.setType(TxnType.USE);
            use.setNote("治疗完成，预领转使用");
            try {
                use.setOperator(SecurityUtils.currentUser());
            } catch (Exception ignored) {}
            txnRepo.save(use);
        }
    }
}

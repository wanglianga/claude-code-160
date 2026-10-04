package com.hd.center.service;

import com.hd.center.entity.*;
import com.hd.center.entity.Enums.BillingStatus;
import com.hd.center.entity.Enums.TxnType;
import com.hd.center.repository.BillingRecordRepository;
import com.hd.center.repository.ConsumableRepository;
import com.hd.center.repository.ConsumableTxnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 收费核对：治疗结束后自动生成待核对单，财务确认医保、自费耗材与补收费用。
 */
@Service
@RequiredArgsConstructor
public class BillingService {

    /** 单次透析医保结算基准价（演示用） */
    public static final BigDecimal INSURANCE_BASE = new BigDecimal("400.00");

    private final BillingRecordRepository billingRepo;
    private final ConsumableTxnRepository txnRepo;
    private final ConsumableRepository consumableRepo;

    /** 下机后自动生成待核对账单 */
    @Transactional
    public BillingRecord createForSession(DialysisSession session) {
        if (billingRepo.findBySessionId(session.getId()).isPresent()) {
            return billingRepo.findBySessionId(session.getId()).get();
        }
        BigDecimal selfPay = BigDecimal.ZERO;
        List<ConsumableTxn> txns = txnRepo.findBySessionId(session.getId());
        for (ConsumableTxn txn : txns) {
            if (txn.getType() != TxnType.PRE_DRAW && txn.getType() != TxnType.USE) continue;
            Consumable c = consumableRepo.findById(txn.getConsumableId()).orElse(null);
            if (c != null && Boolean.TRUE.equals(c.getSelfPay()) && c.getPrice() != null) {
                selfPay = selfPay.add(c.getPrice().multiply(BigDecimal.valueOf(-txn.getQty())));
            }
        }
        BillingRecord record = new BillingRecord();
        record.setSessionId(session.getId());
        record.setPatientName(session.getPatient().getName());
        record.setInsuranceAmount(INSURANCE_BASE);
        record.setSelfPayAmount(selfPay);
        record.setExtraCharge(BigDecimal.ZERO);
        record.setStatus(BillingStatus.PENDING);
        return billingRepo.save(record);
    }

    @Transactional
    public BillingRecord confirm(Long id, String operator) {
        BillingRecord record = billingRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("账单不存在"));
        record.setStatus(BillingStatus.CONFIRMED);
        record.setConfirmedBy(operator);
        record.setConfirmedAt(LocalDateTime.now());
        return billingRepo.save(record);
    }
}

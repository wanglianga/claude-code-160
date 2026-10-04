package com.hdms.service;

import com.hdms.config.GlobalExceptionHandler.BizException;
import com.hdms.config.SecurityUtils;
import com.hdms.model.BillingRecord;
import com.hdms.model.ConsumableTxn;
import com.hdms.model.TreatmentRecord;
import com.hdms.model.enums.BillingStatus;
import com.hdms.model.enums.TxnType;
import com.hdms.repo.BillingRepo;
import com.hdms.repo.ConsumableTxnRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BillingService {
    /** 单次透析医保结算基准价（演示用） */
    private static final BigDecimal BASE_INSURANCE = new BigDecimal("400.00");

    private final BillingRepo billingRepo;
    private final ConsumableTxnRepo txnRepo;

    public BillingService(BillingRepo billingRepo, ConsumableTxnRepo txnRepo) {
        this.billingRepo = billingRepo;
        this.txnRepo = txnRepo;
    }

    /** 治疗完成后自动生成待核对账单：医保基准 + 自费耗材 */
    @Transactional
    public BillingRecord createForTreatment(TreatmentRecord t) {
        billingRepo.findByTreatmentId(t.getId()).ifPresent(b -> {
            throw new BizException("账单已存在");
        });
        List<ConsumableTxn> used = txnRepo.findByScheduleIdAndType(t.getSchedule().getId(), TxnType.USE);
        BigDecimal selfPay = BigDecimal.ZERO;
        StringBuilder detail = new StringBuilder();
        for (ConsumableTxn txn : used) {
            if (txn.getConsumable().isSelfPaid()) {
                BigDecimal amount = txn.getConsumable().getUnitPrice()
                        .multiply(BigDecimal.valueOf(txn.getQuantity()));
                selfPay = selfPay.add(amount);
                if (detail.length() > 0) detail.append("；");
                detail.append(txn.getConsumable().getName()).append("×").append(txn.getQuantity())
                        .append("=").append(amount);
            }
        }
        BillingRecord b = new BillingRecord();
        b.setTreatment(t);
        b.setInsuranceAmount(BASE_INSURANCE);
        b.setSelfPayAmount(selfPay);
        b.setSelfPayDetail(detail.toString());
        return billingRepo.save(b);
    }

    @Transactional(readOnly = true)
    public List<BillingRecord> list(BillingStatus status) {
        return status == null ? billingRepo.findTop200ByOrderByCreatedAtDesc()
                : billingRepo.findByStatusOrderByCreatedAtDesc(status);
    }

    /** 财务调整补收费用 */
    @Transactional
    public BillingRecord adjust(Long id, BigDecimal extraAmount, String extraNote) {
        BillingRecord b = billingRepo.findById(id).orElseThrow(() -> new BizException("账单不存在"));
        if (b.getStatus() == BillingStatus.CONFIRMED) throw new BizException("账单已核对，不可调整");
        b.setExtraAmount(extraAmount == null ? BigDecimal.ZERO : extraAmount);
        b.setExtraNote(extraNote);
        return billingRepo.save(b);
    }

    @Transactional
    public BillingRecord confirm(Long id) {
        BillingRecord b = billingRepo.findById(id).orElseThrow(() -> new BizException("账单不存在"));
        b.setStatus(BillingStatus.CONFIRMED);
        b.setConfirmedBy(SecurityUtils.currentUser());
        b.setConfirmedAt(LocalDateTime.now());
        return billingRepo.save(b);
    }
}

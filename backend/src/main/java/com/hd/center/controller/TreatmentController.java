package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.entity.*;
import com.hd.center.entity.Enums.*;
import com.hd.center.repository.*;
import com.hd.center.service.BillingService;
import com.hd.center.service.ExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 透析治疗过程：透前评估、透中记录、透后（下机）记录。
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class TreatmentController {

    /** 收缩压低于该值自动上报透析中低血压 */
    private static final int HYPOTENSION_THRESHOLD = 90;

    private final SessionRepository sessionRepo;
    private final MachineRepository machineRepo;
    private final PreAssessmentRepository preRepo;
    private final IntraRecordRepository intraRepo;
    private final PostRecordRepository postRepo;
    private final ExceptionService exceptionService;
    private final BillingService billingService;

    /** 透前评估（存在则更新） */
    @PostMapping("/{id}/pre")
    public PreAssessment savePre(@PathVariable Long id, @RequestBody PreAssessment body,
                                 @AuthenticationPrincipal CurrentUser current) {
        DialysisSession session = getSession(id);
        PreAssessment pre = preRepo.findBySessionId(id).orElseGet(PreAssessment::new);
        pre.setSessionId(id);
        pre.setWeight(body.getWeight());
        pre.setBpSys(body.getBpSys());
        pre.setBpDia(body.getBpDia());
        pre.setHeartRate(body.getHeartRate());
        pre.setAccessCondition(body.getAccessCondition());
        pre.setLastDiscomfort(body.getLastDiscomfort());
        pre.setAnticoagPlan(body.getAnticoagPlan());
        pre.setAdjustDehydration(body.getAdjustDehydration());
        pre.setDehydrationTarget(body.getDehydrationTarget());
        pre.setNote(body.getNote());
        pre.setNurseName(current.name());
        PreAssessment saved = preRepo.save(pre);

        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.ASSESSED);
            sessionRepo.save(session);
        }
        // 通路异常提醒
        if (body.getAccessCondition() != null && body.getAccessCondition() != AccessCondition.NORMAL) {
            exceptionService.raise(ExceptionType.OTHER, session.getPatient(), session, session.getMachine(),
                    "透前评估通路异常：" + body.getAccessCondition() + "，请医生关注", current.name());
        }
        return saved;
    }

    /** 新增透中记录；低血压/医嘱变更自动上报异常 */
    @PostMapping("/{id}/intra")
    public IntraRecord addIntra(@PathVariable Long id, @RequestBody IntraRecord body,
                                @AuthenticationPrincipal CurrentUser current) {
        DialysisSession session = getSession(id);
        if (session.getStatus() != SessionStatus.ON_DIALYSIS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅透析中的治疗单可录入透中记录");
        }
        body.setId(null);
        body.setSessionId(id);
        body.setNurseName(current.name());
        if (body.getRecordTime() == null) {
            body.setRecordTime(LocalDateTime.now());
        }
        IntraRecord saved = intraRepo.save(body);

        if (body.getBpSys() != null && body.getBpSys() < HYPOTENSION_THRESHOLD) {
            exceptionService.raise(ExceptionType.HYPOTENSION, session.getPatient(), session, session.getMachine(),
                    "透析中低血压：收缩压 " + body.getBpSys() + " mmHg，请立即处理", current.name());
        }
        if (body.getOrderChange() != null && !body.getOrderChange().isBlank()) {
            exceptionService.raise(ExceptionType.ORDER_CHANGE, session.getPatient(), session, session.getMachine(),
                    "医嘱变更：" + body.getOrderChange(), current.name());
        }
        return saved;
    }

    @GetMapping("/{id}/intra")
    public List<IntraRecord> listIntra(@PathVariable Long id) {
        return intraRepo.findBySessionIdOrderByRecordTime(id);
    }

    /** 下机记录：结束治疗、机器转消毒、自动生成账单 */
    @PostMapping("/{id}/post")
    public PostRecord savePost(@PathVariable Long id, @RequestBody PostRecord body,
                               @AuthenticationPrincipal CurrentUser current) {
        DialysisSession session = getSession(id);
        if (session.getStatus() != SessionStatus.ON_DIALYSIS && session.getStatus() != SessionStatus.OFF_DIALYSIS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅透析中的治疗单可记录下机");
        }
        PostRecord post = postRepo.findBySessionId(id).orElseGet(PostRecord::new);
        post.setSessionId(id);
        post.setOffWeight(body.getOffWeight());
        post.setHemostasisOk(body.getHemostasisOk());
        post.setHemostasisMin(body.getHemostasisMin());
        post.setDepartureMode(body.getDepartureMode());
        post.setNextScheduleNote(body.getNextScheduleNote());
        post.setNurseName(current.name());
        PostRecord saved = postRepo.save(post);

        session.setStatus(SessionStatus.OFF_DIALYSIS);
        session.setActualEndAt(LocalDateTime.now());
        sessionRepo.save(session);

        DialysisMachine machine = session.getMachine();
        machine.setStatus(MachineStatus.DISINFECTING);
        machineRepo.save(machine);

        billingService.createForSession(session);
        return saved;
    }

    private DialysisSession getSession(Long id) {
        return sessionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "治疗单不存在"));
    }
}

package com.hd.center.controller;

import com.hd.center.config.CurrentUser;
import com.hd.center.entity.ExceptionEvent;
import com.hd.center.entity.Enums.ExceptionStatus;
import com.hd.center.entity.Enums.ExceptionType;
import com.hd.center.repository.ExceptionEventRepository;
import com.hd.center.service.ExceptionService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/exceptions")
@RequiredArgsConstructor
public class ExceptionController {

    private final ExceptionEventRepository exceptionRepo;
    private final ExceptionService exceptionService;

    @GetMapping
    public List<ExceptionEvent> list(@RequestParam(required = false) ExceptionStatus status) {
        if (status != null) {
            return exceptionRepo.findByStatusOrderByCreatedAtDesc(status);
        }
        return exceptionRepo.findTop100ByOrderByCreatedAtDesc();
    }

    public record CreateRequest(@NotNull ExceptionType type, Long patientId, String patientName,
                                Long sessionId, Long machineId, String machineCode,
                                @NotNull String description) {}

    /** 手工上报异常 */
    @PostMapping
    public ExceptionEvent create(@RequestBody CreateRequest request,
                                 @AuthenticationPrincipal CurrentUser current) {
        ExceptionEvent event = new ExceptionEvent();
        event.setType(request.type());
        event.setPatientId(request.patientId());
        event.setPatientName(request.patientName());
        event.setSessionId(request.sessionId());
        event.setMachineId(request.machineId());
        event.setMachineCode(request.machineCode());
        event.setDescription(request.description());
        event.setCreatedBy(current.name());
        return exceptionRepo.save(event);
    }

    public record HandleRequest(@NotNull ExceptionStatus status, String handleNote) {}

    /** 处理/办结异常 */
    @PutMapping("/{id}/handle")
    public ExceptionEvent handle(@PathVariable Long id, @RequestBody HandleRequest request,
                                 @AuthenticationPrincipal CurrentUser current) {
        ExceptionEvent event = exceptionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "异常事件不存在"));
        event.setStatus(request.status());
        event.setHandlerName(current.name());
        event.setHandleNote(request.handleNote());
        if (request.status() == ExceptionStatus.RESOLVED) {
            event.setResolvedAt(LocalDateTime.now());
        }
        return exceptionRepo.save(event);
    }
}

package com.iexec.sms.iexecsms.execution.challenge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
public class ExecutionChallengeController {

    private ExecutionChallengeService executionChallengeService;

    public ExecutionChallengeController(ExecutionChallengeService executionChallengeService) {
        this.executionChallengeService = executionChallengeService;
    }

    /**
     * Called by the core, not the worker
     */
    @PostMapping("/executions/challenge/generate/{taskId}")
    public ResponseEntity<String> generateExecutionChallenge(@PathVariable String taskId) {
        Optional<ExecutionAttestor> oAttestor = executionChallengeService.getOrCreate(taskId);

        return oAttestor.map(executionAttestor -> ResponseEntity.ok(executionAttestor.getCredentials().getAddress()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

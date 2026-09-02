package com.example.guitarmes.body.process;

import java.time.LocalDateTime;

public record BodyProcessRunningResponse(
        Long historyId,
        Long bodyId,
        String bodySerialNo,
        Long processId,
        String processName,
        String workerName,
        LocalDateTime startTime) {
}

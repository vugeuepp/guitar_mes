package com.example.guitarmes.neck.process;

import java.time.LocalDateTime;

public record NeckProcessRunningResponse(
        Long historyId,
        Long neckId,
        String neckSerialNo,
        Long processId,
        String processName,
        String workerName,
        LocalDateTime startTime) {
}

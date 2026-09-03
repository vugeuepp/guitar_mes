package com.example.guitarmes.process;

import java.time.LocalDateTime;

public record ProcessRunningResponse(
        Long historyId,
        Long guitarId,
        String guitarSerialNo,
        Long processId,
        String processName,
        String workerName,
        LocalDateTime startTime) {
}

package com.example.guitarmes.process.partsinstallation;
public record PartsInstallationWorkItemResponse(Long itemId, String status, boolean checked,
        String completedAt, int completedCount, int totalCount, boolean readOnly) {}

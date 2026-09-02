package com.example.guitarmes.process;

import java.util.ArrayList;
import java.util.List;

public class BulkProcessEndRequest {
    private List<Long> historyIds = new ArrayList<>();
    public List<Long> getHistoryIds() { return historyIds; }
    public void setHistoryIds(List<Long> historyIds) { this.historyIds = historyIds; }
}

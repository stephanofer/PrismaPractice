package com.stephanofer.prismapractice.core.application.history;

import com.stephanofer.prismapractice.api.history.MatchHistorySummary;

public record HistoryRecordResult(boolean success, MatchHistorySummary summary, HistoryRecordFailureReason failureReason) {

    public static HistoryRecordResult success(MatchHistorySummary summary) {
        return new HistoryRecordResult(true, summary, null);
    }

    public static HistoryRecordResult failure(HistoryRecordFailureReason failureReason) {
        return new HistoryRecordResult(false, null, failureReason);
    }
}

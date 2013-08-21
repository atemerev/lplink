package com.olfa.b2b.events;

import com.olfa.b2b.domain.ExecutionReport;

public interface ExecutionReportListener {
    void onExecutionEvent(ExecutionReport report);
}

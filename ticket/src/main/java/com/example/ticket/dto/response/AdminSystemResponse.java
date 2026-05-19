package com.example.ticket.dto.response;

import java.util.List;

public record AdminSystemResponse(
        String healthStatus,
        String uptime,
        Double processCpuUsage,
        Double systemCpuUsage,
        Double heapUsedBytes,
        Double heapMaxBytes,
        Double liveThreads,
        Double httpRequestCount,
        Double httpRequestMeanSeconds,
        List<MetricResponse> metrics
) {
    public record MetricResponse(String name, String label, Double value, String unit) {
    }
}

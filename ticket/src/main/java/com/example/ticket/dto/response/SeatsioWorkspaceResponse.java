package com.example.ticket.dto.response;

public record SeatsioWorkspaceResponse(
        boolean configured,
        String region,
        String cdnUrl,
        String workspaceKey,
        String secretKey,
        String workspaceName,
        boolean test,
        boolean active,
        String message
) {
}

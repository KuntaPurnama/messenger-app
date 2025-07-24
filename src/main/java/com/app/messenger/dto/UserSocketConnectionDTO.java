package com.app.messenger.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSocketConnectionDTO {
    private String nodeId;
    private String phoneNumber;
}

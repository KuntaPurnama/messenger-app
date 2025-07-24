package com.app.messenger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class FileUploadDTO {
    private List<MessageAttachmentDTO> messageAttachmentDTOS;
    private Map<String, String> errorUploadedFile;
}

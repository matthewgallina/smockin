package com.smockin.admin.dto.response;

public class ExportResponseDTO {

    private final String contentType;
    private final String content;

    public ExportResponseDTO(final String contentType, final String content) {
        this.contentType = contentType;
        this.content = content;
    }

    public String getContentType() {
        return contentType;
    }
    public String getContent() {
        return content;
    }

}

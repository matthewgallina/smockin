package com.smockin.admin.dto.response;

/**
 * Created by mgallina.
 */
public class SimpleMessageResponseDTO<M> {

    private final M message;

    public SimpleMessageResponseDTO(final M message) {
        this.message = message;
    }

    public M getMessage() {
        return message;
    }

}

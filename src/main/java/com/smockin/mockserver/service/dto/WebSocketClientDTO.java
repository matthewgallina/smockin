package com.smockin.mockserver.service.dto;

import java.util.Date;

/**
 * Created by mgallina.
 */
public class WebSocketClientDTO {

    private String id;
    private Date dateJoined;

    public WebSocketClientDTO() {
    }

    public WebSocketClientDTO(final String id, final Date dateJoined) {
        this.id = id;
        this.dateJoined = dateJoined;
    }

    public String getId() {
        return id;
    }
    public void setId(String handshakeId) {
        this.id = id;
    }

    public Date getDateJoined() {
        return dateJoined;
    }
    public void setDateJoined(Date dateJoined) {
        this.dateJoined = dateJoined;
    }

}

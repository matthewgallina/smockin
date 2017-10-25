package com.smockin.admin.dto.response;

import com.smockin.admin.dto.JmsMockDTO;
import com.smockin.admin.persistence.enums.JmsMockTypeEnum;
import com.smockin.admin.persistence.enums.RecordStatusEnum;
import java.util.Date;


/**
 * Created by mgallina.
 */
public class JmsMockResponseDTO extends JmsMockDTO {

    private String extId;
    private Date dateCreated;

    public JmsMockResponseDTO(String extId, String name, RecordStatusEnum status, JmsMockTypeEnum mockType, Date dateCreated) {
        super(name,  status, mockType);
        this.extId = extId;
        this.dateCreated = dateCreated;
    }

    public String getExtId() {
        return extId;
    }
    public void setExtId(String extId) {
        this.extId = extId;
    }

    public Date getDateCreated() {
        return dateCreated;
    }
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

}

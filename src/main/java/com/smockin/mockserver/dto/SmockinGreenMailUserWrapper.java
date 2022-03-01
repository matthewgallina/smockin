package com.smockin.mockserver.dto;

import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.user.GreenMailUser;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SmockinGreenMailUserWrapper {

    private GreenMailUser user;
    private FolderListener listener;
    private boolean disabled;

}

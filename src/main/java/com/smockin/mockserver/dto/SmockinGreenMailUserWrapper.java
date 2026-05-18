package com.smockin.mockserver.dto;

import com.icegreen.greenmail.store.FolderListener;
import com.icegreen.greenmail.user.GreenMailUser;
import lombok.AllArgsConstructor;
import lombok.Data;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Data
@AllArgsConstructor
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"}, justification = "These objects are internal and intentionally shared")
public class SmockinGreenMailUserWrapper {

    private GreenMailUser user;
    private FolderListener listener;
    private boolean disabled;

}

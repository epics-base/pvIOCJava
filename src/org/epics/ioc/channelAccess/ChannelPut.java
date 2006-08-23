/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.pvAccess.*;

/**
 * @author mrk
 *
 */
public interface ChannelPut {
    void destroy();
    void put(ChannelFieldGroup fieldGroup,ChannelPutListener callback,boolean process, boolean wait);
    void cancelPut();
}
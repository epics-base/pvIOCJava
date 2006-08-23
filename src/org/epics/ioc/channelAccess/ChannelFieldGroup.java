/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;


/**
 * @author mrk
 *
 */
public interface ChannelFieldGroup {
    void destroy();
    void addChannelField(ChannelField channelField);
    void removeChannelField(ChannelField channelField);
}
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
public enum ChannelSetFieldResult {
    /**
     * The requested field is located via another channel.
     * Calls to getOtherChannel and getOtherField can be used to connect to the channel and field.
     */
    otherChannel,
    /**
     * t=The requested field is in this channel.
     * getField can be called to retrieve the ChannelData interface.
     */
    thisChannel,
    /**
     * The field could not be found.
     */
    notFound,
    /**
     * Failure. A common reason for failure is that the channel was destroyed.
     */
    failure
}
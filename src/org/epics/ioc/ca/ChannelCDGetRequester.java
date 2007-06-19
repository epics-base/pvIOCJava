/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.ca;

import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.Requester;

/**
 * The methods implemented by the requester for a ChannelCDPut.
 * @author mrk
 *
 */
public interface ChannelCDGetRequester extends Requester{
    /**
     * The get request has completed.
     * The data resides in the CD.
     * @param requestResult the process result.
     */
    void getDone(RequestResult requestResult);
}
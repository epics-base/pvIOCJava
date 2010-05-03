/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import org.epics.ca.client.ChannelRequester;

/**
 * Requester for a CreateFieldRequest.
 * @author mrk
 *
 */
public interface CreateFieldRequestRequester extends ChannelRequester
{
	/**
	 * Get the default field request string.
	 * @return The string.
	 */
	String getDefault();
    /**
     * The request has been created.
     * @param request The request.
     * @param isShared Should the request be a shared request?
     */
    void request(String request);
}
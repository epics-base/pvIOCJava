/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.util;

/**
 * The return value from a request.
 * @author mrk
 *
 */
public enum RequestResult {
    /**
     * The support is going away.
     */
    zombie,
    /**
     * The support is done.
     */
    success,
    /**
     * The request failed.
     */
    failure
}

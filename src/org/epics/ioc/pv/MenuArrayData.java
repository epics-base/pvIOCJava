/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pv;

/**
 * @author mrk
 *
 */
public class MenuArrayData {
    /**
     * The PVMenu[].
     * PVMenuArray.get sets this value.
     * PVMenuArray.put requires that the caller set the value. 
     */
    public PVMenu[] data;
    /**
     * The offset.
     * PVMenuArray.get sets this value.
     * PVMenuArray.put requires that the caller set the value. 
     */
    public int offset;
}

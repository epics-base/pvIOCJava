/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.database;

import org.epics.pvData.pv.PVStructure;

/**
 * @author mrk
 *
 */
public interface PVRecordStructure extends PVRecordField {
	/**
	 * Get the subfields.
	 * @return The array of subfields;
	 */
	PVRecordField[] getPVRecordFields();
	/**
	 * Get the PVStructure.
	 * @return The interface.
	 */
	PVStructure getPVStructure();
}

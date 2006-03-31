/**
 * 
 */
package org.epics.ioc.dbAccess;

import org.epics.ioc.pvAccess.*;

/**
 * get/put a long array.
 * The caller must be prepared to get/put the array in chunks.
 * The return argument is always the number of elements that were transfered.
 * It may be less than the number requested.
 * @author mrk
 *
 */
public interface DBLongArray extends DBArray, PVLongArray{}

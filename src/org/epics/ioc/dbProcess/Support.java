/**
 * Copyright - See the COPYRIGHT that is included with this disctibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import org.epics.ioc.dbAccess.*;

/**
 * interface that must be implemented by record support.
 * @author mrk
 *
 */
public interface Support {
    /**
     * Get the support name.
     * @return The support name.
     */
    String getName();
    /**
     * Get the support state.
     * @return The state.
     */
    SupportState getSupportState();
    /**
     * Get the field which this support supports.
     * @return The field.
     */
    DBData getDBData();
    /**
     * Add a listener for change of SupportState.
     * @param listener The listener.
     * @return (false,true) if the listener (was not, was) added to list.
     */
    boolean addSupportStateListener(SupportStateListener listener);
    /**
     * Remove a listener for change of SupportState.
     * @param listener The listener.
     * @return (false,true) if the listener (was not, was) removed from the list.
     */
    boolean removeSupportStateListener(SupportStateListener listener);
    /**
     * Generate an error message.
     * The name of the field (complete hierarchy) will be prepended to the error
     * message and recordSupport.errorMessage called.
     * @param message The error message.
     */
    void errorMessage(String message);
    /**
     * initialize.
     * perform initialization related to record instance but
     * do not connect to I/O or other records.
     */
    void initialize();
    /**
     * invoked when it is safe to link to I/O and/or other records.
     */
    void start();
    /**
     * disconnect all links to I/O and/or other records.
     */
    void stop();
    /**
     * Clean up any internal state created during initialize.
     */
    void uninitialize();
    /**
     * Perform support processing.
     * @param listener The listener to call when returning active.
     * @return The result of the process request.
     */
    ProcessReturn process(ProcessCompleteListener listener);
    /**
     * Continue processing. This is only called while support is active.
     */
    void processContinue();
    /**
     * Update state. This is only called while support is active.
     */
    void update();
}

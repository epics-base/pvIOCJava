/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.pvAccess;

/**
 * PVEnum provides access to a an array of String choices
 * and an index specifying the current choice.
 * @author mrk
 *
 */
public interface PVEnum extends PVData{
    /**
     * Get the index of the current selected choice.
     * @return index of current choice.
     */
    int getIndex();
    /**
     * Set the choice.
     * @param index for choice.
     */
    void setIndex(int index);
    /**
     * Get the choice values.
     * @return String[] specifying the choices.
     * @throws IllegalStateException if the field is not mutable.
     */
    String[] getChoices();
    /**
     * Set the choice values. 
     * @param choice a String[] specifying the choices.
     * @return (true,false) if the choices were modified.
     * A value of false normally means the choice strings were readonly.
     * @throws UnsupportedOperationException if the choices are not mutable.
     */
    boolean setChoices(String[] choice);
}

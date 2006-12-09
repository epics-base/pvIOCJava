/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.channelAccess;

import org.epics.ioc.util.ScanPriority;

/**
 * Interface for monitoring channel data.
 * @author mrk
 *
 */
public interface ChannelMonitor {
    /**
     * Look for any change to a channelField, i.e. a put to the field.
     * @param channelField The channelField to monitor.
     * @param causeMonitor If true this will cause monitor even if no other changes occur.
     */
    void lookForChange(ChannelField channelField, boolean causeMonitor);
    /**
     * Look for a change in the value of the field.
     * This can only be used for scalar numeric field.
     * @param channelField The channelField to monitor.
     * @param value The deadband value for changes.
     */
    void lookForAbsoluteChange(ChannelField channelField,double value);
    /**
     * Look for a percentage change in the value of the field.
     * @param channelField The channelField to monitor.
     * @param value The deadband value for changes.
     */
    void lookForPercentageChange(ChannelField channelField,double value);
    /**
     * Start monitoring for changes and send notification of changes but not the data.
     * @param channelMonitorNotifyRequestor The requestor.
     * @param threadName Name of notification thread.
     * @param scanPriority Priority of notification thread.
     * @return (false,true) if the monitor (has not, has) started.
     */
    boolean start(ChannelMonitorNotifyRequestor channelMonitorNotifyRequestor,
        String threadName, ScanPriority scanPriority);
    /**
     * Start monitoring for changes and send the data that has changed..
     * @param channelMonitorRequestor The requestor.
     * @param queueSize The size for a data queue.
     * @param threadName Name of notification thread.
     * @param scanPriority Priority of notification thread.
     * @return (false,true) if the monitor (has not, has) started.
     */
    boolean start(ChannelMonitorRequestor channelMonitorRequestor,
        int queueSize, String threadName, ScanPriority scanPriority);
    /**
     * Stop monitoring.
     */
    void stop();
}

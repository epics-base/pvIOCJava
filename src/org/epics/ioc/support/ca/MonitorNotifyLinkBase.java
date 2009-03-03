/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.ca;

import org.epics.ioc.ca.ChannelField;
import org.epics.ioc.ca.ChannelFieldGroup;
import org.epics.ioc.ca.ChannelMonitorNotify;
import org.epics.ioc.ca.ChannelMonitorNotifyFactory;
import org.epics.ioc.ca.ChannelMonitorNotifyRequester;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.RecordSupport;
import org.epics.ioc.support.SupportProcessRequester;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.RequestResult;
import org.epics.pvData.property.AlarmSeverity;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.*;


/**
 * Implementation for a channel access monitorNotify link.
 * @author mrk
 *
 */
public class MonitorNotifyLinkBase extends AbstractLink
implements RecordProcessRequester,ChannelMonitorNotifyRequester
{
    private ChannelMonitorNotify channelMonitorNotify = null;
    private boolean isActive = false;
    
    /**
     * The constructor.
     * @param supportName The supportName.
     * @param pvField The field being supported.
     */
    public MonitorNotifyLinkBase(String supportName,PVField pvField) {
        super(supportName,pvField);
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#initialize(org.epics.ioc.support.RecordSupport)
     */
    public void initialize(RecordSupport recordSupport) {
        super.initialize(recordSupport);
        if(!super.checkSupportState(SupportState.readyForStart,null)) return;
        if(!recordProcess.setRecordProcessRequester(this)) {
            message("notifySupport but record already has recordProcessor",MessageType.error);
            uninitialize();
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.Support#start()
     */
    public void start() {
        super.start();
        if(!super.checkSupportState(SupportState.ready,null)) return;
        super.connect();
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.SupportProcessRequester)
     */
    public void process(SupportProcessRequester supportProcessRequester) {
        supportProcessRequester.supportProcessDone(RequestResult.success);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
     */
    public void recordProcessComplete() {
        isActive = false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
     */
    public void recordProcessResult(RequestResult requestResult) {
        // nothing to do
        
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.ca.AbstractLinkSupport#connectionChange(boolean)
     */
    public void connectionChange(boolean isConnected) {
        if(isConnected) {
            String fieldName = channel.getFieldName();
            if(fieldName==null) fieldName = "value";
            ChannelField channelField = channel.createChannelField(fieldName);
            ChannelFieldGroup channelFieldGroup = channel.createFieldGroup(this);
            channelFieldGroup.addChannelField(channelField);
            channelMonitorNotify = ChannelMonitorNotifyFactory.create(channel, this);
            channelMonitorNotify.setFieldGroup(channelFieldGroup);
            channelMonitorNotify.start();
            
        } else {
            channelMonitorNotify.destroy();
            channelMonitorNotify = null;
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitorNotifyRequester#monitorEvent()
     */
    public void monitorEvent() {
        if(isActive) {
            pvRecord.lock();
            try {
                alarmSupport.setAlarm(
                    "channelMonitorNotify event but record already active", AlarmSeverity.minor);
            } finally {
                pvRecord.unlock();
            }
            return;
        }
        this.isActive = true;
        boolean isActive = recordProcess.process(this, false, null);
        if(!isActive) this.isActive = false;
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.ca.ChannelMonitorNotifyRequester#unlisten()
     */
    @Override
    public void unlisten() {
        stop();
    } 
}
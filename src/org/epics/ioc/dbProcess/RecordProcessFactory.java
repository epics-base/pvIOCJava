/**
 * Copyright - See the COPYRIGHT that is included with this distibution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess;

import java.util.*;

import org.epics.ioc.dbAccess.*;
import org.epics.ioc.pvAccess.*;
import org.epics.ioc.util.*;

/**
 * A factory for creating RecordProcess support for record instances.
 * @author mrk
 *
 */
public class RecordProcessFactory {
    
    /**
     * Create RecordProcess for a record instance.
     * @param dbRecord The record instance.
     * @return The interrace for the newly created RecordProcess.
     */
    static public RecordProcess createRecordProcess(DBRecord dbRecord) {
        return new ProcessInstance(dbRecord);
    }
    
    static private class ProcessInstance implements RecordProcess,SupportProcessRequestor
    {
        private boolean trace = false;
        private DBRecord dbRecord;
        private String recordProcessSupportName;
        private boolean disabled = false;
        private Support recordSupport = null;
        private Support scanSupport = null;
        
        private boolean active = false;
        private boolean leaveActive;
        private RecordProcessRequestor recordProcessRequestor = null;
        private boolean processIsRunning = false;
        private List<ProcessCallbackRequestor> processProcessCallbackListenerList =
            new ArrayList<ProcessCallbackRequestor>();
        private boolean processContinueIsRunning = false;
        private List<ProcessCallbackRequestor> continueProcessCallbackListenerList =
            new ArrayList<ProcessCallbackRequestor>();
        
        private boolean removeRecordProcessRequestorAfterActive = false;
        private boolean callStopAfterActive = false;
        private boolean callUninitializeAfterActive = false;
        private boolean processIsComplete = false;
        private RequestResult requestResult = null;
        
        private PVString pvStatus = null;
        private String startStatus = null;
        private String newStatus = null;
        
        private PVEnum pvSeverity = null;
        private int startSeverity = 0;
        private int newSeverity = 0;
        
        private PVTimeStamp pvTimeStamp = null;
        private TimeStamp timeStamp = new TimeStamp();
        
        private ProcessInstance(DBRecord record) {
            dbRecord = record;
            recordProcessSupportName = "recordProcess(" + dbRecord.getRecordName() + ")";
            recordSupport = dbRecord.getSupport();
            if(recordSupport==null) {
                throw new IllegalStateException(
                    dbRecord.getRecordName() + " has no support");
            }
            DBData[] dbDatas = dbRecord.getFieldDBDatas();
            DBData dbData;
            int index = dbRecord.getFieldDBDataIndex("status");
            if(index>=0) {
                dbData = dbDatas[index];
                if(dbData.getField().getType()==Type.pvString)
                    pvStatus = (PVString)dbData;
            }
            index = dbRecord.getFieldDBDataIndex("severity");
            if(index>=0) {
                dbData = dbDatas[index];
                if(dbData.getField().getType()==Type.pvEnum)
                    pvSeverity = (PVEnum)dbData;
            }
            index = dbRecord.getFieldDBDataIndex("timeStamp");
            if(index>=0) {
                pvTimeStamp = PVTimeStamp.create(dbDatas[index]);
            }
            index = dbRecord.getFieldDBDataIndex("scan");
            if(index>=0) {
                dbData = dbDatas[index];
                if(dbData.getField().getType()==Type.pvStructure) {
                    scanSupport = dbData.getSupport();
                }
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isDisabled()
         */
        public boolean isDisabled() {
            dbRecord.lock();
            try {
                return disabled;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setDisabled(boolean)
         */
        public boolean setDisabled(boolean value) {
            dbRecord.lock();
            try {
                boolean oldValue = disabled;
                disabled = value;
                return (oldValue==value) ? false : true;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#isActive()
         */
        public boolean isActive() {
            dbRecord.lock();
            try {
                return active;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecord()
         */
        public DBRecord getRecord() {
            return dbRecord;
        }     
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#initialize()
         */
        public void initialize() {
            dbRecord.lock();
            try {
                if(trace) traceMessage(" initialize");
                if(scanSupport!=null) scanSupport.initialize();
                recordSupport.initialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#start()
         */
        public void start() {
            dbRecord.lock();
            try {
                if(trace) traceMessage(" start");
                if(scanSupport!=null) scanSupport.start();
                recordSupport.start();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#stop()
         */
        public void stop() {
            dbRecord.lock();
            try {
                if(active) {
                    callStopAfterActive = true;
                    if(trace) traceMessage("stop delayed because active");
                    return;
                }
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                recordSupport.stop();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#uninitialize()
         */
        public void uninitialize() {
            dbRecord.lock();
            try {
                if(active) {
                    callUninitializeAfterActive = true;
                    if(trace) traceMessage("uninitialize delayed because active");
                    return;
                }
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                recordSupport.uninitialize();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setTrace(boolean)
         */
        public boolean setTrace(boolean value) {
            dbRecord.lock();
            try {
                boolean oldValue = trace;
                trace = value;
                if(value!=oldValue) return true;
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setRecordProcessRequestor(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public boolean setRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(this.recordProcessRequestor==null) {
                    this.recordProcessRequestor = recordProcessRequestor;
                    return true;
                }
                dbRecord.message(
                    recordProcessRequestor.getRequestorName()
                    + " called setRecordProcessRequestor but recordProcessRequestor is "
                    + this.recordProcessRequestor.getRequestorName(),
                    MessageType.error);
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#getRecordProcessRequestorName()
         */
        public String getRecordProcessRequestorName() {
            dbRecord.lock();
            try {
                if(recordProcessRequestor==null) return null;
                return recordProcessRequestor.getRequestorName();
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#releaseRecordProcessRequestor(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public boolean releaseRecordProcessRequestor(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(recordProcessRequestor==this.recordProcessRequestor) {
                    if(active) {
                        removeRecordProcessRequestorAfterActive = true;
                    } else {
                        this.recordProcessRequestor = null;
                    }
                    return true;
                }
                return false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#releaseRecordProcessRequestor()
         */
        public void releaseRecordProcessRequestor() {
            dbRecord.lock();
            try {
                dbRecord.message("recordProcessRequestor is being released", MessageType.error);
                if(active) {
                    removeRecordProcessRequestorAfterActive = true;
                } else {
                    recordProcessRequestor = null;
                }
            } finally {
                dbRecord.unlock();
            }
        }        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setActive(org.epics.ioc.dbProcess.RecordProcessRequestor, org.epics.ioc.util.TimeStamp)
         */
        public void setActive(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(trace) traceMessage("setActive" + recordProcessRequestor.getRequestorName());
                if(active) {
                    throw new IllegalStateException("record is already active");
                }
                if(this.recordProcessRequestor==null) {
                    throw new IllegalStateException("no registered requestor");
                }
                if(this.recordProcessRequestor != recordProcessRequestor) {
                    throw new IllegalStateException("not registered requestor");
                }
                active = true;
            } finally {
                dbRecord.unlock();
            }
            active = true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#process(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public void process(RecordProcessRequestor recordProcessRequestor, boolean leaveActive, TimeStamp timeStamp)
        {
            dbRecord.lock();
            try {
                if(this.recordProcessRequestor==null) {
                    throw new IllegalStateException("no registered requestor");
                }
                if(this.recordProcessRequestor != recordProcessRequestor) {
                    throw new IllegalStateException("not registered requestor");
                }
                active = true;
                this.leaveActive = leaveActive;
                requestResult = RequestResult.success;
                if(isDisabled()) {
                    if(trace) traceMessage("disabled");
                    requestResult = RequestResult.failure;
                    recordProcessRequestor.recordProcessResult(requestResult);
                } else {
                    SupportState supportState = recordSupport.getSupportState();
                    if(supportState!=SupportState.ready) {
                        if(trace) traceMessage("record support not ready");
                        requestResult = RequestResult.failure;
                        recordProcessRequestor.recordProcessResult(requestResult);
                    }
                }
                if(requestResult==RequestResult.success){
                    if(pvStatus!=null) {
                        startStatus = pvStatus.get();
                    } else {
                        startStatus = newStatus;
                    }
                    if(pvSeverity!=null) {
                        startSeverity = pvSeverity.getIndex();
                    } else {
                        startSeverity = newSeverity;
                    }
                    newStatus = null;
                    newSeverity = 0;
                    active = true;
                    processIsComplete = false;
                    if(timeStamp==null) {
                        if(trace) traceMessage(
                            "process with system timeStamp "
                            + recordProcessRequestor.getRequestorName()); 
                        TimeUtility.set(this.timeStamp,System.currentTimeMillis());
                    } else {
                        if(trace) traceMessage(
                            "process with callers timeStamp "
                            + recordProcessRequestor.getRequestorName()); 
                        this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
                        this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
                    }
                    processIsRunning = true;
                    recordSupport.process(this);
                    processIsRunning = false;
                    if(processIsComplete) completeProcessing();
                }
                
            } finally {
                dbRecord.unlock();
            }
            if(processIsComplete) {
                recordProcessRequestor.recordProcessComplete();
                return;
            }
            while(true) {
                ProcessCallbackRequestor processCallbackRequestor;
                /*
                 * No need to lock because the list can only be modified by
                 * code that was called directly or indirectly by process
                 * AND process will only be called if the record is not active.
                 */
                if(processProcessCallbackListenerList.size()<=0) break;
                processCallbackRequestor = processProcessCallbackListenerList.remove(0);
                processCallbackRequestor.processCallback();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcess#setInactive(org.epics.ioc.dbProcess.RecordProcessRequestor)
         */
        public void setInactive(RecordProcessRequestor recordProcessRequestor) {
            dbRecord.lock();
            try {
                if(trace) traceMessage("setInactive" + recordProcessRequestor.getRequestorName());
                if(!active) {
                    throw new IllegalStateException("record is not active");
                }
                if(this.recordProcessRequestor==null) {
                    throw new IllegalStateException("no registered requestor");
                }
                if(this.recordProcessRequestor != recordProcessRequestor) {
                    throw new IllegalStateException("not registered requestor");
                }
                active = false;
            } finally {
                dbRecord.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#processContinue()
         */
        public void processContinue(ProcessContinueRequestor processContinueRequestor) {
            boolean callbackListEmpty = true;
            dbRecord.lock();
            try {
                if(!active) {
                    throw new IllegalStateException("processContinue called but record is not active");
                }
                if(trace) {
                    traceMessage("processContinue ");
                }
                processContinueIsRunning = true;
                processContinueRequestor.processContinue();
                processContinueIsRunning = false;
                if(!continueProcessCallbackListenerList.isEmpty()) callbackListEmpty = false;
                if(processIsComplete) {
                    completeProcessing();
                }
            } finally {
                dbRecord.unlock();
            }
            if(processIsComplete) {
                if(recordProcessRequestor!=null) {
                    recordProcessRequestor.recordProcessComplete();
                }
                return;
            }
            if(!callbackListEmpty) while(true) {
                ProcessCallbackRequestor processCallbackRequestor;
                /*
                 * Must lock because callback can again call RecordProcess.processContinue
                 */
                dbRecord.lock();
                try {
                    if(continueProcessCallbackListenerList.size()<=0) break;
                    processCallbackRequestor = continueProcessCallbackListenerList.remove(0);
                } finally {
                    dbRecord.unlock();
                }
                processCallbackRequestor.processCallback();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#requestProcessCallback(org.epics.ioc.dbProcess.ProcessCallbackRequestor)
         */
        public void requestProcessCallback(ProcessCallbackRequestor processCallbackRequestor) {
            if(!active) {
                throw new IllegalStateException("requestProcessCallback called but record is not active");
            }
            if(trace) {
                traceMessage("requestProcessCallback " + processCallbackRequestor.getRequestorName());
            }
            if(processIsRunning) {
                processProcessCallbackListenerList.add(processCallbackRequestor);
                return;
            }
            if(processContinueIsRunning) {
                continueProcessCallbackListenerList.add(processCallbackRequestor);
                return;
            }
            throw new IllegalStateException("Support called requestProcessCallback illegally");
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setStatusSeverity(String status, AlarmSeverity alarmSeverity) {
            checkForIllegalRequest();
            if(trace) traceMessage("setStatusSeverity " + status + " " + alarmSeverity.toString());
            if(alarmSeverity.ordinal()>newSeverity) {  
                newStatus = status;
                newSeverity = alarmSeverity.ordinal();
                return true;
            }
            return false;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setStatus(java.lang.String)
         */
        public boolean setStatus(String status) {
            checkForIllegalRequest();
            if(trace) traceMessage("setStatus " + status);
            if(newSeverity>0) return false;
            newStatus = status;
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getAlarmSeverity()
         */
        public AlarmSeverity getAlarmSeverity() {
            checkForIllegalRequest();
            if(newSeverity<0) return AlarmSeverity.none;
            return AlarmSeverity.getSeverity(newSeverity);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getStatus()
         */
        public String getStatus() {
            checkForIllegalRequest();
            return newStatus;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#setTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void setTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            if(trace) traceMessage("setTimeStamp");
            this.timeStamp.secondsPastEpoch = timeStamp.secondsPastEpoch;
            this.timeStamp.nanoSeconds = timeStamp.nanoSeconds;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.RecordProcessSupport#getTimeStamp(org.epics.ioc.util.TimeStamp)
         */
        public void getTimeStamp(TimeStamp timeStamp) {
            checkForIllegalRequest();
            timeStamp.secondsPastEpoch = this.timeStamp.secondsPastEpoch;
            timeStamp.nanoSeconds = this.timeStamp.nanoSeconds;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getSupportProcessRequestorName()
         */
        public String getRequestorName() {
            return recordProcessSupportName;
        }
      
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#supportProcessDone(org.epics.ioc.util.RequestResult)
         */
        public void supportProcessDone(RequestResult requestResult) {
            if(!processIsRunning && !processContinueIsRunning) {
                throw new IllegalStateException("must be called from process or pocessContinue");
            }
            processIsComplete = true;
            this.requestResult = requestResult;
        }
        
        private void traceMessage(String message) {
            dbRecord.message(
                message + " thread " + Thread.currentThread().getName(),
                MessageType.info);
        }
         
        // called by process, preProcess, and processContinue with record locked.
        private void completeProcessing() {         
            if(removeRecordProcessRequestorAfterActive) {
                if(trace) traceMessage("remove recordProcessRequestor");
                recordProcessRequestor = null;
            }
            if(callStopAfterActive) {
                if(trace) traceMessage("stop");
                if(scanSupport!=null) scanSupport.stop();
                recordSupport.stop();
                callStopAfterActive = false;
            }
            if(callUninitializeAfterActive) {
                if(trace) traceMessage("uninitialize");
                if(scanSupport!=null) scanSupport.uninitialize();
                recordSupport.uninitialize();
                callUninitializeAfterActive = false;
            }
            if(!processProcessCallbackListenerList.isEmpty()
            || !continueProcessCallbackListenerList.isEmpty()){
                dbRecord.message(
                    "completing processing but ProcessCallbackListeners are still present",
                    MessageType.fatalError);
            }
            if(newSeverity!=startSeverity) {
                if(pvSeverity!=null) pvSeverity.setIndex(newSeverity);
            }
            if(newStatus!=startStatus) {
                if(pvStatus!=null) pvStatus.put(newStatus);
            }
            if(pvTimeStamp!=null) {
                pvTimeStamp.put(timeStamp);
            }
            dbRecord.endSynchronous();
            recordProcessRequestor.recordProcessResult(requestResult);
            if(!leaveActive) active = false;
            if(trace) traceMessage("process completion " + recordSupport.getRequestorName());
        }
        
        private void checkForIllegalRequest() {
            if(active && (processIsRunning||processContinueIsRunning)) return;
            if(!active) {
                dbRecord.message("illegal request because record is not active",
                     MessageType.info);
                throw new IllegalStateException("record is not active");
            } else {
                dbRecord.message("illegal request because neither process or processContinue is running",
                        MessageType.info);
                throw new IllegalStateException("neither process or processContinue is running");
            }
        }
    }
}

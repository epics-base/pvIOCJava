/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.support.basic;

import org.epics.pvdata.misc.Executor;
import org.epics.pvdata.misc.ExecutorFactory;
import org.epics.pvdata.misc.ExecutorNode;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVString;
import org.epics.pvioc.database.PVListener;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.install.AfterStart;
import org.epics.pvioc.install.AfterStartFactory;
import org.epics.pvioc.install.AfterStartNode;
import org.epics.pvioc.install.AfterStartRequester;
import org.epics.pvioc.support.AbstractSupport;
import org.epics.pvioc.support.Support;
import org.epics.pvioc.support.SupportProcessRequester;
import org.epics.pvioc.support.SupportState;
import org.epics.pvioc.util.EventScanner;
import org.epics.pvioc.util.PeriodicScanner;
import org.epics.pvioc.util.RequestResult;
import org.epics.pvioc.util.ScanField;
import org.epics.pvioc.util.ScanFieldFactory;
import org.epics.pvioc.util.ScanType;
import org.epics.pvioc.util.ScannerFactory;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanFactory {
    private static Executor executor 
        = ExecutorFactory.create("scanFieldModify", ThreadPriority.lower);
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    /**
     * Create support for the scan field.
     * @param pvRecordStructure The interface to the scan field.
     * @return The support or null if the scan field is improperly defined.
     */
    public static Support create(PVRecordStructure pvRecordStructure) {
        ScanField  scanField = ScanFieldFactory.create(pvRecordStructure.getPVRecord());
        if(scanField==null) return null;
        return new ScanImpl(pvRecordStructure,scanField);
    }
    
    private static class ScanImpl extends AbstractSupport implements PVListener, AfterStartRequester
    {
        private static final String supportName = "org.epics.pvioc.scan";
        private ScanField scanField;
        private PVRecord pvRecord = null;
        
        private boolean isActive = false;
        private boolean isStarted = false;
        
        private PVInt pvScanTypeIndex;
        
        private PVDouble pvRate;
        private PVString pvEventName;
        
        private ScanType scanType = null;
        private double scanRate;
        private ThreadPriority threadPriority = null;
        private String eventName = null;
        private ScanModify scanModify = null;
        private AfterStartNode afterStartNode = null;
        private AfterStart afterStart = null;
       
        
        private ScanImpl(PVRecordStructure pvScan,ScanField scanField) {
            super(supportName,pvScan);
            this.scanField = scanField;
            pvRecord = pvScan.getPVRecord();
            pvScanTypeIndex = scanField.getScanTypeIndexPV();
            pvRate = scanField.getRatePV();
            pvEventName = scanField.getEventNamePV();
            afterStartNode = AfterStartFactory.allocNode(this);
        }
        
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVListener#beginGroupPut(org.epics.pvdata.pv.PVRecord)
         */
        @Override
        public void beginGroupPut(PVRecord pvRecord) {}
        /* (non-Javadoc)
         * @see org.epics.pvioc.database.PVListener#dataPut(org.epics.pvioc.database.PVRecordStructure, org.epics.pvioc.database.PVRecordField)
         */
        @Override
        public void dataPut(PVRecordStructure requested, PVRecordField pvRecordField) {}

        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVListener#endGroupPut(org.epics.pvdata.pv.PVRecord)
         */
        @Override
        public void endGroupPut(PVRecord pvRecord) {}
        /* (non-Javadoc)
         * @see org.epics.pvioc.database.PVListener#dataPut(org.epics.pvioc.database.PVRecordField)
         */
        @Override
        public void dataPut(PVRecordField pvField) {
            if(!isStarted || !isActive) return;
            if(pvField==pvEventName) {
                if(scanType==ScanType.periodic) return;
            }
            callScanModify();
        }
        /* (non-Javadoc)
         * @see org.epics.pvdata.pv.PVListener#unlisten(org.epics.pvdata.pv.PVRecord)
         */
        @Override
        public void unlisten(PVRecord pvRecord) {}
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#getName()
         */
        @Override
        public String getRequesterName() {
            return supportName;
        }       
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#start()
         */
        @Override
        public void start(AfterStart afterStart) {
            setSupportState(SupportState.ready);
            isStarted = true;
            if(isActive) {
                addListeners();
                callScanModify();
                return;
            }
            this.afterStart = afterStart;
            afterStart.requestCallback(afterStartNode, true, ThreadPriority.lower);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#stop()
         */
        @Override
        public void stop() {
            removeListeners();
            isStarted = false;
            setSupportState(SupportState.readyForStart);
            if(scanModify!=null) {
                scanModify.modify();
                scanModify = null;
            }
            afterStart = null;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.AbstractSupport#process(org.epics.pvioc.process.RecordProcessRequester)
         */
        @Override
        public void process(SupportProcessRequester supportProcessRequester) {
            super.message("process is being called. Why?", MessageType.error);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
        }       
        /* (non-Javadoc)
         * @see org.epics.pvioc.install.AfterStartRequester#callback(org.epics.pvioc.install.AfterStartNode)
         */
        @Override
        public void callback(AfterStartNode node) {
            if(isStarted && !isActive) {
                isActive = true;
                addListeners();
                callScanModify();
            }
            afterStart.done(afterStartNode);
        }

        private void addListeners() {
            pvRecord.registerListener(this);
            pvRecord.findPVRecordField(pvScanTypeIndex).addListener(this);
            pvRecord.findPVRecordField(pvRate).addListener(this);
            pvRecord.findPVRecordField(pvEventName).addListener(this);
        }
        
        private void removeListeners() {
            pvRecord.unregisterListener(this);
        }
        
        private void callScanModify() {
            if(scanModify!=null) {
                scanModify.modify();
            } else {
                scanType = scanField.getScanType();
                scanRate = scanField.getRate();
                threadPriority = scanField.getPriority();
                eventName = scanField.getEventName();
                if(scanType==ScanType.event || scanType==ScanType.periodic) {
                    scanModify = new ScanModify();
                    scanModify.modify();
                }
            }
        }
         
        private class ScanModify implements Runnable {
            private boolean isPeriodic = false;
            private boolean isEvent = false;
            private ExecutorNode executorNode = null;
            
            private ScanModify() {
                executorNode = executor.createNode(this);
            }
            
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                    stopScanner();
                    startScanner();
            }
            
            public void modify() {
                executor.execute(executorNode);
            }
            
            
            private void startScanner() {
                if(!isActive || !isStarted) return;
                boolean result = true;
                switch (scanType) {
                case passive: break;
                case event:
                    result = eventScanner.addRecord(pvRecord);
                    if(result) isEvent = true;
                    break;
                case periodic:
                    result = periodicScanner.addRecord(pvRecord);
                    if(result) isPeriodic = true;
                    break;
                }
                update(!result);
            }
            
            private void stopScanner() {
                boolean result = true;
                if(isEvent) {
                    result = eventScanner.removeRecord(pvRecord, eventName, threadPriority);
                    isEvent = false;
                } else if(isPeriodic) {
                    result = periodicScanner.removeRecord(pvRecord, scanRate, threadPriority);
                    isPeriodic = false;
                }
                if(!result && pvScanTypeIndex!=null) {
                    pvScanTypeIndex.put(0);
                }
                update(!result);
            }
            
            private void update(boolean setPassive) {
                pvRecord.lock();
                try {
                    if(setPassive) {
                        pvScanTypeIndex.put(0);
                    }
                    scanType = scanField.getScanType();
                    scanRate = scanField.getRate();
                    threadPriority = scanField.getPriority();
                    eventName = scanField.getEventName();
                } finally {
                    pvRecord.unlock();
                }
            }
        }
    }
}

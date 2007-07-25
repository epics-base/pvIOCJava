/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.db.*;
import org.epics.ioc.process.*;
import org.epics.ioc.pv.*;
import org.epics.ioc.util.*;

/**
 * Support for scan field.
 * @author mrk
 *
 */
public class ScanFactory {
    private static Convert convert = ConvertFactory.getConvert();
    private static PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
    private static EventScanner eventScanner = ScannerFactory.getEventScanner();
    private static final String supportName = "scan";
    /**
     * Create support for the scan field.
     * @param dbStructure The interface to the scan field.
     * @return The support or null if the scan field is improperly defined.
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        ScanField scanField = ScanFieldFactory.create(pvStructure.getPVRecord());
        if(scanField==null) return null;
        String supportName = pvStructure.getSupportName();
        if(!supportName.equals(supportName)) {
            pvStructure.message(
                    "ScanFactory create supportName is " + supportName
                    + " but expected scan",
                    MessageType.fatalError);
                return null;
        }
        try {
            return new ScanImpl(dbStructure);
        } catch (IllegalStateException e) {
            pvStructure.message(
                "ScanFactory create failure " + e.getMessage(),
                MessageType.fatalError);
            return null;
        }  
    }
    
    private static class ScanImpl extends AbstractSupport
    implements ScanSupport
    {
        private DBStructure dbScan;
        private DBRecord dbRecord = null;
        private ScanField scanField;
        private PVProcessSelf pvProcessSelf;
        private boolean isActive = false;
        
        private ScanImpl(DBStructure dbScan) {
            super(supportName,dbScan);
            this.dbScan = dbScan;
            dbRecord = dbScan.getDBRecord();

        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#getName()
         */
        public String getRequesterName() {
            return supportName;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize(org.epics.ioc.process.SupportCreation)
         */
        public void initialize() {
            PVRecord pvRecord = dbRecord.getPVRecord();
            String recordName = pvRecord.getRecordName();
            PVStructure pvScan = dbScan.getPVStructure();
            Structure scanStructure = (Structure)pvScan.getField();
            DBField[] scanDBFields = dbScan.getFieldDBFields();
            PVField[] scanPVFields = pvScan.getFieldPVFields();
            
            String fullFieldName = null;
            PVField pvField = null;
            DBField dbField = null;
            int index; 
            int choiceIndex;
            Menu menu;
            PVMenu pvMenu;
            PVMenu newMenu;
            
            fullFieldName = pvScan.getFullFieldName() + ".scan";
            index = scanStructure.getFieldIndex("scan");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvMenu) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type menu");
            }
            pvMenu = (PVMenu)pvField;
            if(!ScanFieldFactory.isScanMenu(pvMenu)) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not a menuScan ");
            }
            dbField = scanDBFields[index];
            choiceIndex = pvMenu.getIndex();
            menu = (Menu)pvField.getField();
            newMenu = new DBScan(this,dbField,menu);
            newMenu.setIndex(choiceIndex);
            dbField.replacePVField(newMenu);
            
            fullFieldName = pvScan.getFullFieldName() + ".rate";
            index = scanStructure.getFieldIndex("rate");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvDouble) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type double");
            }
            dbField = scanDBFields[index];
            PVDouble oldRate = (PVDouble)pvField;
            double rate = oldRate.get();
            PVDouble newRate = new DBRate(this,dbField,pvField.getField());
            newRate.put(rate);
            dbField.replacePVField(newRate);
            
            fullFieldName = pvScan.getFullFieldName() + ".eventName";
            index = scanStructure.getFieldIndex("eventName");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvString) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type string");
            }
            dbField = scanDBFields[index];
            PVString oldEventName = (PVString)pvField;
            String eventName = oldEventName.get();
            PVString newEventName = new DBEventName(this,dbField,pvField.getField());
            newEventName.put(eventName);
            dbField.replacePVField(newEventName);
            
            fullFieldName = pvScan.getFullFieldName() + ".priority";
            index = scanStructure.getFieldIndex("priority");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvMenu) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type menu");
            }
            pvMenu = (PVMenu)pvField;
            if(!ScanFieldFactory.isPriorityMenu(pvMenu)) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not a menuPriority ");
            }
            dbField = scanDBFields[index];
            choiceIndex = pvMenu.getIndex();
            menu = (Menu)pvField.getField();
            newMenu = new DBPriority(this,dbField,menu);
            newMenu.setIndex(choiceIndex);
            dbField.replacePVField(newMenu);
            
            fullFieldName = pvScan.getFullFieldName() + ".processSelf";
            index = scanStructure.getFieldIndex("processSelf");
            if(index<0) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " not in record");
            }
            pvField = scanPVFields[index];
            if(pvField.getField().getType()!=Type.pvBoolean) {
                throw new IllegalStateException(recordName + "." + fullFieldName + " is not type boolean");
            }
            dbField = scanDBFields[index];
            PVBoolean oldProcessSelf = (PVBoolean)pvField;
            boolean processSelf = oldProcessSelf.get();
            pvProcessSelf = new PVProcessSelf(this,dbField,pvField.getField());
            pvProcessSelf.put(processSelf);
            dbField.replacePVField(pvProcessSelf);
            scanField = ScanFieldFactory.create(pvRecord);
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            stop();
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#start()
         */
        public void start() {
            setSupportState(SupportState.ready);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#stop()
         */
        public void stop() {
            if(isActive) {
                stopScanner();
                isActive = false;
            }
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AbstractSupport#allSupportStarted()
         */
        @Override
        public void allSupportStarted() {
            isActive = true;
            startScanner();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            dbRecord.getPVRecord().message("process is being called. Why?", MessageType.error);
            supportProcessRequester.supportProcessDone(RequestResult.failure);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#canProcessSelf()
         */
        public boolean canProcessSelf() {
            return scanField.getProcessSelf();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfRequest(org.epics.ioc.process.RecordProcessRequester)
         */
        public boolean processSelfRequest(RecordProcessRequester recordProcessRequester) {
            return pvProcessSelf.processSelf(recordProcessRequester);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfSetActive()
         */
        public void processSelfSetActive(RecordProcessRequester recordProcessRequester) {
            pvProcessSelf.processSelfSetActive(recordProcessRequester);
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfProcess(boolean)
         */
        public void processSelfProcess(RecordProcessRequester recordProcessRequester, boolean leaveActive) {
            pvProcessSelf.startScan(recordProcessRequester,leaveActive);
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.ScanSupport#processSelfSetInactive()
         */
        public void processSelfSetInactive(RecordProcessRequester recordProcessRequester) {
            pvProcessSelf.setInactive(recordProcessRequester);
        }
        
        private void startScanner() {
            if(!isActive) return;
            ScanType scanType = scanField.getScanType();
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.addRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.schedule(dbRecord);
                break;
            }
        }
        
        private void stopScanner() {
            if(!isActive) return;
            ScanType scanType = scanField.getScanType();
            switch (scanType) {
            case passive: break;
            case event:
                eventScanner.removeRecord(dbRecord);
                break;
            case periodic:
                periodicScanner.unschedule(dbRecord);
                break;
            }
        }
    }
    
    private static class DBPriority extends BasePVMenu {
        private DBMenu dbMenu;
        private ScanImpl scanImpl;
        
        private DBPriority(ScanImpl scanImpl,DBField dbField,Menu menu) {
            super(dbField.getParent().getPVField(),menu);
            dbMenu = (DBMenu)dbField;
            this.scanImpl = scanImpl;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.db.BaseDBEnum#setIndex(int)
         */
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            if(oldIndex!=index) scanImpl.stopScanner();
            dbMenu.setIndex(index);
            if(oldIndex!=index) scanImpl.startScanner();
        }
    }
    
    private static class DBScan extends BasePVMenu {
        private ScanImpl scanImpl;
        
        private DBScan(ScanImpl scanImpl,DBField dbField,Menu menu) {
            super(dbField.getParent().getPVField(),menu);
            this.scanImpl = scanImpl;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.db.BaseDBEnum#setIndex(int)
         */
        public void setIndex(int index) {
            int oldIndex = super.getIndex();
            if(oldIndex!=index) scanImpl.stopScanner();
            super.setIndex(index);
            if(oldIndex!=index) scanImpl.startScanner();
        }
    }
    
    private static class DBRate extends AbstractPVField implements PVDouble{
        private double value;
        private ScanImpl scanImpl;
        
        private DBRate(ScanImpl scanImpl,DBField dbField,Field field) {
            super(dbField.getParent().getPVField(),field);
            value = 0;
            this.scanImpl = scanImpl;
            String defaultValue = field.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = Float.valueOf(defaultValue);
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#get()
         */
        public double get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVDouble#put(double)
         */
        public void put(double value) {
            if(super.isMutable()) {
                double oldValue = this.value;
                if(oldValue!=value) scanImpl.stopScanner();
                this.value = value;
                if(oldValue!=value) scanImpl.startScanner();
                return ;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

    }
    
    private static class DBEventName extends AbstractPVField implements PVString{
        private String value;
        private ScanImpl scanImpl;
        
        private DBEventName(ScanImpl scanImpl,DBField dbField,Field field) {
            super(dbField.getParent().getPVField(),field);
            value = null;
            this.scanImpl = scanImpl;
            String defaultValue = field.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                value = defaultValue;
            }
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#get()
         */
        public String get() {
            return value;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVString#put(java.lang.String)
         */
        public void put(String value) {
            if(super.isMutable()) {
                String oldValue = this.value;
                boolean modified = false;
                if(oldValue!=null) {
                    if(!oldValue.equals(value)) modified = true;
                } else if(value!=null) {
                    modified = true;
                }
                if(modified) scanImpl.stopScanner();
                this.value = value;
                if(modified) scanImpl.startScanner();
                return;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }
    }
    
    private static class PVProcessSelf extends AbstractPVField
    implements PVBoolean,RecordProcessRequester
    {
        private boolean isProcessSelf = false;
        private RecordProcess recordProcess = null;
        private boolean isActive = false;
        private RecordProcessRequester recordProcessRequester = null;
        
        private PVProcessSelf(ScanImpl scanImpl,DBField dbField,Field field) {
            super(dbField.getParent().getPVField(),field);
            recordProcess = dbField.getDBRecord().getRecordProcess();
            isProcessSelf = false;
            String defaultValue = field.getFieldAttribute().getDefault();
            if(defaultValue!=null && defaultValue.length()>0) {
                isProcessSelf = Boolean.valueOf(defaultValue);
            }
            dbField.replacePVField(this);
        }
        
        private boolean processSelf(RecordProcessRequester recordProcessRequester) {
            if(!isProcessSelf) return false;
            if(isActive) return false;
            isActive = true;
            this.recordProcessRequester = recordProcessRequester;
            return true;
        }
        
        private void processSelfSetActive(RecordProcessRequester recordProcessRequester) {
            if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                throw new IllegalStateException("not the recordProcessRequester");
            }
            recordProcess.setActive(this);
        }
        
        private void startScan(RecordProcessRequester recordProcessRequester,boolean leaveActive) {
            if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                throw new IllegalStateException("not the recordProcessRequester");
            }
            recordProcess.process(this, leaveActive, null);
        }
        
        private void setInactive(RecordProcessRequester recordProcessRequester) {
            if(recordProcessRequester==null || recordProcessRequester!=this.recordProcessRequester) {
                throw new IllegalStateException("not the recordProcessRequester");
            }
            recordProcess.setInactive(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#get()
         */
        public boolean get() {
            return isProcessSelf;
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.PVBoolean#put(boolean)
         */
        public void put(boolean value) {
            if(super.isMutable()) {
                if(isProcessSelf) {
                    recordProcess.releaseRecordProcessRequester(this);
                    isProcessSelf = false;
                }
                if(value) {
                    isProcessSelf = recordProcess.setRecordProcessRequester(this);
                }
                return;
            }
            throw new IllegalStateException("PVField.isMutable is false");
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessComplete()
         */
        public void recordProcessComplete() {
            isActive = false;
            RecordProcessRequester recordProcessRequester = this.recordProcessRequester;
            this.recordProcessRequester = null;
            recordProcessRequester.recordProcessComplete();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
         */
        public void recordProcessResult(RequestResult requestResult) {
            recordProcessRequester.recordProcessResult(requestResult);
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return toString(0);
        }       
        /* (non-Javadoc)
         * @see org.epics.ioc.pv.AbstractPVField#toString(int)
         */
        public String toString(int indentLevel) {
            return convert.getString(this, indentLevel)
            + super.toString(indentLevel);
        }

    }
}

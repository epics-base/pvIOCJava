/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.dbProcess.example;

import org.epics.ioc.dbProcess.*;
import org.epics.ioc.dbAccess.*;
import org.epics.ioc.util.*;

/**
 * Counter record acts as a counter from a min to max value with a specified increment.
 * @author mrk
 *
 */
public class CounterRecordFactory {
    /**
     * Create support.
     * @param dbStructure The structure for which the support will be created.
     * @return The newly created support.
     */
    public static Support create(DBStructure dbStructure) {
        return new CounterRecord(dbStructure);
    }
    
    private static class CounterRecord extends AbstractSupport implements SupportProcessRequestor
    {
        private static String supportName = "counterRecord";
        private DBRecord dbRecord = null;
        private RecordProcess recordProcess = null;
        private DBDouble dbMin = null;
        private DBDouble dbMax = null;
        private DBDouble dbInc = null;
        private DBDouble dbValue = null;
        private DBArray dbLinkArray = null;
        
        private LinkSupport linkArraySupport = null;
        
        private CounterRecord(DBStructure dbStructure) {
            super(supportName,dbStructure);
            dbRecord = dbStructure.getRecord();
            DBData[] dbData = dbStructure.getFieldDBDatas();
            int index;
            index = dbStructure.getFieldDBDataIndex("min");
            if(index<0) throw new IllegalStateException("field min does not exist");
            dbMin = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("max");
            if(index<0) throw new IllegalStateException("field max does not exist");
            dbMax = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("inc");
            if(index<0) throw new IllegalStateException("field inc does not exist");
            dbInc = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("value");
            if(index<0) throw new IllegalStateException("field value does not exist");
            dbValue = (DBDouble)dbData[index];
            index = dbStructure.getFieldDBDataIndex("linkArray");
            if(index<0) throw new IllegalStateException("field linkArray does not exist");
            dbLinkArray = (DBArray)dbData[index];
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbProcess.SupportProcessRequestor#getProcessRequestorName()
         */
        public String getSupportProcessRequestorName() {
            return dbRecord.getRecordName();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,"initialize")) return;
            recordProcess = dbRecord.getRecordProcess();
            linkArraySupport = (LinkSupport)dbLinkArray.getSupport();
            if(linkArraySupport!=null) {
                linkArraySupport.setField(dbValue);
                linkArraySupport.initialize();
                setSupportState(linkArraySupport.getSupportState());
            } else {
                setSupportState(SupportState.readyForStart);
            }
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            if(linkArraySupport!=null) {
                linkArraySupport.start();
                setSupportState(linkArraySupport.getSupportState());
            } else {
                setSupportState(SupportState.ready);
            }
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#stop()
         */
        public void stop() {
            if(!super.checkSupportState(SupportState.ready,supportName)) return;
            if(linkArraySupport!=null) linkArraySupport.stop();
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()==SupportState.ready) {
                stop();
            }
            if(super.getSupportState()!=SupportState.readyForStart) return;
            if(linkArraySupport!=null) linkArraySupport.uninitialize();
            linkArraySupport = null;
            setSupportState(SupportState.readyForInitialize);
        }   
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#process(org.epics.ioc.dbLinkArray.ProcessRequestListener)
         */
        public RequestResult process(SupportProcessRequestor supportProcessRequestor) {
            if(!super.checkSupportState(SupportState.ready,"process")) return RequestResult.failure;
            double min = dbMin.get();
            double max = dbMax.get();
            double inc = dbInc.get();
            double value = dbValue.get();
            value += inc;
            if(value>max) value = min;
            dbValue.put(value);
            if(linkArraySupport!=null) {
                return linkArraySupport.process(this);
            }
            return RequestResult.success;
        }      
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.Support#processContinue()
         */
        public void processContinue() {
            dbRecord.message("why was processContinue called", IOCMessageType.error);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.ProcessRequestListener#processComplete(org.epics.ioc.dbLinkArray.Support, org.epics.ioc.dbLinkArray.ProcessResult)
         */
        public void processComplete(RequestResult requestResult) {
            recordProcess.getRecordProcessSupport().processComplete(requestResult);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.dbLinkArray.ProcessRequestListener#requestResult(org.epics.ioc.util.AlarmSeverity, java.lang.String, org.epics.ioc.util.TimeStamp)
         */
        public void processResult(AlarmSeverity alarmSeverity, String status, TimeStamp timeStamp) {
            // nothing to do 
        }
    }
}

/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.alarm;

import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.support.AbstractSupport;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.Support;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;

/**
 * Support for alarm field.
 * @author mrk
 *
 */
public class AlarmFactory {
   
    private static final String alarmSupportName = "alarm";
    /**
     * Create support for an alarm field.
     * @param dbStructure The interface to the alarm field.
     * @return The support or null if the alarm field is improperly defined.
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        Structure structure = (Structure)pvStructure.getField();
        String supportName = pvStructure.getSupportName();
        if(!supportName.equals(alarmSupportName)) {
            pvStructure.message("no support for " + supportName, MessageType.fatalError);
            return null;
        }
        if(!structure.getStructureName().equals("alarm")) {
            pvStructure.message("not an alarm structure" + supportName, MessageType.fatalError);
        }
        return new AlarmImpl(dbStructure);
    }
    
    public static AlarmSupport findAlarmSupport(DBField startDBField) {
        if(startDBField==null) return null;
        DBField parentDBField;
        if(startDBField instanceof DBStructure) {
            parentDBField = startDBField;
        } else {
            parentDBField = startDBField.getParent();
        }
        while(parentDBField!=null) {
            if(parentDBField instanceof DBStructure) {
                DBStructure parentDBStructure = (DBStructure)parentDBField;
                DBField[] dbFields = parentDBStructure.getDBFields();
                for(DBField dbField : dbFields) {
                    Field field = dbField.getPVField().getField();
                    Type type = field.getType();
                    if(type==Type.pvStructure) {
                        Structure structure = (Structure)field;
                        if(structure.getStructureName().equals("alarm")) {
                            Support support = dbField.getSupport();
                            if(support instanceof AlarmImpl) {
                                return (AlarmSupport)support;
                            }
                        }
                    }
                }
            }
            parentDBField = parentDBField.getParent();
        }
        return null;
    }
    
    private static class AlarmImpl extends AbstractSupport implements AlarmSupport
    {
        private DBStructure dbAlarm;
        private PVStructure pvAlarm;
        private RecordProcess recordProcess = null;
        
        private DBField dbSeverity = null;
        private PVInt pvSeverity = null;
        private DBField dbMessage = null;
        private PVString pvMessage = null;
        
        private boolean gotAlarm;
        private boolean active = false;
        private int beginIndex = 0;
        private int currentIndex = 0;
        private String beginMessage = null;
        private String currentMessage = null;
        
        private AlarmImpl parentAlarmSupport = null;
        
        private AlarmImpl(DBStructure dbAlarm) {
            super(alarmSupportName,dbAlarm);
            this.dbAlarm = dbAlarm;
            DBField[] dbFields = dbAlarm.getDBFields();
            pvAlarm = dbAlarm.getPVStructure();
            PVField[] pvFields = pvAlarm.getPVFields();
            Structure structure = pvAlarm.getStructure();
            int index = structure.getFieldIndex("message");
            if(index<0) {
                pvAlarm.message("field message does not exist", MessageType.error);
                return;
            }
            pvMessage = (PVString)pvFields[index];
            dbMessage = dbFields[index];
            index = structure.getFieldIndex("severity");
            if(index<0) {
                pvAlarm.message("field severity does not exist", MessageType.error);
                return;
            }
            PVField pvField = pvFields[index];
            if(pvField.getField().getType()!=Type.pvStructure) {
                pvAlarm.message("field severity is not a structure", MessageType.error);
                return;
            }
            DBStructure dbStructure = (DBStructure)dbFields[index];
            dbFields = dbStructure.getDBFields();
            PVStructure pvStructure = (PVStructure)pvFields[index];
            pvFields = pvStructure.getPVFields();
            structure = pvStructure.getStructure();
            index = structure.getFieldIndex("index");
            if(index<0) {
                pvStructure.message("field index does not exist", MessageType.error);
            }
            dbSeverity = dbFields[index];
            pvSeverity = (PVInt)pvFields[index];
        }  
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#initialize(org.epics.ioc.process.SupportCreation)
         */
        public void initialize() {
            // look for parent starting with parent of parent
            AlarmSupport parent = AlarmFactory.findAlarmSupport(dbAlarm.getParent().getParent());
            if(parent!=null) {
                parentAlarmSupport = (AlarmImpl)parent;
            }
            recordProcess = dbAlarm.getDBRecord().getRecordProcess();
            super.setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.AbstractSupport#uninitialize()
         */
        public void uninitialize() {
            parentAlarmSupport = null;
            super.setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#beginProcess()
         */
        public void beginProcess() {
            if(active) return;
            gotAlarm = false;
            active = true;
            beginIndex = pvSeverity.get();
            beginMessage = pvMessage.get();
            currentMessage = beginMessage;
            currentIndex = 0;
            currentMessage = null;
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#endProcess()
         */
        public void endProcess() {
            active = false;
            boolean messageChange = false;
            if(beginMessage==null) {
                if(currentMessage!=null) messageChange = true;
            } else {
                if(currentMessage==null) {
                    messageChange = true;
                } else if(!beginMessage.equals(currentMessage)) {
                    messageChange = true;
                }
            }
            if(currentIndex!=beginIndex || messageChange) {
                pvSeverity.put(currentIndex);                
                pvMessage.put(currentMessage);
                dbSeverity.postPut();
                dbMessage.postPut();
            }
        }
        
        /* (non-Javadoc)
         * @see org.epics.ioc.support.AlarmSupport#setStatusSeverity(java.lang.String, org.epics.ioc.util.AlarmSeverity)
         */
        public boolean setAlarm(String message, AlarmSeverity severity) {
            int newIndex = severity.ordinal();
            if(!active) {
                if(recordProcess.isActive()) {
                    beginProcess();
                } else { // record is not being processed
                    if(newIndex>0) { // raise alarm
                        pvSeverity.put(newIndex);                
                        pvMessage.put(message);
                        dbSeverity.postPut();
                        dbMessage.postPut();
                        return true;
                    } else { // no alarm just return false
                        return false;
                    }
                }
            }
            if(!gotAlarm || newIndex>currentIndex) {
                currentIndex = newIndex;
                currentMessage = message;
                gotAlarm = true;
                if(parentAlarmSupport!=null) {
                    parentAlarmSupport.setAlarm(message, severity);
                }
                return true;
            }
            return false;
        }
    }
}
/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support;

import org.epics.ioc.create.Enumerated;
import org.epics.ioc.db.DBField;
import org.epics.ioc.db.DBStructure;
import org.epics.ioc.db.DBStructureArray;
import org.epics.ioc.process.SupportProcessRequester;
import org.epics.ioc.process.SupportState;
import org.epics.ioc.pv.Field;
import org.epics.ioc.pv.PVBoolean;
import org.epics.ioc.pv.PVDouble;
import org.epics.ioc.pv.PVField;
import org.epics.ioc.pv.PVInt;
import org.epics.ioc.pv.PVString;
import org.epics.ioc.pv.PVStructure;
import org.epics.ioc.pv.PVStructureArray;
import org.epics.ioc.pv.Structure;
import org.epics.ioc.pv.Type;
import org.epics.ioc.util.AlarmSeverity;
import org.epics.ioc.util.MessageType;
import org.epics.ioc.util.RequestResult;

/**
 * Support for a doubleAlarm link.
 * @author mrk
 *
 */
public class DoubleAlarmFactory {
    /**
     * Create support for a doubleAlarm structure.
     * @param dbStructure The structure.
     * @return An interface to the support or null if the supportName was not "doubleAlarm".
     */
    public static Support create(DBStructure dbStructure) {
        PVStructure pvStructure = dbStructure.getPVStructure();
        String supportName = pvStructure.getSupportName();
        if(supportName==null || !supportName.equals(supportName)) {
            pvStructure.message("does not have support " + supportName,MessageType.error);
            return null;
        }
        return new DoubleAlarmImpl(dbStructure);
    }
    
    private static String supportName = "doubleAlarm";
    
    private static class DoubleAlarmImpl extends AbstractSupport
    {
        private DBStructure dbStructure;
        private PVStructure pvStructure;
        
        private boolean noop;
        private AlarmSupport alarmSupport;
        
        private PVStructureArray intervalPVArray;
        private PVInt pvOutOfRange;
        private PVBoolean pvActive;
        private PVDouble pvHystersis;
        
        private DBStructureArray dbAlarmIntervalArray = null;
        private PVDouble[] pvAlarmIntervalValue = null;
        private PVInt[] pvAlarmIntervalSeverity = null;
        private PVString[] pvAlarmIntervalMessage = null;
        
        private PVDouble pvValue;
        private double lastAlarmIntervalValue;
        private int lastAlarmSeverityIndex;
       
        private DoubleAlarmImpl(DBStructure dbStructure) {
            super(supportName,dbStructure);
            this.dbStructure = dbStructure;
            pvStructure = dbStructure.getPVStructure();
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#initialize()
         */
        public void initialize() {
            if(!super.checkSupportState(SupportState.readyForInitialize,supportName)) return;
            SupportState supportState = SupportState.readyForStart;
            noop = false;
            DBField dbParent = dbStructure.getParent();
            PVField pvParent = dbParent.getPVField();
            PVField pvField = pvParent.findProperty("value");
            if(pvField==null) {
                pvStructure.message("value field not found", MessageType.error);
                return;
            }
            DBField valueDBField = dbStructure.getDBRecord().findDBField(pvField);
            pvField = valueDBField.getPVField();
            if(pvField.getField().getType()!=Type.pvDouble) {
                super.message("field type is not double", MessageType.error);
                return;
            }
            pvValue = (PVDouble)pvField;
            alarmSupport = AlarmFactory.findAlarmSupport(dbStructure);
            if(alarmSupport==null) {
                super.message("no alarmSupport", MessageType.error);
                noop = true;
                setSupportState(supportState);
                return;
            }
            DBField[] dbFields = dbStructure.getDBFields();
            if(dbFields.length==0) {
                noop = true;
                setSupportState(supportState);
                return;
            }
            Structure structure = dbStructure.getPVStructure().getStructure();
            pvActive = pvStructure.getBooleanField("active");
            if(pvActive==null) return;
            
            intervalPVArray = (PVStructureArray)pvStructure.getArrayField(
                "interval", Type.pvStructure);
            if(intervalPVArray==null) return;
            int index = structure.getFieldIndex("interval");
            dbAlarmIntervalArray = (DBStructureArray)dbFields[index];
            index = structure.getFieldIndex("outOfRange");
            if(index<0) {
                super.message("outOfRange does not exist", MessageType.error);
                return;
            }
            Enumerated enumerated = AlarmSeverity.getAlarmSeverity(dbFields[index]);
            if(enumerated==null) return;
            pvOutOfRange = enumerated.getIndexField();
            if(pvOutOfRange==null) return;
            pvHystersis = pvStructure.getDoubleField("hystersis");
            if(pvHystersis==null) return;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#start()
         */
        public void start() {
            if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
            SupportState supportState = SupportState.ready;
            if(noop) {
                setSupportState(supportState);
                return;
            }
            int size = intervalPVArray.getLength();
            if(size<=0) {
                super.message("invalid interval", MessageType.error);
                return;
            }
            DBStructure[] dbFields = dbAlarmIntervalArray.getElementDBStructures();
            pvAlarmIntervalValue = new PVDouble[size];
            pvAlarmIntervalSeverity = new PVInt[size];
            pvAlarmIntervalMessage = new PVString[size];
            
            for(int i=0; i<size; i++) {
                DBStructure dbStructure = dbFields[i];
                PVStructure pvStructure = dbStructure.getPVStructure();
                Structure structure = pvStructure.getStructure();
                PVField[] pvFields = pvStructure.getPVFields();
                Field[] fields = structure.getFields();
                int index = structure.getFieldIndex("value");
                if(index<0) {
                    super.message("invalid interval no value field", MessageType.error);
                    return;
                }
                Field field = fields[index];
                if(field.getType()!=Type.pvDouble) {
                    super.message("invalid interval value field is not double", MessageType.error);
                    return;
                }
                pvAlarmIntervalValue[i] = (PVDouble)pvFields[index];
                index = structure.getFieldIndex("severity");
                if(index<0) {
                    super.message("invalid interval no severity field", MessageType.error);
                    return;
                }
                Enumerated enumerated = AlarmSeverity.getAlarmSeverity(
                        dbStructure.getDBFields()[index]);
                if(enumerated==null) {
                    super.message("invalid interval severity field is not alarmSeverity", MessageType.error);
                    return;
                }
                pvAlarmIntervalSeverity[i] = enumerated.getIndexField();
                index = structure.getFieldIndex("message");
                if(index<0) {
                    super.message("invalid interval no message field", MessageType.error);
                    return;
                }
                field = fields[index];
                if(field.getType()!=Type.pvString) {
                    super.message("invalid interval message field is not string", MessageType.error);
                    return;
                }
                pvAlarmIntervalMessage[i] = (PVString)pvFields[index];
            }
            lastAlarmSeverityIndex = 0;
            setSupportState(supportState);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#stop()
         */
        public void stop() {
            if(super.getSupportState()!=SupportState.ready) return;
            pvAlarmIntervalValue = null;
            pvAlarmIntervalSeverity = null;
            setSupportState(SupportState.readyForStart);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#uninitialize()
         */
        public void uninitialize() {
            if(super.getSupportState()!=SupportState.ready) return;
            pvActive = null;
            pvOutOfRange = null;
            intervalPVArray = null;
            pvHystersis = null;
            setSupportState(SupportState.readyForInitialize);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.process.Support#process(org.epics.ioc.process.RecordProcessRequester)
         */
        public void process(SupportProcessRequester supportProcessRequester) {
            if(!noop && pvActive.get()) checkAlarm();
            supportProcessRequester.supportProcessDone(RequestResult.success);
        }                

        private void checkAlarm() {
            boolean active = pvActive.get();
            if(!active) return;
            double  val = pvValue.get();
            int len = pvAlarmIntervalValue.length;
            double intervalValue = 0.0;
            for(int i=0; i<len; i++) {
                intervalValue = pvAlarmIntervalValue[i].get();
                if(val<=intervalValue) {
                    int sevIndex = pvAlarmIntervalSeverity[i].get();
                    raiseAlarm(intervalValue,val,sevIndex,pvAlarmIntervalMessage[i].get());
                    return;
                }
            }
            int outOfRange = pvOutOfRange.get();
            // intervalValue is pvAlarmIntervalValue[len-1].get();
            raiseAlarm(intervalValue,val,outOfRange,"out of range");
        }
        
        private void raiseAlarm(double intervalValue,double val,int severityIndex,String message) {
            AlarmSeverity alarmSeverity = AlarmSeverity.getSeverity(severityIndex);
            if(severityIndex<lastAlarmSeverityIndex) {
                double diff = lastAlarmIntervalValue - val;
                if(diff<0.0) diff = -diff;
                if(diff<pvHystersis.get()) {
                    alarmSeverity = AlarmSeverity.getSeverity(lastAlarmSeverityIndex);
                    intervalValue = lastAlarmIntervalValue;
                }
            }
            if(alarmSeverity==AlarmSeverity.none) {
                lastAlarmSeverityIndex = severityIndex;
                return;
            }
            alarmSupport.setAlarm(message, alarmSeverity);
            lastAlarmIntervalValue = intervalValue;
            lastAlarmSeverityIndex = severityIndex;
        }
    }
}

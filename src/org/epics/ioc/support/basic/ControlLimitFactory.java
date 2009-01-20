/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.basic;

import org.epics.pvData.pv.*;
import org.epics.pvData.misc.*;
import org.epics.pvData.factory.*;
import org.epics.pvData.property.*;
import org.epics.ioc.support.*;
import org.epics.ioc.support.alarm.*;

import org.epics.ioc.util.*;

/**
 * Factory for an enumerated structure.
 * @author mrk
 *
 */
public class ControlLimitFactory {
    private static PVProperty pvProperty = PVPropertyFactory.getPVProperty();
    private static PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    
    /**
     * replace the pvField implementation with an implementation that enforces control limits.
     * @param pvField
     */
    public static void replacePVField(PVField pvField) {
        if(pvField.getField().getType()!=Type.scalar) {
            pvField.message("replacePVField field is not scalar", MessageType.error);
            return;
        }
        Scalar scalar = (Scalar)pvField.getField();
        if(!scalar.getScalarType().isNumeric()) {
            pvField.message("replacePVField field is not a numeric scalar", MessageType.error);
            return;
        }
        PVField pvControl = pvProperty.findProperty(pvField, "control");
        if(pvControl==null) {
            pvField.message("replacePVField control is not a property", MessageType.error);
            return;
        }
        PVField pvLow = pvProperty.findProperty(pvControl,"limit.low");
        PVField pvHigh = pvProperty.findProperty(pvControl,"limit.high");
        if(pvLow==null || pvHigh==null) {
            pvField.message("replacePVField invalid control structure", MessageType.error);
            return;
        }
        if(pvLow.getField().getType()!=Type.scalar) {
            pvLow.message("replacePVField is not a scalar", MessageType.error);
            return;
        }
        if(pvHigh.getField().getType()!=Type.scalar) {
            pvLow.message("replacePVField is not a scalar", MessageType.error);
            return;
        }
        new ControlLimitImpl((PVScalar)pvField,(PVScalar)pvLow,(PVScalar)pvHigh);
    }

    private static class ControlLimitImpl {
        
        private static Convert convert = ConvertFactory.getConvert();
        private PVField valuePVField = null;
        private AlarmSupport alarmSupport = null;
        /** Constructor.
         * @param valuePVField The PVField interface for the value field.
         * @param lowPVField The PVField interface for the low limit.
         * @param highPVField The PVField interface for the high limit.
         */
        public ControlLimitImpl(PVScalar valuePVField, PVScalar lowPVField, PVScalar highPVField) {
            this.valuePVField = valuePVField;
            PVStructure parentPVField = valuePVField.getParent();
            PVScalar newPVField = null;
            Scalar valueField = valuePVField.getScalar();
            ScalarType type = valueField.getScalarType();
            switch(type) {
            case pvByte:
                newPVField = new ByteValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvShort:
                newPVField = new ShortValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvInt:
                newPVField = new IntValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvLong:
                newPVField = new LongValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvFloat:
                newPVField = new FloatValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            case pvDouble:
                newPVField = new DoubleValue(parentPVField,valueField,lowPVField,highPVField);
                break;
            default:
                throw new IllegalStateException("valuePVfield does not have a supported type");
            }
            valuePVField.replacePVField(newPVField);
            double oldValue = convert.toDouble(valuePVField);
            convert.fromDouble(newPVField, oldValue);
        }
        
        private void raiseAlarm(boolean isHigh) {
            if(alarmSupport==null) {
                RecordSupport recordSupport = SupportDatabaseFactory.get(masterPVDatabase).getRecordSupport(valuePVField.getPVRecord());
                alarmSupport = AlarmSupportFactory.findAlarmSupport(valuePVField,recordSupport);
                if(alarmSupport==null) {
                    valuePVField.message("ControlLimit: no alarmSupport", MessageType.warning);
                }
            }
            String message = null;
            if(isHigh) {
                message = "ControlLimit: attempt to exceed high limit";
            } else {
                message = "ControlLimit: attempt to exceed low limit";
            }
            if(alarmSupport!=null) {
                alarmSupport.setAlarm(message, AlarmSeverity.minor);
            } else {
                valuePVField.message(message, MessageType.warning);
            }
        }
        
        private class ByteValue extends AbstractPVScalar implements PVByte {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private byte value;
            private ByteValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(parent,field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVByte#get()
             */
            public byte get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVByte#put(byte)
             */
            public void put(byte value) {
                byte lowValue = convert.toByte(lowPVField);
                byte highValue = convert.toByte(highPVField);
                if(lowValue>highValue) return;
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
                this.value = value;
                super.postPut();
            }
        }
        
        private class ShortValue extends AbstractPVScalar implements PVShort {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private short value;
            private ShortValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(parent,field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVShort#get()
             */
            public short get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVShort#put(short)
             */
            public void put(short value) {
                short lowValue = convert.toShort(lowPVField);
                short highValue = convert.toShort(highPVField);
                if(lowValue>highValue) return;
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
                this.value = value;
                super.postPut();
            }
        }
        private class IntValue extends AbstractPVScalar implements PVInt {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private int value;
            private IntValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(parent,field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVInt#get()
             */
            public int get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVInt#put(int)
             */
            public void put(int value) {
                int lowValue = convert.toInt(lowPVField);
                int highValue = convert.toInt(highPVField);
                if(lowValue>highValue) return;
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
                this.value = value;
                super.postPut();
            }
        }
        private class LongValue extends AbstractPVScalar implements PVLong {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private long value;
            private LongValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(parent,field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVLong#get()
             */
            public long get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVLong#put(long)
             */
            public void put(long value) {
                long lowValue = convert.toLong(lowPVField);
                long highValue = convert.toLong(highPVField);
                if(lowValue>highValue) return;
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
                this.value = value;
                super.postPut();
            }
        }
        private class FloatValue extends AbstractPVScalar implements PVFloat {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private float value;
            private FloatValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(parent,field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVFloat#get()
             */
            public float get() {
                return value;
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.pv.PVFloat#put(float)
             */
            public void put(float value) {
                float lowValue = convert.toFloat(lowPVField);
                float highValue = convert.toFloat(highPVField);
                if(lowValue>highValue) return;
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
                this.value = value;
                super.postPut();
            }
        }
        private class DoubleValue extends AbstractPVScalar implements PVDouble {
            private PVScalar lowPVField;
            private PVScalar highPVField;
            private double value;
            private DoubleValue(PVStructure parent,Scalar field,PVScalar lowPVField,PVScalar highPVField) {
                super(parent,field);
                this.lowPVField = lowPVField;
                this.highPVField = highPVField;
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
                double lowValue = convert.toDouble(lowPVField);
                double highValue = convert.toDouble(highPVField);
                if(lowValue>highValue) return;
                if(value<lowValue) {
                    value = lowValue;
                    raiseAlarm(false);
                } else if(value>highValue) {
                    value = highValue;
                    raiseAlarm(true);
                }
                this.value = value;
                super.postPut();
            }
        }
    }
}
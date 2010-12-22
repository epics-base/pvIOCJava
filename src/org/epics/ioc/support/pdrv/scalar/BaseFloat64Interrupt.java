/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.support.pdrv.scalar;

import org.epics.ioc.database.PVRecordStructure;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.pdrv.Trace;
import org.epics.ioc.pdrv.interfaces.Float64;
import org.epics.ioc.pdrv.interfaces.Float64InterruptListener;
import org.epics.ioc.pdrv.interfaces.Interface;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDouble;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVScalar;
import org.epics.pvData.pv.PVString;
import org.epics.pvData.pv.ScalarType;
import org.epics.pvData.pv.Type;

/**
 * Implement Float64Interrupt.
 * @author mrk
 *
 */
public class BaseFloat64Interrupt extends AbstractPortDriverInterruptLink
implements Float64InterruptListener
{
    /**
     * The constructor.
     * @param pvRecordStructure The structure being supported.
     * @param supportName The name of the support.
     */
    public BaseFloat64Interrupt(PVRecordStructure pvRecordStructure,String supportName) {
        super(supportName,pvRecordStructure);
    }

    private Float64 float64 = null;
    private double value = 0.0;
    private PVDouble pvLowLimit = null;
    private PVDouble pvHighLimit = null;
    private PVString pvUnits = null;
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#initialize()
     */
    @Override
    public void initialize() {
        super.initialize();
        if(!super.checkSupportState(SupportState.readyForStart,supportName)) return;
        PVField pvDisplay = pvProperty.findProperty(valuePVField,"display");
        if(pvDisplay!=null) {
            PVField pvTemp = pvProperty.findProperty(pvDisplay,"units");
            if(pvTemp!=null && pvTemp.getField().getType()==Type.scalar) {
                PVScalar pvScalar = (PVScalar)pvTemp;
                if(pvScalar.getScalar().getScalarType()==ScalarType.pvString) {
                    pvUnits = (PVString)pvTemp;
                }
            }
            pvTemp = pvProperty.findProperty(pvDisplay,"limit");
            if(pvTemp!=null) {
                PVField pvTemp1 = pvProperty.findProperty(pvTemp,"low");
                if(pvTemp1!=null && pvTemp1.getField().getType()==Type.scalar) {
                    PVScalar pvScalar = (PVScalar)pvTemp1;
                    if(pvScalar.getScalar().getScalarType()==ScalarType.pvDouble) {
                        pvLowLimit = (PVDouble)pvTemp1;
                    }
                }
                pvTemp1 = pvProperty.findProperty(pvTemp,"high");
                if(pvTemp1!=null && pvTemp1.getField().getType()==Type.scalar) {
                    PVScalar pvScalar = (PVScalar)pvTemp1;
                    if(pvScalar.getScalar().getScalarType()==ScalarType.pvDouble) {
                        pvHighLimit = (PVDouble)pvTemp1;
                    }
                }
            }
        }
        if(valuePVField.getField().getType()==Type.scalar) return;
        super.uninitialize();
        pvStructure.message("value field is not a scalar type", MessageType.fatalError);
        return;
    }      
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#start()
     */
    @Override
    public void start(AfterStart afterStart) {
        super.start(afterStart);
        if(!super.checkSupportState(SupportState.ready,supportName)) return;
        Interface iface = device.findInterface(user, "float64");
        if(iface==null) {
            pvStructure.message("interface float64 not supported", MessageType.fatalError);
            super.stop();
            return;
        }
        float64 = (Float64)iface;
        if(pvUnits!=null && (pvUnits.get()==null || pvUnits.get().length()==0)) {
            String units = float64.getUnits(user);
            pvUnits.put(units);
        }
        if(pvLowLimit!=null && pvHighLimit!=null) {
            if(pvLowLimit.get()==pvHighLimit.get()) {
                double[] limits = float64.getDisplayLimits(user);
                if(limits!=null) {
                    pvLowLimit.put(limits[0]);
                    pvHighLimit.put(limits[1]);
                }
            }
        }
        float64.addInterruptUser(user, this);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.pdrv.AbstractPortDriverInterruptLink#stop()
     */
    @Override
    public void stop() {
        super.stop();
        float64.removeInterruptUser(user, this);
        float64 = null;
    }  
    /* (non-Javadoc)
     * @see org.epics.ioc.pdrv.interfaces.Float64InterruptListener#interrupt(double)
     */
    @Override
    public void interrupt(double value) {
    	this.value = value;
    	if(isProcessor) {
    		recordProcess.queueProcessRequest(processToken);
        } else {
            pvRecord.lock();
            try {
                putData(value);
                if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
                    deviceTrace.print(Trace.SUPPORT,
                        "pv %s interrupt and record not processed value %e",fullName,value);
                }
            } finally {
                pvRecord.unlock();
            }
        }
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#becomeProcessor()
     */
    @Override
    public void becomeProcessor() {
    	putData(value);
    	recordProcess.process(processToken,false);
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#canNotProcess(java.lang.String)
     */
    @Override
    public void canNotProcess(String reason) {
    	pvRecord.lock();
    	try {
    		putData(value);
    	} finally {
    		pvRecord.unlock();
    	}
    }
    /* (non-Javadoc)
     * @see org.epics.ioc.support.RecordProcessRequester#lostRightToProcess()
     */
    @Override
    public void lostRightToProcess() {
    	isProcessor = false;
    	processToken = null;
    }	
    
    private void putData(double value) {
        convert.fromDouble((PVScalar)valuePVField, value);
        if((deviceTrace.getMask()&Trace.SUPPORT)!=0) {
            deviceTrace.print(Trace.SUPPORT,
                "pv %s value = %e",fullName,value);
        }
    }
}


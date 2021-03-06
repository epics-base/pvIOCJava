/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.pvCopy;

import junit.framework.TestCase;

import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.copy.PVCopy;
import org.epics.pvdata.copy.PVCopyFactory;
import org.epics.pvdata.factory.ConvertFactory;
import org.epics.pvdata.factory.PVDataFactory;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.misc.BitSetUtil;
import org.epics.pvdata.misc.BitSetUtilFactory;
import org.epics.pvdata.pv.Convert;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVDataCreate;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUnion;
import org.epics.pvdata.pv.PVUnionArray;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Type;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordField;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.database.PVReplaceFactory;
import org.epics.pvioc.example.RegularUnionArrayExampleRecord;
import org.epics.pvioc.example.RegularUnionExampleRecord;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PVCopyTest extends TestCase {
    private final static PVDatabase master = PVDatabaseFactory.getMaster();
    private final static PVDataCreate pvDataCreate = PVDataFactory.getPVDataCreate();
    private final static BitSetUtil bitSetUtil = BitSetUtilFactory.getCompressBitSet();
    private final static Requester requester = new RequesterImpl();
    private static final CreateRequest createRequest = CreateRequest.create();
    private final static Convert convert = ConvertFactory.getConvert();
   
    
    private static class RequesterImpl implements Requester {
		@Override
		public String getRequesterName() {
			return "pvCopyTest";
		}
		@Override
		public void message(String message, MessageType messageType) {
		    System.out.printf("message %s messageType %s%n",message,messageType.name());
			
		}
    }
    
    public static void testPVCopy() {
        // get database for testing
        Requester iocRequester = new RequesterForTesting("accessTest");
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/structures.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/powerSupply.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/powerSupplyArray.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/test/org/epics/pvioc/pvCopy/scalarArray.xml", iocRequester);
        PVReplaceFactory.replace(master);
        PVUnionTest();
        PVUnionArrayTest();
        badRequestTest();
        timeStampTest();
        exampleTest();
        longTest();
    }
    
    public static void PVUnionTest() {
        String recordName = "unionExample";
        RegularUnionExampleRecord.start(recordName);
        PVRecord pvRecord = master.findRecord(recordName);
        assertTrue(pvRecord!=null);
//System.out.println("pvRecord");
//System.out.println(pvRecord);
        PVStructure pvRequest = createRequest.createRequest("");
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
        PVCopy pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy!=null);
//System.out.println("pvCopy");
//System.out.println(pvCopy.dump());
        PVStructure pvCopyStructure = pvCopy.createPVStructure();
//System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        BitSet bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        PVUnion pvRecordField = pvRecord.getPVRecordStructure().getPVStructure().getSubField(PVUnion.class,"value");
        PVUnion pvCopyField = pvCopyStructure.getSubField(PVUnion.class,"value");
System.out.println("pvCopyField");
System.out.println(pvCopyField);
System.out.println(pvCopyField);
System.out.println(pvCopyField.getField());
        pvCopyField.select(PVString.class,"string").put("test");
        bitSet.set(pvCopyField.getFieldOffset());
System.out.println("pvCopyStructure");
System.out.println(pvCopyStructure);
System.out.println(bitSet);
        pvCopy.updateMaster(pvCopyStructure, bitSet);
System.out.println("pvRecord");
System.out.println(pvRecord);     
        pvRecordField.select(PVString.class,"string").put("test other way");
        bitSet.clear();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
System.out.println("pvRecord");
System.out.println(pvRecord);  
System.out.println("pvCopyStructure");
System.out.println(pvCopyStructure);
System.out.println(bitSet);
    }
    
    public static void PVUnionArrayTest() {
        String recordName = "unionArrayExample";
        RegularUnionArrayExampleRecord.start(recordName);
        PVRecord pvRecord = master.findRecord(recordName);
        assertTrue(pvRecord!=null);
System.out.println("pvRecord");
System.out.println(pvRecord);
        PVStructure pvRequest = createRequest.createRequest("");
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
        PVCopy pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy!=null);
System.out.println("pvCopy");
System.out.println(pvCopy.dump());
        PVStructure pvCopyStructure = pvCopy.createPVStructure();
System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        BitSet bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        PVUnionArray pvRecordField = pvRecord.getPVRecordStructure().getPVStructure().getSubField(PVUnionArray.class,"value");
        PVUnionArray pvCopyField = pvCopyStructure.getSubField(PVUnionArray.class,"value");
System.out.println("pvCopyField");
System.out.println(pvCopyField);
System.out.println(pvCopyField);
System.out.println(pvCopyField.getField());
        PVUnion pvUnion = pvDataCreate.createPVUnion(pvRecordField.getUnionArray().getUnion());
        pvUnion.select(PVString.class,"string").put("test");
        PVUnion[] pvUnions = new PVUnion[1];
        pvUnions[0] = pvUnion;
        pvCopyField.put(0, 1, pvUnions, 0);
        bitSet.set(pvCopyField.getFieldOffset());
System.out.println("pvCopyStructure");
System.out.println(pvCopyStructure);
System.out.println(bitSet);
        pvCopy.updateMaster(pvCopyStructure, bitSet);
System.out.println("pvRecord");
System.out.println(pvRecord); 
        pvUnion = pvDataCreate.createPVUnion(pvRecordField.getUnionArray().getUnion());
        pvUnions[0] = pvUnion;
        pvUnion.select(PVString.class,"string").put("test other way");
        pvRecordField.put(0, 1, pvUnions, 0);
        bitSet.clear();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
System.out.println("pvRecord");
System.out.println(pvRecord);  
System.out.println("pvCopyStructure");
System.out.println(pvCopyStructure);
System.out.println(bitSet);
    }
    public static void badRequestTest() {
        System.out.printf("%n%n****badRequestTest****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = null;
        String request = "";
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        pvRecord = master.findRecord("powerSupply");
        assertTrue(pvRecord!=null);
        request = "xxx";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
//System.out.println("pvRequest " + pvRequest);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy==null);
        request = "xxx{yyy[zzz=nnn]}";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
//System.out.println("pvRequest " + pvRequest);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy==null);

    }
    
    public static void timeStampTest() {
        System.out.printf("%n%n****timeStamp****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = null;
        String request = "";
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        PVStructure pvCopyStructure = null;
        BitSet bitSet = null;
        pvRecord = master.findRecord("powerSupply");
        assertTrue(pvRecord!=null);
        PVStructure pvTop = pvRecord.getPVRecordStructure().getPVStructure();
        PVStructure pvTimeStamp = pvTop.getStructureField("timeStamp");
        PVLong pvRecordSeconds = (PVLong)pvTop.getSubField("timeStamp.secondsPastEpoch");
        PVInt pvRecordNanoseconds = (PVInt)pvTop.getSubField("timeStamp.nanoseconds");
        PVInt pvRecordUserTag = (PVInt)pvTop.getSubField("timeStamp.userTag");
//System.out.printf("pvTop%n%s%n ",pvTop.toString());
//request = "";
//pvRequest = CreateRequestFactory.createRequest(request,requester);
//pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
//pvCopyStructure = pvCopy.createPVStructure();
//System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
//System.out.printf("pvRecordSeconds%n%s%n",pvRecordSeconds);

        
        request = "timeStamp[causeMonitor=true]";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
//System.out.println("pvRequest " + pvRequest);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy!=null);
//System.out.println("pvCopy " +pvCopy.dump());
        pvCopyStructure = pvCopy.createPVStructure();
//System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        int offsetRecord = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvTimeStamp).getPVField());
//System.out.println("timeStamp offsetRecord " + offsetRecord);
        int offCopy = pvCopyStructure.getSubField("timeStamp").getFieldOffset();
//System.out.println("offCopy " + offCopy);
        assert(offsetRecord==offCopy);
        offsetRecord = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvRecordSeconds).getPVField());
//System.out.println("timeStamp,seconds offsetRecord " + offsetRecord);
        offCopy = pvCopyStructure.getSubField("timeStamp.secondsPastEpoch").getFieldOffset();
//System.out.println("offCopy " + offCopy);
        assert(offsetRecord==offCopy);
        offsetRecord = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvRecordNanoseconds).getPVField());
//System.out.println("timeStamp.nano offsetRecord " + offsetRecord);
        offCopy = pvCopyStructure.getSubField("timeStamp.nanoseconds").getFieldOffset();
//System.out.println("offCopy " + offCopy);
        assert(offsetRecord==offCopy);
        offsetRecord = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvRecordUserTag).getPVField());
//System.out.println("timeStamp.userTag offsetRecord " + offsetRecord);
        offCopy = pvCopyStructure.getSubField("timeStamp.userTag").getFieldOffset();
//System.out.println("offCopy " + offCopy);
        assert(offsetRecord==offCopy);
    }
    
    public static void exampleTest() {
        System.out.printf("%n%n****Example****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = null;
        String request = "";
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        PVStructure pvCopyStructure = null;
        BitSet bitSet = null;
        pvRecord = master.findRecord("powerSupply");
        assertTrue(pvRecord!=null);
        
        request = "power.value";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy!=null);
//System.out.printf("pvCopy%s",pvCopy.dump());
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        PVRecordStructure recordPVStructure = pvRecord.getPVRecordStructure();
        PVStructure pvrs = recordPVStructure.getPVStructure();
        PVField pvrf = pvrs.getSubField("power.value");
        PVDouble pvDouble = (PVDouble)pvrf;
        pvDouble.put(.04);
        PVRecordField recordPVField = pvRecord.findPVRecordField(pvrf);
        int offset = pvCopy.getCopyOffset(recordPVField.getPVField());
//System.out.printf("offset %d%n",offset);
        assertTrue(offset==pvCopyStructure.getSubField("power.value").getFieldOffset());
        pvRecord.lock();
        pvCopy.updateCopyFromBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
//System.out.printf("bitSet %s%n",bitSet.toString());
        pvDouble = (PVDouble)pvCopyStructure.getSubField("power.value");
        pvDouble.put(2.0);
        bitSet.set(0);
        pvRecord.lock();
        pvCopy.updateMaster(pvCopyStructure, bitSet);
        pvRecord.unlock();
        pvDouble = (PVDouble)pvrf;
        double value = pvDouble.get();
//System.out.printf("value %f%n",value);
        assertTrue(value==2.0);
        
        request = "current.alarm.severity";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        assertTrue(pvCopy!=null);
//System.out.printf("pvCopy%s",pvCopy.dump());
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        PVInt pvSeverity = pvRecord.getPVRecordStructure().getPVStructure().getIntField("current.alarm.severity");
        assertTrue(pvSeverity!=null);
        bitSet.clear();
        int severity = pvSeverity.get();
        if(severity==0) {
            severity = 1;
        } else {
            severity = 0;
        }
        pvSeverity.put(severity);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
//System.out.printf("bitSet %s%n",bitSet.toString());
        assertTrue(bitSet.length()!=0);
        pvSeverity = pvCopyStructure.getIntField("current.alarm.severity");
        assertTrue(pvSeverity!=null);
        assertTrue(severity==pvSeverity.get());
        
        request = "alarm,timeStamp,power.value";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        
        request = "alarm,timeStamp,power.value[xxx=yyy]";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        pvDouble = pvCopyStructure.getDoubleField("power.value");
        assertTrue(pvDouble!=null);
        
        request = "xxx,yyy{zzz,vvv},alarm,timeStamp,power.value";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);

        request = "field(alarm,timeStamp,current.value)";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        PVDouble pvValue = pvCopyStructure.getDoubleField("current.value");
        assertTrue(pvValue!=null);
        
        request = "field(alarm,timeStamp,power.value,current.value,voltage.value)";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s",pvCopyStructure);
        
        request = "field(alarm,timeStamp,power{value,alarm},"
                + "current{value,alarm},voltage{value,alarm})";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        
        pvRecord = master.findRecord("powerSupplyArray");
        request = "field(alarm,timeStamp,supply{" 
           + "0{voltage.value,current.value,power.value},"
           + "1{voltage.value,current.value,power.value}"
           + "})";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("pvCopyStructure%n%s%n",pvCopyStructure);
        
        pvRecord = master.findRecord("powerSupplyArray");
        request = "field(alarm,timeStamp,supply.0.current.value)";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
        System.out.printf("%npvCopyStructure%n%s%n",pvCopyStructure);
    }
    
    
    public static void longTest() {  
        System.out.printf("%n%n****Long Test****%n");
        // definitions for request structure to pass to PVCopyFactory
        int offset = 0;
        PVStructure pvRequest = null;
        // definitions for PVCopy
        PVCopy pvCopy = null;
        PVRecordStructure pvRecordStructure = null;
        PVStructure pvCopyStructure = null;
        BitSet bitSet = null;
        PVField pvInRecord = null;
        PVField pvFromRecord = null;
        PVField pvFromCopy = null;
        // fields in pvRecordStructure
        PVLong pvRecordSeconds = null;
        PVInt pvRecordNanoseconds = null;
        PVInt pvRecordUserTag = null;
        PVDouble pvRecordPowerValue = null;
        PVDouble pvRecordCurrentValue = null;
        // fields in pvCopyStructure
        PVField pvCopyTimeStamp = null;
        PVLong pvCopySeconds = null;
        PVInt pvCopyNanoseconds = null;
        PVInt pvCopyUserTag = null;
        PVField pvCopyPower = null;
        PVField pvCopyPowerValue = null;
        PVField pvCopyCurrentValue = null;
        
        PVRecord pvRecord = master.findRecord("powerSupply");
//System.out.println(pvRecord);
        String request = "alarm,timeStamp,current,voltage,power";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
        System.out.printf("%nrequest %s%npvRequest%n%s%n",request,pvRequest);
        PVStructure pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,null);
        pvCopyStructure = pvCopy.createPVStructure();
//System.out.println(pvCopyStructure);
//System.out.println(pvCopy.dump());
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvInRecord = pvStructure.getSubField("power.value");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("value"));
        pvInRecord = pvStructure.getSubField("alarm");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("alarm"));
        pvRecordStructure = (PVRecordStructure)pvRecord.findPVRecordField(pvStructure.getSubField("alarm"));
        pvInRecord = pvStructure.getSubField("alarm.message");
        PVRecordField xxx = pvRecord.findPVRecordField(pvInRecord);
        offset = pvCopy.getCopyOffset(xxx.getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("message"));
        pvCopy.initCopy(pvCopyStructure, bitSet);
        bitSet.clear();
        pvCopyPowerValue = pvCopyStructure.getSubField("power.value");  
        pvCopyTimeStamp = pvCopyStructure.getSubField("timeStamp");
        pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        pvRecordNanoseconds = (PVInt)pvStructure.getSubField("timeStamp.nanoseconds");
        pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        pvRecordNanoseconds.put(1000);
        pvRecordUserTag.put(1);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        bitSetUtil.compress(bitSet, pvCopyStructure);
        showModified("update nanoseconds " + pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        bitSet.clear();
        pvRecordPowerValue.put(20.0);
        pvRecordSeconds.put(20000);
        pvRecordNanoseconds.put(2000);
        pvRecordUserTag.put(2);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        bitSetUtil.compress(bitSet, pvCopyStructure);
        showModified("update value and timeStamp " + pvCopyPowerValue.toString() + " " + pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        bitSet.clear();
        System.out.printf(
             "%npower, current, voltage. For each value and alarm."
              + " Note that PVRecord.power does NOT have an alarm field.%n");
        request = "field(alarm,timeStamp,power{value,alarm},"
                + "current{value,alarm},voltage{value,alarm})";
//System.out.println("request " + request);
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
//System.out.println(pvRequest);
        assertTrue(pvRequest!=null);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,"field");
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
//System.out.println(pvCopyStructure);
        pvInRecord = pvStructure.getSubField("current.value");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("value"));
        pvRecordStructure = (PVRecordStructure)pvRecord.findPVRecordField(pvStructure.getSubField("current.alarm"));
        pvInRecord = pvStructure.getSubField("current.alarm.message");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("message"));
        pvCopy.initCopy(pvCopyStructure, bitSet);
//System.out.println(pvCopyStructure);
//System.out.println(pvRecord.getPVRecordStructure().getPVField());
        bitSet.clear();
        // get pvRecord fields
        pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        pvRecordNanoseconds = (PVInt)pvStructure.getSubField("timeStamp.nanoseconds");
        pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        pvRecordPowerValue = (PVDouble)pvStructure.getSubField("power.value");
        pvRecordCurrentValue = (PVDouble)pvStructure.getSubField("current.value");
        // get pvStructureForCopy fields
        pvCopyTimeStamp = pvCopyStructure.getSubField("timeStamp");
        pvCopySeconds = (PVLong)pvCopyStructure.getSubField("timeStamp.secondsPastEpoch");
        pvCopyNanoseconds = (PVInt)pvCopyStructure.getSubField("timeStamp.nanoseconds");
        pvCopyUserTag = (PVInt)pvCopyStructure.getSubField("timeStamp.userTag");
        pvCopyPower = pvCopyStructure.getSubField("power");
        pvCopyPowerValue = pvCopyStructure.getSubField("power.value"); 
        pvCopyCurrentValue = pvCopyStructure.getSubField("current.value"); 
        pvRecordNanoseconds.put(1000);
        pvRecordUserTag.put(1);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        bitSetUtil.compress(bitSet, pvCopyStructure);
        showModified("update nanoseconds " + pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        bitSet.clear();
        pvRecordPowerValue.put(4.0);
        pvRecordCurrentValue.put(.4);
        pvRecordSeconds.put(40000);
        pvRecordNanoseconds.put(4000);
        pvRecordUserTag.put(4);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        assertTrue(bitSet.get(pvCopyPowerValue.getFieldOffset()));
        assertFalse(bitSet.get(pvCopyPower.getFieldOffset()));
        assertTrue(bitSet.get(pvCopySeconds.getFieldOffset()));
        assertTrue(bitSet.get(pvCopyNanoseconds.getFieldOffset()));
        assertTrue(bitSet.get(pvCopyUserTag.getFieldOffset()));
        assertFalse(bitSet.get(pvCopyTimeStamp.getFieldOffset()));
        showModified("before compress update value, current, and timeStamp " + pvCopyPowerValue.toString() + " "
                + pvCopyCurrentValue.toString() + " "+ pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        bitSetUtil.compress(bitSet, pvCopyStructure);
        assertFalse(bitSet.get(pvCopyPowerValue.getFieldOffset()));
        assertTrue(bitSet.get(pvCopyPower.getFieldOffset()));
        assertFalse(bitSet.get(pvCopySeconds.getFieldOffset()));
        assertFalse(bitSet.get(pvCopyNanoseconds.getFieldOffset()));
        assertFalse(bitSet.get(pvCopyUserTag.getFieldOffset()));
        assertTrue(bitSet.get(pvCopyTimeStamp.getFieldOffset()));
        showModified("after compress update value, current, and timeStamp " + pvCopyPowerValue.toString() + " "
                + pvCopyCurrentValue.toString() + " "+ pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        bitSetUtil.compress(bitSet, pvCopyStructure);
        showModified("after second compress update value, current, and timeStamp " + pvCopyPowerValue.toString() + " "
                + pvCopyCurrentValue.toString() + " "+ pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        PVStructure empty = pvDataCreate.createPVStructure(new String[0],new PVField[0]);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), empty,null);
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
        pvCopy.initCopy(pvCopyStructure, bitSet);
//System.out.println(pvCopyStructure);
//System.out.println(pvRecord.getPVRecordStructure().getPVField());
//System.out.println(pvRecord.getPVRecordStructure().getPVField().getField());
//String yyy = "voltage.input.input";
//PVField xxx = pvCopyStructure.getSubField(yyy);
//System.out.println("copy " + xxx.toString() + " numberFields " +xxx.getNumberFields());
//xxx = ((PVStructure)pvRecord.getPVRecordStructure().getPVField()).getSubField(yyy);
//System.out.println("copy " + xxx.toString() + " numberFields " +xxx.getNumberFields());
        compareCopyWithMaster("after init",pvCopyStructure,pvCopy);
        pvRecordPowerValue.put(6.0);
        pvRecordCurrentValue.put(.6);
        pvRecordSeconds.put(60000);
        pvRecordNanoseconds.put(6000);
        pvRecordUserTag.put(6);
        compareCopyWithMaster("after change record ",pvCopyStructure,pvCopy);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        compareCopyWithMaster("after updateCopy",pvCopyStructure,pvCopy);
        pvCopySeconds = (PVLong)pvCopyStructure.getSubField("timeStamp.secondsPastEpoch");
        pvCopyNanoseconds = (PVInt)pvCopyStructure.getSubField("timeStamp.nanoseconds");
        pvCopyUserTag = (PVInt)pvCopyStructure.getSubField("timeStamp.userTag");
///System.out.println(pvCopyStructure);
//System.out.println(pvRecord);
//System.out.println(pvRecord.getPVRecordStructure().getPVField().getField().toString());
        PVDouble pvDouble = (PVDouble)pvCopyStructure.getSubField("power.value");
        pvDouble.put(7.0);
        pvCopySeconds.put(700);
        pvCopyNanoseconds.put(7000);
        pvCopyUserTag.put(7);
        compareCopyWithMaster("after change copy ",pvCopyStructure,pvCopy);
//System.out.println("pvCopyStructure");
//System.out.println(pvCopyStructure);
//System.out.println("pvRecord");
//System.out.println(pvRecord.getPVRecordStructure().getPVField());
        pvRecord.lock();
        pvCopy.updateMaster(pvCopyStructure, bitSet);
        pvRecord.unlock();
        compareCopyWithMaster("after updateMaster",pvCopyStructure,pvCopy);
        
        System.out.printf("%npowerSupplyArray: value alarm and timeStamp."
                 + " Note where power and alarm are chosen.%n");
        pvRecord = master.findRecord("powerSupplyArray");
        pvStructure = pvRecord.getPVRecordStructure().getPVStructure();
//System.out.println("pvStructure " + pvStructure);
        request = "supply{0{power.value,alarm}},timeStamp";
//System.out.println("request " + request);
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
//System.out.println("pvRequest " + pvRequest);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,null);
//System.out.println(pvCopy.dump());
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
//System.out.println(pvCopyStructure.toString());
        pvInRecord = pvStructure.getSubField("supply.0.power.value");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        assertTrue(offset!=0);
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
//System.out.println(pvFromCopy);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("value"));
        pvInRecord = (PVStructure)pvStructure.getSubField("supply.0.alarm");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("alarm"));
        pvRecordStructure = (PVRecordStructure)pvRecord.findPVRecordField(pvStructure.getSubField("supply.0.alarm"));
        pvInRecord = pvStructure.getSubField("supply.0.alarm.message");
        offset = pvCopy.getCopyOffset(pvRecordStructure.getPVStructure(),pvRecord.findPVRecordField(pvInRecord).getPVField());
        pvFromRecord = pvCopy.getMasterPVField(offset);
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("message"));
        pvCopy.initCopy(pvCopyStructure, bitSet);
        bitSet.clear();
        pvCopyPowerValue = pvCopyStructure.getSubField("supply.0.power.value");
        assertTrue(pvCopyPowerValue!=null);
        pvCopyTimeStamp = pvCopyStructure.getSubField("timeStamp");
        pvRecordSeconds = (PVLong)pvStructure.getSubField("timeStamp.secondsPastEpoch");
        pvRecordNanoseconds = (PVInt)pvStructure.getSubField("timeStamp.nanoseconds");
        pvRecordUserTag = (PVInt)pvStructure.getSubField("timeStamp.userTag");
        pvRecordPowerValue = (PVDouble)pvStructure.getSubField("supply.0.power.value");
        pvRecordNanoseconds.put(1000);
        pvRecordUserTag.put(1);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        bitSetUtil.compress(bitSet, pvCopyStructure);
        showModified("update nanoseconds " + pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);
        bitSet.clear();
        pvRecordPowerValue.put(2.0);
        pvRecordSeconds.put(20000);
        pvRecordNanoseconds.put(2000);
        pvRecordUserTag.put(2);
        pvRecord.lock();
        pvCopy.updateCopySetBitSet(pvCopyStructure, bitSet);
        pvRecord.unlock();
        bitSetUtil.compress(bitSet, pvCopyStructure);
        showModified("update value and timeStamp " + pvCopyPowerValue.toString() + " " + pvCopyTimeStamp.toString(),pvCopyStructure,bitSet);

        request = "supply.0.current{value,alarm},timeStamp";
//System.out.println("request " + request);
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        System.out.println("pvRequest " + pvRequest);
        pvCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest,null);
//System.out.println(pvCopy.dump());
        pvCopyStructure = pvCopy.createPVStructure();
        bitSet = new BitSet(pvCopyStructure.getNumberFields());
//System.out.println(pvCopyStructure.toString());
        pvInRecord = pvStructure.getSubField("supply.0.current.value");
        offset = pvCopy.getCopyOffset(pvRecord.findPVRecordField(pvInRecord).getPVField());
        assertTrue(offset!=0);
        PVRecordField pvRecordField = pvRecord.findPVRecordField(pvCopy.getMasterPVField(offset));
        assertTrue(pvRecordField!=null);
        pvFromRecord = pvRecordField.getPVField();
        pvFromCopy = pvCopyStructure.getSubField(offset);
        assertTrue(pvInRecord==pvFromRecord);
//System.out.println("pvFromCopy " +pvFromCopy);
//System.out.println("pvFromRecord " +pvFromRecord);
        assertTrue(pvFromCopy.getFieldName().equals("value"));
    }
    
    static void showModified(String message,PVStructure pvStructure,BitSet bitSet) {
        System.out.println();
        System.out.println(message);
        System.out.printf("modifiedFields bitSet %s%n", bitSet);
        int size = bitSet.size();
        int index = -1;
        while(++index < size) {
            if(bitSet.get(index)) {
                PVField pvField = pvStructure.getSubField(index);
                StringBuilder builder = new StringBuilder();
                convert.getFullFieldName(builder,pvField);
               System.out.println("   " + builder.toString());
            }
        }
    }
    
    static void compareCopyWithMaster(String message,PVStructure pvStructure,PVCopy pvCopy) {
        System.out.println();
        System.out.println(message);
        int length = pvStructure.getNumberFields();
        for(int offset=1; offset<length; offset++) {
            PVField pvCopyField = pvStructure.getSubField(offset);
            if(pvCopyField.getField().getType()==Type.structure) continue;
            PVField pvMasterField = pvCopy.getMasterPVField(offset);
            if(!pvCopyField.equals(pvMasterField)) {
                StringBuilder builder = new StringBuilder();
                builder.append("    ");
                convert.getFullFieldName(builder,pvCopyField);
                builder.append(" NE ");
                convert.getFullFieldName(builder,pvMasterField);
                builder.append(" NE ");
                System.out.println(builder.toString());
            }
        }
    }
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        master.cleanMaster();
    }
}


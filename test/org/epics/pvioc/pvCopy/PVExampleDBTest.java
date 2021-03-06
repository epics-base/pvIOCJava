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
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVReplaceFactory;
import org.epics.pvioc.xml.XMLToPVDatabaseFactory;



/**
 * JUnit test for pvAccess.
 * It also provides examples of how to use the pvAccess interfaces.
 * @author mrk
 *
 */
public class PVExampleDBTest extends TestCase {
    private final static PVDatabase master = PVDatabaseFactory.getMaster();
    private final static Requester requester = new RequesterImpl();
    private static final CreateRequest createRequest = CreateRequest.create();
   
    
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
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/xml/rpc.xml", iocRequester);
        XMLToPVDatabaseFactory.convert(master,"${JAVAIOC}/example/exampleDB.xml", iocRequester);
        PVReplaceFactory.replace(master);
        exampleTest();
    }
    
    public static void exampleTest() {
        System.out.printf("%n%n****Example****%n");
        // definitions for request structure to pass to PVCopyFactory
        PVRecord pvRecord = null;
        String request = "";
        PVStructure pvRequest = null;
        // definitions for PVCopy
        String iocname = System.getenv("IOCNAME");
        pvRecord = master.findRecord(iocname + "recordListPGRPC");
        assertTrue(pvRecord!=null);
System.out.println(pvRecord);
        request = "record[process=true]putField(argument)getField(result)";
        pvRequest = createRequest.createRequest(request);
        if(pvRequest==null) requester.message(createRequest.getMessage(), MessageType.error);
        assertTrue(pvRequest!=null);
System.out.println(pvRequest);
        assertTrue(pvRequest!=null);
        System.out.println("pvRequest " + pvRequest);
        PVCopy pvPutCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest, "putField");
System.out.println(pvPutCopy.dump());
        PVCopy pvGetCopy = PVCopyFactory.create(pvRecord.getPVRecordStructure().getPVStructure(), pvRequest, "getField");
System.out.println(pvGetCopy.dump());
        PVStructure pvPutStructure = pvPutCopy.createPVStructure();
System.out.println(pvPutStructure);
        PVStructure pvGetStructure = pvGetCopy.createPVStructure();
System.out.println(pvGetStructure);
        BitSet putBitSet = new BitSet(pvPutStructure.getNumberFields());
        pvPutCopy.initCopy(pvPutStructure, putBitSet);
        BitSet getBitSet = new BitSet(pvGetStructure.getNumberFields());
        pvGetCopy.initCopy(pvGetStructure, getBitSet);
        
    }
    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        master.cleanMaster();
    }
}


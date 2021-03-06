/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.epics.pvaccess.PVAException;
import org.epics.pvaccess.server.rpc.RPCServer;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.Requester;
import org.epics.pvioc.database.PVDatabase;
import org.epics.pvioc.database.PVDatabaseFactory;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.install.Install;
import org.epics.pvioc.install.InstallFactory;
/**
 * The main program to start a JavaIOC.
 * The program is started with a command line of
 * java org.epics.pvioc.JavaIOC 
 * The command line options are:
 * <pre>
 *     -structures list
 *             list is a list of xml files containing structure definitions. Each is parsed and put into the master database
 *     -records list
 *             list is a list of xml files containing records definitions. Each is parsed and started and put into the master database 
 *     -dumpStructures
 *             Dump all structures in the master database
 *     -dumpRecords
 *             Dump all record instances in the master database
 *     -server serverFile
 *             A server is specified in the serverFile
 *     -rpcService rpcServiceFile
 *             List of "serviceName,XXXService" pairs
 *     -pvRecord pvRecordFile
 *             List of "recordName,XXXRecord pairs
 *     -run
 *             Starts the JavaIOC as RPCServer
 *            
 * @author mrk
 *
 */
public class JavaIOC {
    private enum State {
        structure,
        record,
        server,
        rpcService,
        pvRecord
    }

    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final Install install = InstallFactory.get();
    /**
     * read and dump a database instance file.
     * @param  args is a sequence of flags and filenames.
     */
    public static void main(String[] args) {
        if(args.length==1 && args[0].equals("?")) {
            usage();
            return;
        }
        boolean runForever = false;
        Requester iocRequester = new Listener();
        int nextArg = 0;
        State state = null;
        String rpcServiceFile = null;
        String pvRecordFile = null;
        while(nextArg<args.length) {
            String arg = args[nextArg++];
            if(arg.charAt(0) == '-') {
                if(arg.length()>1) {
                    arg = arg.substring(1);
                } else {
                    if(nextArg>=args.length) {
                        System.out.printf("last arg is - illegal\n");
                        return;
                    }
                    arg = args[nextArg++];
                }
                if(arg.equals("dumpStructures")) {
                    dumpStructures();
                } else if(arg.equals("dumpRecords")) {
                    dumpRecords();
                } else if(arg.equals("structures")){
                    state = State.structure;
                } else if(arg.equals("records")){
                    state = State.record;
                } else if(arg.equals("rpcService")){
                    state = State.rpcService;
                } else if(arg.equals("pvRecord")){
                    state = State.pvRecord;
                } else if(arg.equals("run")) {
                	runForever = true;
                } else if(arg.equals("server")) {
                    state = State.server;
                } else {
                    System.err.println("unknown arg: " + arg);
                    usage();
                    return;
                }
            } else if(state==State.structure){
                parseStructures(arg,iocRequester);
            } else if(state==State.record){
                parseRecords(arg,iocRequester);
            } else if(state==State.server) {
                startServer(arg);
            } else if(state==State.rpcService) {
                rpcServiceFile = arg;
            } else if(state==State.pvRecord) {
                pvRecordFile = arg;
            } else {
                System.err.println("unknown arg: " + arg);
                usage();
                return;
            }
            if (runForever) {
                RPCServer rpcServer = new RPCServer();
                System.out.println(startRPCServices(rpcServer,rpcServiceFile));
                startPVRecords(pvRecordFile);
                try {
                    rpcServer.run(0);
                } catch (PVAException xx) {
                    throw new RuntimeException("RPCServer exception", xx);
                }
            }
        }
    }
    
    static void usage() {
        System.out.println("Usage:"
                + " -structures fileList"
                + " -records fileList"
                + " -dumpStructures"
                + " -dumpRecords"
                + " -server file"
                + " -rpcService file"
                + " -pvRecord file"
                + " -run ");
    }
    
    static void printError(String channel, String factory,String message) {
        System.err.println("channel "+ channel + " factory " + factory + " " + message);
    }
    
    static void printError(String factory,String message) {
        System.err.println("factory " + factory + " " + message);
    }
    
    static void startPVRecords(String fileName) {
        if(fileName==null) return; 
        System.out.println("starting PVRecords fileName " + fileName);
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String inLine = null;
            while((inLine = in.readLine()) !=null) {
                int comma = inLine.indexOf(',');
                String recordName = inLine.substring(0,comma);
                String factoryName = inLine.substring(comma+1);
                System.out.println("starting record factoryName " + factoryName);
                Class<?> startClass;
                Method method = null;
                try {
                    startClass = Class.forName(factoryName);
                }catch (ClassNotFoundException e) {
                    printError(recordName,factoryName,"record factory "
                            + e.getLocalizedMessage()
                            + " class not found");
                    continue;
                }
                try {
                    method = startClass.getMethod("start",String.class);
                } catch (NoSuchMethodException e) {
                    printError(recordName,factoryName,"record factory "
                            + e.getLocalizedMessage()
                            + " method start not found");
                    continue;
                }
                if(!Modifier.isStatic(method.getModifiers())) {
                    printError(recordName,factoryName,"record factory "
                            + factoryName
                            + " start is not a static method ");
                    continue;
                }
                try {
                    method.invoke(null,recordName );
                } catch(IllegalAccessException e) {
                    printError(recordName,factoryName,"record start IllegalAccessException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(IllegalArgumentException e) {
                    printError(recordName,factoryName,"record start IllegalArgumentException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(InvocationTargetException e) {
                    printError(recordName,factoryName,"record start InvocationTargetException "
                            + e.getLocalizedMessage());
                    continue;
                }
            }
            in.close();
        } catch (IOException e) {
            System.err.println("PVRecords error " + e.getMessage());
            return;
        }
    }
    
    static String startRPCServices(RPCServer rpcServer,String fileName) {
        String rtnValue = "";
        if(fileName==null) return rtnValue; 
        System.out.println("starting RPCServices fileName " + fileName);
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String inLine = null;
            while((inLine = in.readLine()) !=null) {
                int comma = inLine.indexOf(',');
                String serviceName = inLine.substring(0,comma);
                String factoryName = inLine.substring(comma+1);
                System.out.println("starting service factoryName " + factoryName);
                Class<?> startClass;
                Method method = null;
                try {
                    startClass = Class.forName(factoryName);
                }catch (ClassNotFoundException e) {
                    printError(serviceName,factoryName,"server factory "
                            + e.getLocalizedMessage()
                            + " class not found");
                    continue;
                }
                try {
                    method = startClass.getMethod("start",String.class,RPCServer.class);
                } catch (NoSuchMethodException e) {
                    printError(serviceName,factoryName,"server factory "
                            + e.getLocalizedMessage()
                            + " method start not found");
                    continue;
                }
                if(!Modifier.isStatic(method.getModifiers())) {
                    printError(serviceName,factoryName,"server factory "
                            + factoryName
                            + " start is not a static method ");
                    continue;
                }
                try {
                    method.invoke(null,serviceName,rpcServer );
                } catch(IllegalAccessException e) {
                    printError(serviceName,factoryName,"server start IllegalAccessException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(IllegalArgumentException e) {
                    printError(serviceName,factoryName,"server start IllegalArgumentException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(InvocationTargetException e) {
                    printError(serviceName,factoryName,"server start InvocationTargetException "
                            + e.getLocalizedMessage());
                    continue;
                }
                if(rtnValue.length()>0) rtnValue += "\n";
                rtnValue += serviceName + " " + factoryName;
            }
            in.close();
        } catch (IOException e) {
            System.err.println("startServer error " + e.getMessage());
            return rtnValue;
        }
        return rtnValue;
    }
    
    static void startServer(String fileName) {
        System.out.println("starting servers fileName " + fileName);
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String factoryName = null;
            while((factoryName = in.readLine()) !=null) {
                System.out.println("starting server factoryName " + factoryName);
                Class<?> startClass;
                Method method = null;
                try {
                    startClass = Class.forName(factoryName);
                }catch (ClassNotFoundException e) {
                    printError(factoryName,"server factory "
                            + e.getLocalizedMessage()
                            + " class not found");
                    continue;
                }
                try {
                    method = startClass.getDeclaredMethod("start", (Class[])null);
                } catch (NoSuchMethodException e) {
                    printError(factoryName,"server factory "
                            + e.getLocalizedMessage()
                            + " method start not found");
                    continue;
                }
                if(!Modifier.isStatic(method.getModifiers())) {
                    printError(factoryName,"server factory "
                            + factoryName
                            + " start is not a static method ");
                    continue;
                }
                try {
                    method.invoke(null, new Object[0]);
                } catch(IllegalAccessException e) {
                    printError(factoryName,"server start IllegalAccessException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(IllegalArgumentException e) {
                    printError(factoryName,"server start IllegalArgumentException "
                            + e.getLocalizedMessage());
                    continue;
                } catch(InvocationTargetException e) {
                    printError(factoryName,"server start InvocationTargetException "
                            + e.getLocalizedMessage());
                    continue;
                }
            }
            in.close();
        } catch (IOException e) {
            System.err.println("startServer error " + e.getMessage());
            return;
        }
    }
        
    static void dumpStructures() {
        String[] names = masterPVDatabase.getStructureNames();
        PVStructure[] pvStructures = masterPVDatabase.getStructures();
        if(pvStructures.length>0) System.out.println("\n\nstructures");
        for(int i=0; i<names.length; ++i) {
            System.out.println(names[i]);
            System.out.println(pvStructures[i]);
        }
    }
    
    static void dumpRecords() {
        PVRecord[] pvRecords = masterPVDatabase.getRecords();
        if(pvRecords.length>0) System.out.println("\n\nrecords");
        for(PVRecord pvRecord : pvRecords) {
            System.out.print(pvRecord.toString());
        }
    }

    static void parseStructures(String fileName,Requester iocRequester) {
    	long startTime = 0;
    	long endTime = 0;
    	startTime = System.nanoTime();
    	try {
    		install.installStructures(fileName,iocRequester);
    	}  catch (IllegalStateException e) {
    	    System.out.printf("parseStructures: %s%n",e.getMessage());
    	    e.printStackTrace();
    	}
    	endTime = System.nanoTime();
    	double diff = (double)(endTime - startTime)/1e9;
    	System.out.printf("\ninstalled structures %s time %f seconds\n",fileName,diff);
    }

    static void parseRecords(String fileName,Requester iocRequester) {
    	long startTime = 0;
    	long endTime = 0;
    	startTime = System.nanoTime();
    	try {
    		install.installRecords(fileName,iocRequester);
    	}  catch (IllegalStateException e) {
    	    System.out.printf("parseRecords: %s%n",e.getMessage());
    	    e.printStackTrace();
    	}
    	endTime = System.nanoTime();
    	double diff = (double)(endTime - startTime)/1e9;
    	System.out.printf("\ninstalled records %s time %f seconds\n",fileName,diff);
    }
     
    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "javaIOC";
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }
}

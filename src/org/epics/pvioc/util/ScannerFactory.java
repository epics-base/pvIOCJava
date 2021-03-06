/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.pvioc.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.epics.pvdata.misc.RunnableReady;
import org.epics.pvdata.misc.ThreadCreate;
import org.epics.pvdata.misc.ThreadCreateFactory;
import org.epics.pvdata.misc.ThreadPriority;
import org.epics.pvdata.misc.ThreadReady;
import org.epics.pvdata.property.TimeStamp;
import org.epics.pvdata.property.TimeStampFactory;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvioc.database.PVRecord;
import org.epics.pvioc.database.PVRecordStructure;
import org.epics.pvioc.support.ProcessToken;
import org.epics.pvioc.support.RecordProcess;
import org.epics.pvioc.support.RecordProcessRequester;


/**
 * Factory for periodic and event scanning.
 * All methods are thread safe.
 * @author mrk
 *
 */
public class ScannerFactory {
    private static PeriodicScanner periodicScanner = new PeriodicScannerImpl();
    private static EventScanner eventScanner = new EventScannerImpl();

    /**
     * Get the interface for the periodic scanner.
     * @return The interface.
     */
    public static PeriodicScanner getPeriodicScanner() {
        return periodicScanner;
    }
    /**
     * Get the interface for the event scanner.
     * @return The interface.
     */
    public static EventScanner getEventScanner() {
        return eventScanner;
    }
     
    private static ThreadCreate threadCreate = ThreadCreateFactory.getThreadCreate();
    private static String lineBreak = System.getProperty("line.separator");

    private static class ProcessRecord implements RecordProcessRequester {
        private String name;
        private RecordProcess recordProcess;
        private PVRecord pvRecord;
        private PVStructure pvStructure;
        private ProcessToken processToken = null;
        private volatile boolean isActive = false;
        private int numberConsecutiveActive = 0;
        private PVInt pvMaxConsecutiveActive = null;
        private int maxConsecutiveActive = 1;
        private volatile boolean release = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForNotActive = lock.newCondition();
        private TimeStamp timeStamp = null;
        private ScanField scanField = null;
       

        private ProcessRecord(String name,RecordProcess recordProcess) {
        	this.name = name;
        	this.recordProcess = recordProcess;
        	processToken = recordProcess.requestProcessToken(this);
        	if(processToken==null) return;
        	pvRecord = recordProcess.getRecord();
        	PVRecordStructure pvRecordStructure = pvRecord.getPVRecordStructure();
        	pvStructure = pvRecordStructure.getPVStructure();
        	PVField pvField = pvStructure.getSubField("scan.maxConsecutiveActive");
        	if(pvField!=null && (pvField instanceof PVInt)) {
        		pvMaxConsecutiveActive = (PVInt)pvField;
        	}
        	scanField = ScanFieldFactory.create(pvRecord);
        }

        private boolean canProcess() {
        	return (processToken==null) ? false : true;
        }

        private void execute(TimeStamp timeStamp) {
        	if(release) return;
        	if(isActive) {
        		if(pvMaxConsecutiveActive!=null && numberConsecutiveActive==0) {
        			maxConsecutiveActive = pvMaxConsecutiveActive.get();
        		}
        		if(++numberConsecutiveActive == maxConsecutiveActive) {
        			pvRecord.lock();
        			try {
        				message(" active too long", MessageType.info);
        			} finally {
        				pvRecord.unlock();
        			}
        		}
        	} else {
        		isActive = true;
        		numberConsecutiveActive = 0;
        		this.timeStamp = timeStamp;
        		recordProcess.queueProcessRequest(processToken);
        	}
        }
		/* (non-Javadoc)
		 * @see org.epics.pvioc.support.RecordProcessRequester#becomeProcessor()
		 */
		@Override
		public void becomeProcessor() {
        	recordProcess.process(processToken,false, timeStamp);
		}
        
        @Override
		public void canNotProcess(String reason) {
        	message("can not process " + reason,MessageType.error);
			recordProcessComplete();
		}

		@Override
		public void lostRightToProcess() {
			if(scanField!=null) {
				PVInt pvInt = scanField.getScanTypeIndexPV();
				pvInt.put(0); // set scan type passive
				return;
			}
			throw new IllegalStateException(
                    pvRecord.getRecordName() + " lostRightToProcess");
		}

		private PVRecord getPVRecord() {
            return pvRecord;
        }
        
        private void release() {
            try {
                lock.lock();
                try {
                    release = true;
                    while(isActive) {
                        waitForNotActive.await();
                    }
                } finally {
                    lock.unlock();
                }
            } catch(InterruptedException e) {}
            recordProcess.releaseProcessToken(processToken);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.RecordProcessRequester#getRecordProcessRequesterName()
         */
        public String getRequesterName() {
            return name;
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            pvRecord.message(pvRecord.getRecordName() + " " + message, messageType);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.RecordProcessRequester#recordProcessComplete(org.epics.pvioc.process.RequestResult)
         */
        public void recordProcessComplete() {
            lock.lock();
            try {
                isActive = false;
                if(release) waitForNotActive.signal();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.process.RecordProcessRequester#recordProcessResult(org.epics.pvioc.util.AlarmSeverity, java.lang.String, org.epics.pvioc.util.TimeStamp)
         */
        public void recordProcessResult(RequestResult requestResult) {
            // nothing to do.    
        }
    }

    private static class ProcessRecordList {
        private String name;
        private ProcessRecord[] processRecords = new ProcessRecord[0];
        
        private TimeStamp timeStamp = TimeStampFactory.create();
        
        private volatile boolean isActive = false;
        private volatile boolean listModify = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForNotActive = lock.newCondition();
        private Condition waitForModify = lock.newCondition();

        private ProcessRecordList(String name) {
            this.name = name;
        }

        private void runList(long startTime) {                      
            lock.lock();
            try {
                while(listModify) {
                    try {
                        waitForModify.await();
                    } catch(InterruptedException e) {
                        return;
                    }
                }
                isActive = true;
            } finally {
                lock.unlock();
            }
            timeStamp.put(startTime);
            for(ProcessRecord processRecord : processRecords) {
                if(processRecord!=null) processRecord.execute(timeStamp);
            }
            lock.lock();
            try {
                isActive = false;
                if(listModify) {
                    waitForNotActive.signal();
                }               
            } finally {
                lock.unlock();
            }
        }

        boolean remove(PVRecord pvRecord) {
            lock.lock();
            try {
                ProcessRecord recordExecutor = null;
                int index = 0;
                for(int i=0; i< processRecords.length; i++) {
                    if(processRecords[i].getPVRecord()==pvRecord) {
                        recordExecutor = processRecords[i];
                        index = i;
                        break;
                    }
                }
                if(recordExecutor==null) return false;
                listModify = true;
                while(isActive) {
                    try {
                        waitForNotActive.await();
                    } catch(InterruptedException e) {
                        return false;
                    }
                }
                processRecords[index] = null;
                recordExecutor.release();
                listModify = false;
                waitForModify.signal();
                return true;
            } finally {
                lock.unlock();
            }
        }
        
        void add(ProcessRecord recordExecutor) {
            lock.lock();
            try {
                listModify = true;
                while(isActive) {
                    try {
                        waitForNotActive.await();
                    } catch(InterruptedException e) {
                        return;
                    }
                }
                int index = -1;
                for(int i=0; i< processRecords.length; i++) {
                    if(processRecords[i]==null) {
                        index = i;
                        break;
                    }
                }
                if(index<0) {
                    ProcessRecord[] executors = new ProcessRecord[processRecords.length+5];
                    for(int i=0; i< processRecords.length; i++) {
                        executors[i] = processRecords[i];
                    }
                    for(int i=processRecords.length; i<executors.length -1; i++) {
                        executors[i] = null;
                    }
                    index = processRecords.length;
                    processRecords = executors;
                }
                processRecords[index] = recordExecutor;
                listModify = false;
                waitForModify.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    private static class ProcessPeriodic extends ProcessRecordList implements RunnableReady {
        private long period;
        private Thread thread;

        private ProcessPeriodic(String name,long period, int priority) {
            super(name);
            this.period = period;
            thread = threadCreate.create(name, priority, this);
            
        }

        /* (non-Javadoc)
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            threadReady.ready();
            try {
                while(true) {
                    long startTime = System.currentTimeMillis();
                    super.runList(startTime);
                    long endTime = System.currentTimeMillis();
                    long delay = period - (endTime - startTime);
                    if(delay<1) delay = 1;
                    Thread.sleep(delay);
                }
            } catch(InterruptedException e) {}
        }
        private long getPeriod() {
            return period;
        }
        private Thread getThread() {
            return thread;
        }
        private String getName() {
            return super.name;
        }
    }

    private static class PeriodNode {
        private long period;
        private LinkedList<ProcessPeriodic> priorityList = new LinkedList<ProcessPeriodic>();
        
        PeriodNode(long period) {
            this.period = period;
        }
        
        ProcessPeriodic getProcessPeriodioc(int priority,boolean addNew,String name) {
            ListIterator<ProcessPeriodic> iter = priorityList.listIterator();
            while(iter.hasNext()) {
                ProcessPeriodic next = iter.next();
                int threadPriority = next.getThread().getPriority();
                if(priority>threadPriority) continue;
                if(priority==threadPriority) return next;
                if(!addNew) return null;
                next = new ProcessPeriodic(name,period,priority);
                iter.previous();
                iter.add(next);
                return next;
            }
            if(!addNew) return null;
            ProcessPeriodic processPeriodioc = new ProcessPeriodic(name,period,priority);
            priorityList.add(processPeriodioc);
            return processPeriodioc;
        }
    }
    
    private static class PeriodicScannerImpl implements PeriodicScanner {
        private LinkedList<PeriodNode> periodList = new LinkedList<PeriodNode>();
        private ReentrantLock lock = new ReentrantLock();
        private long minPeriod = 10;
        private int deltaPeriod = 10;
                
        private PeriodicScannerImpl() {
            String envValue = System.getProperty("IOCPeriodicScanPeriodMinimum", System.getenv("IOCPeriodicScanPeriodMinimum"));
            if(envValue!=null) {
                double value = Double.parseDouble(envValue);
                minPeriod = (long)(value*1000.0);
            }
            envValue = System.getProperty("IOCPeriodicScanPeriodDelta", System.getenv("IOCPeriodicScanPeriodDelta"));
            if(envValue!=null) {
                double value = Double.parseDouble(envValue);
                deltaPeriod = (int)(value*1000.0);
            }
        }
        
        
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.PeriodicScanner#schedule(org.epics.pvioc.pv.PVRecord)
         */
        public boolean addRecord(PVRecord pvRecord) {
            ScanField scanField = ScanFieldFactory.create(pvRecord);
            if(scanField==null) {
                pvRecord.message(
                        "PeriodicScanner.addRecord invalid scan field",
                        MessageType.fatalError);;
                        return false;
            }
            if(scanField.getScanType()!=ScanType.periodic) {
                pvRecord.message(
                        "PeriodicScanner.addRecord scanType is not periodic",
                        MessageType.fatalError);;
                        return false;
            }
            double rate = scanField.getRate();
            int priority = scanField.getPriority().getJavaPriority();
            long period = rateToPeriod(rate);
            ProcessRecord processRecord = null;
            ProcessPeriodic processPeriodic = null;
            String name = "periodic(" + String.valueOf(period)
            + "," + String.valueOf(priority) + ")";
            lock.lock();
            try {
                PeriodNode periodNode = getPeriodNode(period,true);
                processPeriodic = periodNode.getProcessPeriodioc(priority, true, name);
            } finally {
                lock.unlock();
            }
            processRecord = new ProcessRecord(processPeriodic.getName(),pvRecord.getRecordProcess());
            if(!processRecord.canProcess()) {
                return false;
            }
            processPeriodic.add(processRecord);
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.PeriodicScanner#unschedule(org.epics.pvioc.pv.PVRecord, double, org.epics.pvioc.util.ThreadPriority)
         */
        public boolean removeRecord(PVRecord pvRecord, double rate, ThreadPriority threadPriority) {
            int priority = threadPriority.getJavaPriority();
            long period = rateToPeriod(rate);
            ProcessPeriodic processPeriodic = null;
            lock.lock();
            try {
                PeriodNode periodNode = getPeriodNode(period,false);
                if(periodNode!=null) {
                    processPeriodic = periodNode.getProcessPeriodioc(priority, false, null);
                }
            } finally {
                lock.unlock();
            }
            if(processPeriodic==null) {
                pvRecord.message(
                        "PeriodicScanner.unschedule but not in list",
                        MessageType.error);
                return false;
            }
            processPeriodic.remove(pvRecord);
            return true;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuilder builder = new StringBuilder();
            lock.lock();
            try {
                ListIterator<PeriodNode> iter = periodList.listIterator();
                while(iter.hasNext()) {
                    PeriodNode periodNode = iter.next();
                    ListIterator<ProcessPeriodic> iter1 = periodNode.priorityList.listIterator();
                    while(iter1.hasNext()) {
                        ProcessPeriodic next = iter1.next();
                        builder.append(showProcessPeriodic(next) + lineBreak);
                    }
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.PeriodicScanner#show(double, org.epics.pvioc.util.ThreadPriority)
         */
        public String show(double rate, ThreadPriority threadPriority) {
            StringBuilder builder = new StringBuilder();
            int priority = threadPriority.getJavaPriority();
            long period = rateToPeriod(rate);
            lock.lock();
            try {
                ProcessPeriodic processPeriodic = null;
                PeriodNode periodNode = getPeriodNode(period,false);
                if(periodNode!=null) {
                    processPeriodic = periodNode.getProcessPeriodioc(priority, false, null);
                }
                if(processPeriodic!=null) {
                    builder.append(showProcessPeriodic(processPeriodic) + lineBreak);
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.PeriodicScanner#show(double)
         */
        public String show(double rate) {
            StringBuilder builder = new StringBuilder();
            long period = rateToPeriod(rate);
            lock.lock();
            try {
                PeriodNode periodNode = getPeriodNode(period,false);
                if(periodNode!=null) {
                    ListIterator<ProcessPeriodic> iter1 = periodNode.priorityList.listIterator();
                    while(iter1.hasNext()) {
                        ProcessPeriodic next = iter1.next();
                        builder.append(showProcessPeriodic(next) + lineBreak);
                    }
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.PeriodicScanner#show(org.epics.pvioc.util.ThreadPriority)
         */
        public String show(ThreadPriority threadPriority) {
            StringBuilder builder = new StringBuilder();
            int priority = threadPriority.getJavaPriority();
            lock.lock();
            try {
                ListIterator<PeriodNode> iter = periodList.listIterator();
                while(iter.hasNext()) {
                    PeriodNode periodNode = iter.next();
                    ProcessPeriodic processPeriodic = periodNode.getProcessPeriodioc(priority, false, null);
                    if(processPeriodic!=null) {
                        builder.append(showProcessPeriodic(processPeriodic) + lineBreak);
                    }
                }
            }finally {
                lock.unlock();
            }
            return builder.toString();
        }
        
        private PeriodNode getPeriodNode(long period,boolean addNew) {
            ListIterator<PeriodNode> iter = periodList.listIterator();
            while(iter.hasNext()) {
                PeriodNode next = iter.next();
                if(period>next.period) continue;
                if(period==next.period) return next;
                if(!addNew) return null;
                next = new PeriodNode(period);
                iter.previous();
                iter.add(next);
                return next;
            }
            if(!addNew) return null;
            PeriodNode periodNode = new PeriodNode(period);
            periodList.add(periodNode);
            return periodNode;
        }
        
        private long rateToPeriod(double rate) {
            long period = (long)(rate*1000);
            if(period<minPeriod) period = minPeriod;
            long n = (period - minPeriod)/deltaPeriod;
            period = minPeriod + n*deltaPeriod;
            return period;
        }        

        private String showProcessPeriodic(ProcessPeriodic processPeriodic) {
            StringBuilder builder = new StringBuilder();
            ProcessRecordList processRecordList = processPeriodic;
            ProcessRecord[] processRecords = processRecordList.processRecords;
            Thread thread = processPeriodic.getThread();
            builder.append(String.format("thread %s period %d priority %d record list{",
                    thread.getName(),
                    processPeriodic.getPeriod(),
                    thread.getPriority()));
            for(int i=0; i<processRecords.length; i++) {
                ProcessRecord processRecord = processRecords[i];
                if(processRecord==null) continue;
                RecordProcess recordProcess = processRecord.recordProcess;
                String name = recordProcess.getRecord().getRecordName();
                builder.append(lineBreak + "    " + name);
            }
            builder.append(lineBreak + "}");
            return builder.toString();
        }
    }

    private static class ProcessEvent extends ProcessRecordList
    implements EventAnnounce, RunnableReady
    {
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForWork = lock.newCondition();
        /* gotWork prevents following race condition:
         *  super.runList results in announce being called before waitForWork is called.
         */
        private volatile boolean gotWork = false;
        private Thread thread;
        private long startTime = 0;

        private ProcessEvent(String name,int priority) {
            super(name);
            thread = threadCreate.create(name, priority, this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            boolean firstTime = true;
            try {
                while(true) {
                    lock.lock();
                    try {
                        if(firstTime) {
                            firstTime = false;
                            threadReady.ready();
                        }
                        if(!gotWork) {
                            waitForWork.await();
                        }
                        gotWork = false;
                    } finally {
                        lock.unlock();
                    }
                    super.runList(startTime);
                }
            } catch(InterruptedException e) {}
        }
        public void announce() {
            lock.lock();
            try {
                gotWork = true;
                waitForWork.signal();
            } finally {
                lock.unlock();
            }
        }

        private void setStartTime(long startTime) {
            lock.lock();
            try {
                this.startTime = startTime;
            } finally {
                lock.unlock();
            }
        }
        private Thread getThread() {
            return thread;
        }
        private String getName() {
            return super.name;
        }
    }

    private static class Announce implements EventAnnounce, RunnableReady {
        // announcerList kept for diagnostic purpose only
        private LinkedList<String> announcerList = new LinkedList<String>();
        
        private ProcessEvent[] processEvents = new ProcessEvent[0];
        
        private volatile boolean isActive = false;
        private volatile boolean listModify = false;
        private ReentrantLock lock = new ReentrantLock();
        private Condition waitForWork = lock.newCondition();
        private Condition waitForModify = lock.newCondition();
        private Condition waitForNotActive = lock.newCondition();
        private String eventName;
        private Thread thread;

        private Announce(String name) {
            super();
            eventName = name;
            thread = threadCreate.create(
                "event(" + name + ")",
                ThreadPriority.valueOf("higher").getJavaPriority(),
                this);
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.RunnableReady#run(org.epics.pvioc.util.ThreadReady)
         */
        public void run(ThreadReady threadReady) {
            boolean firstTime = true;
            while(true) {
                lock.lock();
                try {
                    if(firstTime) {
                        firstTime = false;
                        threadReady.ready();
                    }
                    try {
                        waitForWork.await();
                    } catch(InterruptedException e) {
                        return;
                    }
                    if(listModify) {
                        try {
                            waitForModify.await();
                        } catch(InterruptedException e) {
                            return;
                        }
                    }
                    isActive = true;
                } finally {
                    lock.unlock();
                }
                long startTime = System.currentTimeMillis();
                for(ProcessEvent processEvent : processEvents) {
                    if(processEvent==null) continue;
                    processEvent.setStartTime(startTime);
                    processEvent.announce();
                }
                lock.lock();
                try {
                    isActive = false;
                    if(listModify) {
                        waitForNotActive.signal();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
        
        public void announce() {
            lock.lock();
            try {
                waitForWork.signal();
            } finally {
                lock.unlock();
            }
        }

        private Thread getThread() {
            return thread;
        }

        private String getEventName() {
            return eventName;
        }

        void addProcessEvent(ProcessEvent processEvent) {
            lock.lock();
            try {
                listModify = true;
                while(isActive) {
                    try {
                        waitForNotActive.await();
                    } catch(InterruptedException e) {
                        return;
                    }
                }
                int index = -1;
                for(int i=0; i< processEvents.length; i++) {
                    if(processEvents[i]==null) {
                        index = i;
                        break;
                    }
                }
                if(index<0) {
                    ProcessEvent[] executors = new ProcessEvent[processEvents.length+1];
                    for(int i=0; i< processEvents.length; i++) {
                        executors[i] = processEvents[i];
                    }
                    index = executors.length -1;
                    processEvents = executors;
                }
                processEvents[index] = processEvent;
                listModify = false;
                waitForModify.signal();
            } finally {
                lock.unlock();
            }
        }
       
        private void addAnnouncer(String name) {
            lock.lock();
            try {
                announcerList.add(name);
            } finally {
                lock.unlock();
            }
        }
        private void removeAnnouncer(String name) {
            lock.lock();
            try {
                announcerList.remove(name);
            } finally {
                lock.unlock();
            }
        }
        private String[] getAnnouncerList() {
            lock.lock();
            try {
                int length = announcerList.size();
                String[] list = new String[length];
                ListIterator<String> iter = announcerList.listIterator();
                int ind = 0;
                while(iter.hasNext()) {
                    list[ind++] = iter.next();
                }
                return list;
            } finally {
                lock.unlock();
            }
        }
    }

    private static class EventScannerImpl implements EventScanner {
        private ReentrantLock lock = new ReentrantLock();
        private ArrayList<Announce> eventAnnouncerList = new ArrayList<Announce>();

        private EventScannerImpl() {}

        private Announce getAnnounce(String name) {
            Announce announce = null;
            ListIterator<Announce> iter = eventAnnouncerList.listIterator();
            while(iter.hasNext()) {
                announce = iter.next();
                int compare = name.compareTo(announce.getEventName());
                if(compare>0) continue;
                if(compare==0) return announce;
                announce = new Announce(name);
                iter.previous();
                iter.add(announce);
                break;
            }
            if(announce==null) {
                announce = new Announce(name);
                eventAnnouncerList.add(announce);
            }
            return announce;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.EventScanner#addEventAnnouncer(java.lang.String, java.lang.String)
         */
        public EventAnnounce addEventAnnouncer(String eventName, String announcer) {
            lock.lock();
            try {
                Announce announce = getAnnounce(eventName);
                announce.addAnnouncer(announcer);
                return announce;
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.EventScanner#addRecord(org.epics.pvioc.pv.PVRecord)
         */
        public boolean addRecord(PVRecord pvRecord) {
            lock.lock();
            try {
                ScanField scanField = ScanFieldFactory.create(pvRecord);
                if(scanField==null) {
                    pvRecord.message(
                            "Eventcanner.addRecord invalid scan field",
                            MessageType.fatalError);;
                            return false;
                }
                if(scanField.getScanType()!=ScanType.event) {
                    pvRecord.message(
                            "EventScanner.addRecord scanType is not event",
                            MessageType.fatalError);;
                            return false;
                }
                String eventName = scanField.getEventName();
                if(eventName==null || eventName.length()==0) {
                    pvRecord.message(
                        "EventScanner:addRecord eventName is null",
                        MessageType.fatalError);
                    return false;
                }
                int priority = scanField.getPriority().getJavaPriority();
                String threadName = "event(" + eventName + "," + String.valueOf(priority) + ")";
                Announce announce = getAnnounce(eventName);
                ProcessEvent processEvent = null;
                ProcessEvent[] processEvents = announce.processEvents;
                for(int i=0; i<processEvents.length; i++) {
                    ProcessEvent processEventNow = processEvents[i];
                    int threadPriority = processEventNow.getThread().getPriority();
                    if(priority<threadPriority) continue;
                    if(priority==threadPriority) {
                        processEvent = processEventNow;
                        break;
                    }
                }
                if(processEvent==null) {
                    processEvent = new ProcessEvent(threadName,priority);
                    announce.addProcessEvent(processEvent);
                }
                ProcessRecord processRecord = new ProcessRecord(processEvent.getName(),pvRecord.getRecordProcess());
                if(!processRecord.canProcess()) {
                    return false;
                }     
                processEvent.add(processRecord);
            } finally {
                lock.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.EventScanner#removeEventAnnouncer(org.epics.pvioc.util.EventAnnounce, java.lang.String)
         */
        public void removeEventAnnouncer(EventAnnounce eventAnnounce, String announcer) {
            lock.lock();
            try {
                Announce announce = (Announce)eventAnnounce;
                announce.removeAnnouncer(announcer);
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.EventScanner#removeRecord(org.epics.pvdata.pv.PVRecord, java.lang.String, org.epics.pvdata.misc.ThreadPriority)
         */
        public boolean removeRecord(PVRecord pvRecord, String eventName, ThreadPriority threadPriority) {
            lock.lock();
            try {
                Announce announce = getAnnounce(eventName);
                int priority = threadPriority.getJavaPriority();
                ProcessEvent processEvent = null;
                ProcessEvent[] processEvents = announce.processEvents;
                for(int i=0; i<processEvents.length; i++) {
                    ProcessEvent processEventNow = processEvents[i];
                    int threadPrio = processEventNow.getThread().getPriority();
                    if(priority<threadPrio) continue;
                    if(priority==threadPrio) {
                        processEvent = processEventNow;
                        break;
                    }
                }
                if(processEvent==null || !processEvent.remove(pvRecord)) {
                    pvRecord.message(
                            "EventScanner.removeRecord but not in list",
                            MessageType.error);
                }
            } finally {
                lock.unlock();
            }
            return true;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            StringBuilder builder = new StringBuilder();
            lock.lock();
            try {
                ListIterator<Announce> iter = eventAnnouncerList.listIterator();
                while(iter.hasNext()) {
                    Announce announce = iter.next();
                    builder.append(showAnnounce(announce));
                }
                return builder.toString();
            } finally {
                lock.unlock();
            }
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.EventScanner#show(java.lang.String)
         */
        public String show(String eventName) {
            lock.lock();
            try {
                StringBuilder builder = new StringBuilder();
                ListIterator<Announce> iter = eventAnnouncerList.listIterator();
                while(iter.hasNext()) {
                    Announce announce = iter.next();
                    if(eventName.equals(announce.getEventName())) {
                        builder.append(showAnnounce(announce));
                    }
                }
                return builder.toString();
            } finally {
                lock.unlock();
            }
        }        
        private String showAnnounce(Announce announce) {
            StringBuilder builder = new StringBuilder();
            Thread thread = announce.getThread();
            builder.append(String.format(
                    "thread %s eventName %s priority %d announcers {",
                    thread.getName(),announce.getEventName(),thread.getPriority()));
            String[] announcerList = announce.getAnnouncerList();
            boolean isFirst = true;
            for(String announcer : announcerList) {
                if(isFirst) {
                    isFirst = false;
                } else {
                    builder.append(',');
                }
                builder.append(announcer);
            }
            builder.append("}");
            ProcessEvent[] processEvents = announce.processEvents;
            for(ProcessEvent processEvent : processEvents) {
                if(processEvent==null) continue;
                thread = processEvent.getThread();
                builder.append(String.format(lineBreak + "    thread %s priority %d record list{",
                        thread.getName(),
                        thread.getPriority()));
                ProcessRecordList processRecordList = processEvent;
                ProcessRecord[] recordExecutors = processRecordList.processRecords;
                for(int j=0; j<recordExecutors.length; j++) {
                    ProcessRecord recordExecutor = recordExecutors[j];
                    if(recordExecutor==null) continue;
                    RecordProcess recordProcess = recordExecutor.recordProcess;
                    String name = recordProcess.getRecord().getRecordName();
                    builder.append(lineBreak + "        " + name);
                }
                builder.append(lineBreak + "    }");
            }
            builder.append(lineBreak + "}");
            return builder.toString();
        }
    }
}

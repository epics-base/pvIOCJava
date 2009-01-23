/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS JavaIOC is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */
package org.epics.ioc.swtshell;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.epics.ioc.support.RecordProcess;
import org.epics.ioc.support.RecordProcessRequester;
import org.epics.ioc.support.SupportDatabase;
import org.epics.ioc.support.SupportDatabaseFactory;
import org.epics.ioc.support.SupportState;
import org.epics.ioc.util.EventScanner;
import org.epics.ioc.util.PeriodicScanner;
import org.epics.ioc.util.RequestResult;
import org.epics.ioc.util.ScannerFactory;
import org.epics.pvData.factory.PVDatabaseFactory;
import org.epics.pvData.misc.Executor;
import org.epics.pvData.misc.ExecutorFactory;
import org.epics.pvData.misc.ThreadCreateFactory;
import org.epics.pvData.misc.ThreadPriority;
import org.epics.pvData.property.PVProperty;
import org.epics.pvData.property.PVPropertyFactory;
import org.epics.pvData.property.TimeStamp;
import org.epics.pvData.property.TimeStampFactory;
import org.epics.pvData.pv.MessageType;
import org.epics.pvData.pv.PVDatabase;
import org.epics.pvData.pv.PVField;
import org.epics.pvData.pv.PVRecord;
import org.epics.pvData.pv.PVStructure;
import org.epics.pvData.pv.Requester;

/**
 * A shell for introspecting a JavaIOC Database.
 * The menubar at the top of the display provides access to the DBD (Database Definition Database).
 * The recordName row provides access to record instances. It provides two controls: a select
 * button and a text entry widget.
 * A record name can be entered in the text window followed by the enter key.
 * If the select button is pressed, a list of all record instances is displayed.
 * The user can select a name.
 * @author mrk
 *
 */
public class IntrospectDatabaseFactory {
    private static final Executor executor = ExecutorFactory.create("swtshell:introspectDatabase",ThreadPriority.low);
    private static final PVDatabase masterPVDatabase = PVDatabaseFactory.getMaster();
    private static final SupportDatabase masterSupportDatabase = SupportDatabaseFactory.get(masterPVDatabase);
    private static final PVProperty pvProperty = PVPropertyFactory.getPVProperty(); 
    private static final String newLine = String.format("%n");
    
    /**
     * A shell for introspecting the local IOC database.
     * @param display The display.
     */
    public static void init(Display display) {
        Introspect introspect = new Introspect(display);
        introspect.start();
    }
    
    
    private static class Introspect implements SelectionListener, Requester{
        private Display display;
        private Shell shell;
        private Button selectButton;
        private Text selectText = null;
        private Button dumpButton;
        private Button showStateButton;
        private Button setTraceButton;
        private Button setEnableButton;
        private Button setSupportStateButton;
        private Button releaseProcessorButton;
        private Button timeProcessButton;
        private Text numberTimesText = null;
        private Button showBadRecordsButton;
        private Button showThreadsButton;
        private Button clearButton;
        private Text consoleText;
        private SelectLocalRecord selectLocalRecord;
        
        private PVRecord pvRecord = null;
        
        private Introspect(Display display) {
            this.display = display;
        }
        
        private void start() {
            shell = new Shell(display);
            shell.setText("introspectDatabase");
            GridLayout layout = new GridLayout();
            layout.numColumns = 1;
            shell.setLayout(layout);
            Menu menuBar = new Menu(shell,SWT.BAR);
            shell.setMenuBar(menuBar);
            MenuItem pvdStructureMenu = new MenuItem(menuBar,SWT.CASCADE);
            pvdStructureMenu.setText("structure");
            new StructurePVD(pvdStructureMenu);
            Composite recordComposite = new Composite(shell,SWT.BORDER);
            GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
            recordComposite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 1;
            recordComposite.setLayout(layout);
            
            Composite composite = new Composite(recordComposite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 3;
            composite.setLayout(layout);
            new Label(composite,SWT.NONE).setText("record");
            selectButton = new Button(composite,SWT.PUSH);
            selectButton.setText("select");
            selectLocalRecord = SelectLocalRecordFactory.create(shell,this);
            selectButton.addSelectionListener(this);
            selectText = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 500;
            selectText.setLayoutData(gridData);
            selectText.addSelectionListener(this);
            
            composite = new Composite(recordComposite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 4;
            composite.setLayout(layout);
            dumpButton = new Button(composite,SWT.PUSH);
            dumpButton.setText("dump");
            dumpButton.setEnabled(false);
            dumpButton.addSelectionListener(this);
            showStateButton = new Button(composite,SWT.PUSH);
            showStateButton.setText("showState");
            showStateButton.setEnabled(false);
            showStateButton.addSelectionListener(this);
            setTraceButton = new Button(composite,SWT.PUSH);
            setTraceButton.setText("setTrace");
            setTraceButton.setEnabled(false);
            setTraceButton.addSelectionListener(this);
            setEnableButton = new Button(composite,SWT.PUSH);
            setEnableButton.setText("setEnable");
            setEnableButton.setEnabled(false);
            setEnableButton.addSelectionListener(this);
            
            composite = new Composite(recordComposite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 4;
            composite.setLayout(layout);
            timeProcessButton = new Button(composite,SWT.PUSH);
            timeProcessButton.setText("timeProcess");
            timeProcessButton.addSelectionListener(this);
            new Label(composite,SWT.NONE).setText("numberTimes");
            numberTimesText = new Text(composite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            gridData.minimumWidth = 100;
            numberTimesText.setLayoutData(gridData);
            numberTimesText.setText("100000");
            
            composite = new Composite(recordComposite,SWT.BORDER);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            layout = new GridLayout();
            layout.numColumns = 2;
            composite.setLayout(layout);
            setSupportStateButton = new Button(composite,SWT.PUSH);
            setSupportStateButton.setText("setSupportState");
            setSupportStateButton.setEnabled(false);
            setSupportStateButton.addSelectionListener(this);
            releaseProcessorButton = new Button(composite,SWT.PUSH);
            releaseProcessorButton.setText("releaseProcessor");
            releaseProcessorButton.setEnabled(false);
            releaseProcessorButton.addSelectionListener(this);
            composite = new Composite(shell,SWT.BORDER);
            layout = new GridLayout();
            layout.numColumns = 2;
            composite.setLayout(layout);
            gridData = new GridData(GridData.FILL_HORIZONTAL);
            composite.setLayoutData(gridData);
            showBadRecordsButton = new Button(composite,SWT.PUSH);
            showBadRecordsButton.setText("&showBadRecords");
            showBadRecordsButton.addSelectionListener(this);
            showThreadsButton = new Button(composite,SWT.PUSH);
            showThreadsButton.setText("&showThreads");
            showThreadsButton.addSelectionListener(this);
            composite = new Composite(shell,SWT.BORDER);
            layout = new GridLayout();
            layout.numColumns = 1;
            composite.setLayout(layout);
            gridData = new GridData(GridData.FILL_BOTH);
            composite.setLayoutData(gridData);
            clearButton = new Button(composite,SWT.PUSH);
            clearButton.setText("&Clear");
            clearButton.addSelectionListener(this);
            consoleText = new Text(composite,SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL|SWT.READ_ONLY);
            gridData = new GridData(GridData.FILL_BOTH);
            consoleText.setLayoutData(gridData);
            shell.open();
        }       
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetDefaultSelected(SelectionEvent e) {
            widgetSelected(e);
        }
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        public void widgetSelected(SelectionEvent e) {
            Object object = e.getSource();
            if(object==selectButton) {
                pvRecord = selectLocalRecord.getPVRecord();
                if(pvRecord==null) {
                    consoleText.append("record not found"+ newLine);
                    setButtonsEnabled(false);
                } else {
                    consoleText.append(pvRecord.getPVRecord().getRecordName() + String.format("%n"));
                    setButtonsEnabled(true);
                    selectText.setText(pvRecord.getPVRecord().getRecordName());
                }
                return;
            }
            if(object==selectText) {
                pvRecord = masterPVDatabase.findRecord(selectText.getText());
                if(pvRecord==null) {
                    consoleText.append("record not found"+ newLine);
                    setButtonsEnabled(false);
                } else {
                    setButtonsEnabled(true);
                }
            }
            if(object==dumpButton) {
                consoleText.append(pvRecord.getPVRecord().toString());
                return;
            }
            if(object==timeProcessButton) {
                if(pvRecord==null) {
                    consoleText.append("no record"+ newLine);
                    return;
                }
                int ntimes = Integer.parseInt(numberTimesText.getText());
                if(ntimes<=0) {
                    consoleText.append("invalid numberTimes"+ newLine);
                    return;
                }
                TimeProcess timeProcess = new TimeProcess(pvRecord,ntimes,this);
                consoleText.append(timeProcess.doIt() + newLine);
                return;
            }
            if(object==showStateButton) {
                RecordProcess recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                boolean processSelf = recordProcess.canProcessSelf();
                String processRequesterName = recordProcess.getRecordProcessRequesterName();
                SupportState supportState = recordProcess.getSupportState();
                boolean isActive = recordProcess.isActive();
                boolean isEnabled = recordProcess.isEnabled();
                boolean isTrace = recordProcess.isTrace();
                String alarmSeverity = null;
                PVField pvField = pvProperty.findProperty(pvRecord,"alarm.severity.choice");
                if(pvField!=null) alarmSeverity = pvField.toString();
                String alarmMessage = null;
                pvField = pvProperty.findProperty(pvRecord,"alarm.message");
                if(pvField!=null) alarmMessage = pvField.toString();
                consoleText.append(pvRecord.getPVRecord().getRecordName() + newLine);
                consoleText.append(
                    "  processSelf " + processSelf + " processRequester " + processRequesterName
                    + " supportState " + supportState.name() + newLine);
                consoleText.append(
                    "  isActive " + isActive + " isEnabled " + isEnabled + " isTrace " + isTrace + newLine);
                consoleText.append(
                    "  alarmSeverity " + alarmSeverity + " alarmMessage " + alarmMessage + newLine);
                return;
            }
            if(object==setTraceButton) {
                SetTrace set = new SetTrace(shell);
                set.set();
                return;
            }
            if(object==setEnableButton) {
                SetEnable set = new SetEnable(shell);
                set.set();
                return;
            }
            if(object==setSupportStateButton) {
                SetSupportState set = new SetSupportState(shell);
                set.set();
                return;
            }
            if(object==releaseProcessorButton) {
                RecordProcess recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                if(recordProcess==null) {
                    message("recordProcess is null", MessageType.error);
                    return;
                }
                MessageBox mb = new MessageBox(
                        shell,SWT.ICON_WARNING|SWT.YES|SWT.NO);
                mb.setMessage("VERY DANGEROUS. DO YOU WANT TO PROCEED?");
                int rc = mb.open();
                if(rc==SWT.YES) {
                    recordProcess.releaseRecordProcessRequester();
                }
                return;
            }
            if(object==showBadRecordsButton) {
                PVRecord[] pvRecords = masterPVDatabase.getRecords();
                for(PVRecord pvRecord : pvRecords) {
                    RecordProcess recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                    boolean isActive = recordProcess.isActive();
                    boolean isEnabled = recordProcess.isEnabled();
                    SupportState supportState = recordProcess.getSupportState();
                    String alarmSeverity = null;
                    PVField pvField = pvProperty.findProperty(pvRecord,"alarm.severity.choice");
                    if(pvField!=null) alarmSeverity = pvField.toString();
                    String alarmMessage = null;
                    pvField = pvProperty.findProperty(pvRecord,"alarm.message");
                    if(pvField!=null) alarmMessage = pvField.toString();
                    String status = "";
                    if(isActive) status += " isActive";
                    if(!isEnabled) status += " disabled";
                    if(supportState!=SupportState.ready) status += " supportState " + supportState.name();
                    if(alarmSeverity!=null && !alarmSeverity.equals("none")) status += " alarmSeverity " + alarmSeverity;
                    if(alarmMessage!=null && !alarmMessage.equals("null") && alarmMessage.length()>0)
                        status += " alarmMessage " + alarmMessage;
                    if(status!="") {
                        status = pvRecord.getRecordName() + " " + status + newLine;
                        consoleText.append(status);
                    }
                }
                return;
            }
            if(object==showThreadsButton) {
                Thread[] threads = ThreadCreateFactory.getThreadCreate().getThreads();
                for(Thread thread : threads) {
                    String name = thread.getName();
                    int priority = thread.getPriority();
                    consoleText.append(name + " priority " + priority + newLine);
                }
                PeriodicScanner periodicScanner = ScannerFactory.getPeriodicScanner();
                EventScanner eventScanner = ScannerFactory.getEventScanner();
                consoleText.append(periodicScanner.toString());
                consoleText.append(eventScanner.toString());
                return;
            }
            if(object==clearButton) {
                consoleText.selectAll();
                consoleText.clearSelection();
                consoleText.setText("");
                return;
            }
            
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return "introspectDatabase";
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            consoleText.setText(message);
        }
        
        private void setButtonsEnabled(boolean value) {
            dumpButton.setEnabled(value);
            showStateButton.setEnabled(value);
            setTraceButton.setEnabled(value);
            setEnableButton.setEnabled(value);
            timeProcessButton.setEnabled(value);
            setSupportStateButton.setEnabled(value);
            releaseProcessorButton.setEnabled(value);
        }
        
        private class SetEnable extends Dialog  implements SelectionListener {
            private Shell shell;
            private Button falseButton;
            private Button trueButton;
            RecordProcess recordProcess;

            private SetEnable(Shell parent) {
                super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
            }
            
            private void set() {
                recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                boolean initialState = recordProcess.isEnabled();
                shell = new Shell(getParent(),getStyle());
                shell.setText("setEnable");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                falseButton = new Button(composite,SWT.RADIO);
                falseButton.setText("disable");
                falseButton.setSelection(initialState ? false : true);
                trueButton = new Button(composite,SWT.RADIO);
                trueButton.setText("enable");
                trueButton.setSelection(initialState ? true : false);
                falseButton.addSelectionListener(this);
                trueButton.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();

                if(object==falseButton) {
                    recordProcess.setEnabled(false);
                }
                if(object==trueButton) {
                    recordProcess.setEnabled(true);
                }
            }
            
        }
        private class SetTrace extends Dialog  implements SelectionListener {
            private Shell shell;
            private Button falseButton;
            private Button trueButton;
            RecordProcess recordProcess;

            private SetTrace(Shell parent) {
                super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
            }
            
            private void set() {
                recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                boolean initialState = recordProcess.isTrace();
                shell = new Shell(getParent(),getStyle());
                shell.setText("setEnable");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                falseButton = new Button(composite,SWT.RADIO);
                falseButton.setText("traceOff");
                falseButton.setSelection(initialState ? false : true);
                trueButton = new Button(composite,SWT.RADIO);
                trueButton.setText("traceOn");
                trueButton.setSelection(initialState ? true : false);
                falseButton.addSelectionListener(this);
                trueButton.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();

                if(object==falseButton) {
                    recordProcess.setTrace(false);
                }
                if(object==trueButton) {
                    recordProcess.setTrace(true);
                }
            }
            
        }
        
        private class SetSupportState extends Dialog  implements SelectionListener {
            private Shell shell;
            private Button initializeButton;
            private Button startButton;
            private Button stopButton;
            private Button uninitializeButton;
            RecordProcess recordProcess;

            private SetSupportState(Shell parent) {
                super(parent,SWT.PRIMARY_MODAL|SWT.DIALOG_TRIM);
            }
            
            private void set() {
                recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                shell = new Shell(getParent(),getStyle());
                shell.setText("setEnable");
                GridLayout gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                shell.setLayout(gridLayout);
                Composite composite = new Composite(shell,SWT.BORDER);
                gridLayout = new GridLayout();
                gridLayout.numColumns = 1;
                composite.setLayout(gridLayout);
                initializeButton = new Button(composite,SWT.PUSH);
                initializeButton.setText("initialize");
                startButton = new Button(composite,SWT.PUSH);
                startButton.setText("start");
                stopButton = new Button(composite,SWT.PUSH);
                stopButton.setText("stop");
                uninitializeButton = new Button(composite,SWT.PUSH);
                uninitializeButton.setText("uninitialize");
                initializeButton.addSelectionListener(this);
                startButton.addSelectionListener(this);
                stopButton.addSelectionListener(this);
                uninitializeButton.addSelectionListener(this);
                shell.pack();
                shell.open();
                Display display = getParent().getDisplay();
                while(!shell.isDisposed()) {
                    if(!display.readAndDispatch()) {
                        display.sleep();
                    }
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                Object object = e.getSource();

                if(object==initializeButton) {
                    recordProcess.initialize();
                }
                if(object==startButton) {
                    recordProcess.start();
                }
                if(object==stopButton) {
                    recordProcess.stop();
                }
                if(object==uninitializeButton) {
                    recordProcess.uninitialize();
                }
            }
            
        }
        private class StructurePVD implements SelectionListener {
            
            private StructurePVD(MenuItem menuItem) {
                Menu menuStructure = new Menu(shell,SWT.DROP_DOWN);
                menuItem.setMenu(menuStructure);
                MenuItem choiceAll = new MenuItem(menuStructure,SWT.DEFAULT|SWT.PUSH);
                choiceAll.setText("all");
                choiceAll.addSelectionListener(this);
                PVStructure[] pvdStructures = masterPVDatabase.getStructures();
                for(PVStructure pvdStructure : pvdStructures) {
                    String name = pvdStructure.getFullName();
                    MenuItem choiceItem = new MenuItem(menuStructure,SWT.PUSH);
                    choiceItem.setText(name);
                    choiceItem.addSelectionListener(this);
                }
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetDefaultSelected(SelectionEvent arg0) {
                widgetSelected(arg0);
            }
            /* (non-Javadoc)
             * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent arg0) {
                MenuItem choice = (MenuItem)arg0.getSource();
                String name = choice.getText();
                if(!name.equals("all")) {
                    PVStructure value = masterPVDatabase.findStructure(name);
                    consoleText.append(value.toString());
                    return;
                }
                PVStructure[] pvdStructures = masterPVDatabase.getStructures();
                for(PVStructure pvdStructure : pvdStructures) {
                    consoleText.append(pvdStructure.toString());
                }
            }
            
        }
        
        private class TimeProcess implements Runnable,RecordProcessRequester
        {   
            private Requester requester;
            private PVRecord pvRecord;
            int ntimes = 100000;
            private RecordProcess recordProcess = null;
            private ReentrantLock lock = new ReentrantLock();
            private Condition waitAllDone = lock.newCondition();
            private boolean allDone = false;
            private Condition waitProcessDone = lock.newCondition();
            private boolean processDone = false;
            private String result = null;

            private TimeProcess(PVRecord pvRecord,int ntimes,Requester requester) {
                this.requester = requester;
                this.pvRecord = pvRecord;
                this.ntimes = ntimes;
            }
            
            String doIt() {
                recordProcess = masterSupportDatabase.getRecordSupport(pvRecord).getRecordProcess();
                if(!recordProcess.setRecordProcessRequester(this)) return "could not process the record";
                allDone = false;
                executor.execute(this);
                lock.lock();
                try {
                    while(!allDone) {
                        try {
                        waitAllDone.await();
                        } catch(InterruptedException e) {}
                    }
                }finally {
                    lock.unlock();
                }
                recordProcess.releaseRecordProcessRequester(this);
                return result;
            }
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
                TimeStamp timeStamp = TimeStampFactory.create(0,0);
                long start = System.currentTimeMillis();
                timeStamp.put(start);
               for(int i=0; i<ntimes; i++) {
                   processDone = false;
                   recordProcess.process(this, false, timeStamp);
                   lock.lock();
                   try {
                       while(!processDone) {
                           try {
                           waitProcessDone.await();
                           } catch(InterruptedException e) {}
                       }
                   }finally {
                       lock.unlock();
                   }
               }
               long end = System.currentTimeMillis();
               double diff = end - start;
               diff = diff/1000.0;
               result = "seconds/record=" + diff/ntimes + " records/second=" + ((double)ntimes)/diff;
               lock.lock();
               try {
                   allDone = true;
                   waitAllDone.signal();
               } finally {
                   lock.unlock();
               }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#getRequesterName()
             */
            public String getRequesterName() {
                return requester.getRequesterName();
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.util.Requester#message(java.lang.String, org.epics.ioc.util.MessageType)
             */
            public void message(final String message, final MessageType messageType) {
                requester.message(message, MessageType.info);
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.RecordProcessRequester#recordProcessComplete()
             */
            public void recordProcessComplete() {
                lock.lock();
                try {
                    processDone = true;
                    waitProcessDone.signal();
                } finally {
                    lock.unlock();
                }
            }
            /* (non-Javadoc)
             * @see org.epics.ioc.support.RecordProcessRequester#recordProcessResult(org.epics.ioc.util.RequestResult)
             */
            public void recordProcessResult(RequestResult requestResult) {
                // nothing to do
            }
        }
    }
}

/*
 * Copyright (c) 2009 by Cosylab
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file "LICENSE-CAJ". If the license is not included visit Cosylab web site,
 * <http://www.cosylab.com>.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package org.epics.ioc.caV4;

import java.util.Timer;
import java.util.TimerTask;

import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.AfterStartFactory;
import org.epics.ioc.install.AfterStartNode;
import org.epics.ioc.install.AfterStartRequester;
import org.epics.ioc.install.NewAfterStartRequester;
import org.epics.pvData.misc.ThreadPriority;

public class ClientFactory {
    static private boolean isRegistered = false; 
    /**
     * This initializes the Channel Access client and invokes AfterStart to wait for 2 seconds after database initialization.
     */
    public static synchronized void start() {
        if(isRegistered) return;
        isRegistered = true;
        AfterStartDelay afterStartDelay = new AfterStartDelay();
        afterStartDelay.start();
        org.epics.ca.ClientFactory.start();
    }
    
    // afterStartDelay ensures that no run method gets called until after 2 seconds after
    // the last record has started. This allows time to connect to servers.
    private static class AfterStartDelay extends TimerTask  implements NewAfterStartRequester,AfterStartRequester {
        private static final Timer timer = new Timer("caClientDelay");
        private AfterStartNode afterStartNode = null;
        private AfterStart afterStart = null;

        private AfterStartDelay() {}

        private void start() {
            afterStartNode = AfterStartFactory.allocNode(this);
            AfterStartFactory.newAfterStartRegister(this);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.install.NewAfterStartRequester#callback(org.epics.ioc.install.AfterStart)
         */
        public void callback(AfterStart afterStart) {
            this.afterStart = afterStart;
            afterStart.requestCallback(afterStartNode, false, ThreadPriority.middle);
        }
        /* (non-Javadoc)
         * @see org.epics.ioc.install.AfterStartRequester#callback(org.epics.ioc.install.AfterStartNode)
         */
        public void callback(AfterStartNode node) {
            timer.schedule(this, 2000);
        }
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {
            afterStart.done(afterStartNode);
            afterStart = null;
        }

    }
}
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

import org.epics.ca.CAConstants;
import org.epics.ca.CAException;
import org.epics.ca.client.Channel;
import org.epics.ca.client.ConnectionEvent;
import org.epics.ca.client.EventListener;
import org.epics.ca.core.impl.client.ClientContextImpl;
import org.epics.ioc.channelAccess.ChannelAccessFactory;
import org.epics.ioc.install.AfterStart;
import org.epics.ioc.install.AfterStartFactory;
import org.epics.ioc.install.AfterStartNode;
import org.epics.ioc.install.AfterStartRequester;
import org.epics.ioc.install.NewAfterStartRequester;
import org.epics.pvData.channelAccess.ChannelFind;
import org.epics.pvData.channelAccess.ChannelFindRequester;
import org.epics.pvData.channelAccess.ChannelProvider;
import org.epics.pvData.channelAccess.ChannelRequester;
import org.epics.pvData.misc.ThreadPriority;

public class ClientFactory {
    static private boolean isRegistered = false; 
    /**
     * This initializes the Channel Access client.
     */
    public static void start() {
        if(isRegistered) return;
        isRegistered = true;
        AfterStartDelay afterStartDelay = new AfterStartDelay();
        afterStartDelay.start();
        try {
            ChannelAccessFactory.registerChannelProvider(new ChannelProviderImpl());
        } catch (CAException e) {
            throw new RuntimeException("Failed to initializa client channel access.", e);
        }
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
	
	private static class ChannelProviderImpl implements ChannelProvider
	{
	    static private final String providerName = "caV4";
		private final ClientContextImpl context;
		
		
		public ChannelProviderImpl() throws CAException
		{
			context = new ClientContextImpl();
			context.initialize();
		}		
		@Override
		public ChannelFind channelFind(String channelName,
				ChannelFindRequester channelFindRequester)
		{
			// TODO there is no actual support for find right now in CA... can be done via create
			throw new RuntimeException("not implemented");
		}
		
        @Override
		public void createChannel(String channelName,
				final ChannelRequester channelRequester) {

			EventListener<ConnectionEvent> cl = new EventListener<ConnectionEvent>()
		    {
		 		public void onEvent(ConnectionEvent connectionEvent) {
		 			channelRequester.channelStateChange(connectionEvent.getChannel(), connectionEvent.isConnected());
				}
		    };

		    Channel channel;
			try {
				channel = context.createChannel(channelName, cl, CAConstants.CA_DEFAULT_PRIORITY);
			} catch (Throwable th) {
				// TODO error handling missing in IF
				th.printStackTrace();
				channelRequester.channelNotCreated();
				return;
			}
		    channelRequester.channelCreated(channel);
		}
		@Override
		public void destroy() {
			context.dispose();
		}
		@Override
		public String getProviderName() {
			return providerName;
		}
		
	}
}
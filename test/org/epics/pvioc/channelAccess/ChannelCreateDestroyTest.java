/**
 * Copyright - See the COPYRIGHT that is included with this distribution.
 * EPICS pvData is distributed subject to a Software License Agreement found
 * in file LICENSE that is included with this distribution.
 */

package org.epics.pvioc.channelAccess;

import junit.framework.TestCase;

import org.epics.pvaccess.client.Channel;
import org.epics.pvaccess.client.Channel.ConnectionState;
import org.epics.pvaccess.client.ChannelProvider;
import org.epics.pvaccess.client.ChannelProviderRegistryFactory;
import org.epics.pvaccess.client.ChannelRequester;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.Requester;
import org.epics.pvdata.pv.Status;
import org.epics.pvioc.install.Install;
import org.epics.pvioc.install.InstallFactory;

/**
 * @author <a href="mailto:matej.sekoranjaATcosylab.com">Matej Sekoranja</a>
 * @version $Id$
 */
public class ChannelCreateDestroyTest extends TestCase {
	
	private static class ChannelRequesterImpl implements ChannelRequester {
		
		Channel channel;
		
		@Override
		public void message(String message, MessageType messageType) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public String getRequesterName() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void channelStateChange(org.epics.pvaccess.client.Channel c,
				ConnectionState isConnected) {
			// TODO Auto-generated method stub
		}
				
		@Override
		public synchronized void channelCreated(Status status, org.epics.pvaccess.client.Channel channel) {
			this.channel = channel;
			this.notifyAll();
		}
	};
	
	final static long TIMEOUT_MS = 3000;
	
	private Channel syncCreateChannel(String name)
	{
		ChannelRequesterImpl cr = new ChannelRequesterImpl();
		synchronized (cr) {
			provider.createChannel(name, cr, ChannelProvider.PRIORITY_DEFAULT);
			if (cr.channel == null)
			{
				try {
					cr.wait(TIMEOUT_MS);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			assertNotNull("failed to create channel", cr.channel);
			return cr.channel;
		}
	}

	private ChannelProvider provider;

    private static class Listener implements Requester {
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#getRequesterName()
         */
        public String getRequesterName() {
            return ChannelCreateDestroyTest.class.getName();
        }
        /* (non-Javadoc)
         * @see org.epics.pvioc.util.Requester#message(java.lang.String, org.epics.pvioc.util.MessageType)
         */
        public void message(String message, MessageType messageType) {
            System.out.println(message);
            
        }
    }

    private static final Install install = InstallFactory.get();
    private static final Requester iocRequester = new Listener(); 
    static
	{
		// start javaIOC
        try {
            install.installStructures("xml/structures.xml",iocRequester);
            install.installRecords("example/exampleDB.xml",iocRequester);
        }  catch (IllegalStateException e) {
            System.out.println("IllegalStateException: " + e);
        }
		
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		provider = ChannelProviderRegistryFactory.getChannelProviderRegistry().getProvider("local");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
            install.cleanMaster(iocRequester);
	}

	/**
	 * Here is no memory leak...
	 */
	/*
	public void testAllocation()
	{
		final int COUNT = 1000000;
		for (int i = 0; i <= COUNT; i++)
		{
			ByteBuffer bb = ByteBuffer.allocate(16*1024);
			if (bb.isDirect())	// do something with it...
				System.out.println("is direct");
			if ((i % 1000)==0) 
			{
				System.gc();
				System.out.println(i+" : used by VM " +Runtime.getRuntime().totalMemory() + ", free:" + Runtime.getRuntime().freeMemory());
			}
		}
	}
	*/

	/**
	 * But here it is!!!
	 */
	public void testConnectDisconnect()
	{
		final int COUNT = 1000000;
		for (int i = 0; i <= COUNT; i++)
		{
			Channel channel = syncCreateChannel("valueOnly");
			channel.destroy();
			if ((i % 100000)==0) 
			{
				System.gc();
				System.out.println(i+" : used by VM " +Runtime.getRuntime().totalMemory() + ", free:" + Runtime.getRuntime().freeMemory());
			}
		}
	}
}

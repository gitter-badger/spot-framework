package io.spotnext.core.management.service;

import java.net.InetAddress;
import java.net.SocketException;

import io.spotnext.core.management.exception.RemoteServiceInitException;

/**
 * This is the basis for any remote interface service, such as HTTP or SSH
 * service.
 *
 */
public interface RemoteInterfaceServiceEndpoint {

	/**
	 * Initializes the remote service.
	 * 
	 * @throws RemoteServiceInitException
	 * @throws SocketException
	 */
	void init() throws RemoteServiceInitException;

	/**
	 * Is called when a shutdown signal is received (eg. from spring).
	 * 
	 * @throws RemoteServiceInitException
	 */
	void shutdown() throws RemoteServiceInitException;

	/**
	 * Gets the port that is being used for this service.
	 */
	int getPort();

	/**
	 * Gets the address on which the service is listening.
	 * 
	 */
	InetAddress getBindAddress();
}

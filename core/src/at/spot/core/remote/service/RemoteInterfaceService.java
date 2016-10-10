package at.spot.core.remote.service;

import java.net.SocketException;

import at.spot.core.remote.eception.RemoteServiceInitException;

/**
 * This is the basis for any remote interface service, such as HTTP or SSH
 * service.
 *
 */
public interface RemoteInterfaceService {

	/**
	 * Initializes the remote service.
	 * 
	 * @throws RemoteServiceInitException
	 * @throws SocketException
	 */
	public void init() throws RemoteServiceInitException, SocketException;
}
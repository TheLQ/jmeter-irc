/**
 * Copyright (C) 2011 Leon Blakey <lord.quackstar at gmail.com>
 *
 * This file is part of JMeter-IRC.
 *
 * JMeter-IRC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JMeter-IRC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PircBotX.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.apache.jmeter.protocol.irc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

/**
 *
 * @author lordquackstar
 */
public class IrcServer {
	protected int port;
	protected ServerSocket server;
	protected Set<Client> clients = Collections.synchronizedSet(new HashSet());
	protected Set<IrcBotSampler> listeners = Collections.synchronizedSet(new HashSet());
	
	public IrcServer() {
	}
	
	public void init() throws IOException {
		server = new ServerSocket(port);
		while (true) {
			final Client curClient = new Client(server.accept());
			clients.add(curClient);
			//Handle client input in new thread
			new Thread() {
				@Override
				public void run() {
					handleClientInput(curClient);
				}
			};
		}
	}
	
	public void handleClientInput(Client client) {
		try {
			String inputLine = "";
			while ((inputLine = client.getIn().readLine()) != null)
				//Dispatch to listeners
				for (IrcBotSampler listener : listeners)
					listener.acceptLine(inputLine);

			//Client has disconnected, forget about
			client.getIn().close();
			client.getOut().close();
			clients.remove(client);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void close() throws IOException {
		//Close down all of the clients
		for(Client curClient : clients) {
			curClient.getIn().close();
			curClient.getOut().close();
		}
	}
	
	@Data
	protected class Client {
		protected Socket socket;
		protected BufferedReader in;
		protected BufferedWriter out;
		
		public Client(Socket socket) throws IOException {
			this.socket = socket;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		}
	}
}

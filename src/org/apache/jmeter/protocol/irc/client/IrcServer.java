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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PircBotX. If not, see <http://www.gnu.org/licenses/>.
 */
package org.apache.jmeter.protocol.irc.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import lombok.Data;
import lombok.Getter;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 *
 * @author lordquackstar
 */
public class IrcServer {
	private static final Logger log = LoggingManager.getLoggerForClass();
	protected int port;
	protected ServerSocket server;
	protected Client client;
	protected final ConcurrentSkipListSet<IrcBotSampler> samplers = new ConcurrentSkipListSet<IrcBotSampler>();
	protected final String serverAddress = "irc.jmeter";
	@Getter
	protected boolean closedGood = false;
	protected IrcBotGui gui;

	public IrcServer(int port, IrcBotGui gui) {
		this.port = port;
		this.gui = gui;
	}

	public void init() throws IOException {
		server = new ServerSocket(port);
		log.info("Server created on port " + port);
		while (true) {
			log.info("Waiting for clients");
			Socket newSocket = server.accept();
			if (client != null) {
				//Only can handle one client
				log.warn("Disconnecting new client, a current client is already connected");
				newSocket.close();
			}
			client = new Client(newSocket);
			client.log("New client connection accepted");
			gui.updateClientConnected(true);
			//Handle client input in new thread
			new Thread() {
				@Override
				public void run() {
					handleClientInput();
				}
			}.start();

		}
	}

	public void handleClientInput() {
		try {
			String inputLine = "";
			try {
				//Temporarily set timeout to 5 seconds
				client.getSocket().setSoTimeout(5000);
				client.log("Waiting for initial Nick line");
				//Wait for initial NICK line
				while ((inputLine = client.getIn().readLine()) != null)
					if (inputLine.toUpperCase().trim().startsWith("NICK ")) {
						client.setInitNick(inputLine.split(" ", 2)[1]);
						break;
					}
			} catch (SocketTimeoutException e) {
				//Client hasn't responded, close the connection
				client.log("Timed out sending Join. Disconnecting...");
				forgetClient(client);
			}

			client.log("Nick recieved, continuing");

			//Resume normal timeout
			client.getSocket().setSoTimeout(0);

			client.log("Sending that client has connected");
			//Write line saying client has connected to this IRC server
			client.getOut().write(":" + serverAddress + " 004 " + serverAddress + " jmeter-ircd-basic-0.1 ov b\r\n");
			client.getOut().flush();

			client.log("Awaiting input from user");
			//Read input from user
			Input:
			while ((inputLine = client.getIn().readLine()) != null) {
				//See if there are any wait requests on this
				for (IrcBotSampler curSampler : samplers)
					if (curSampler.parseLine(inputLine))
						continue Input;
				if (inputLine.toUpperCase().trim().startsWith("JOIN "))
					sendToClient(":" + client.getInitNick() + "!~client@clients.jmeter JOIN :" + inputLine.split(" ", 2)[1]);
				else
					log.warn("Client # " + client.getClientNum() + "Line not matched - " + inputLine);
			}

			//Client has disconnected, forget about
			client.log("Client has disconnected, ending");
		} catch (IOException ex) {
			log.error("Client #" + client.getClientNum() + " raised exception during input. Forgetting about client now...", ex);
		} finally {
			forgetClient(client);
			gui.updateClientConnected(false);
		}
	}

	public void addSampler(IrcBotSampler sampler) {
		samplers.add(sampler);
	}

	public void forgetClient(Client client) {
		client.log("Forgetting about client #" + client.getClientNum());
		try {
			client.getIn().close();
			client.getOut().close();
		} catch (IOException ex) {
			log.error("Client #" + client.getClientNum() + " raised exception when disconnecting", ex);
		} finally {
			client = null;
		}
	}

	public void clearSamplers() {
		samplers.clear();
	}

	public void close() throws IOException {
		closedGood = true;
		//Close down all of the clients
		forgetClient(client);
		if (server.isBound())
			server.close();
	}

	public void sendToClient(String line) throws IOException {
		synchronized (client.getOut()) {
			client.getOut().write(line + "\r\n");
			client.getOut().flush();
		}
	}

	public int getPort() {
		return port;
	}

	public Client getClient() {
		return client;
	}

	@Data
	protected static class Client implements Comparable<Client> {
		protected Socket socket;
		protected BufferedReader in;
		protected BufferedWriter out;
		protected String initNick;
		protected static int totalClients = 0;
		protected int clientNum;

		public Client(Socket socket) throws IOException {
			this.socket = socket;
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			totalClients++;
			clientNum = totalClients;
		}

		public void log(String line) {
			log.debug(clientNum + ": " + line);
		}

		@Override
		public int compareTo(Client o) {
			return getClientNum() - o.getClientNum();
		}
	}
}

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.jmeter.protocol.irc.IrcServer.WaitRequest;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 *
 * @author lordquackstar
 */
public class IrcBotSampler extends AbstractSampler {
	private static final Logger log = LoggingManager.getLoggerForClass();
	public static final String botPrefix = "IrcBotSampler.botPrefix";
	public static final String channelPrefix = "IrcBotSampler.channelPrefix";
	public static final String numChannels = "IrcBotSampler.numChannels";
	public static final String command = "IrcBotSampler.command";
	public static final String targetNick = "IrcBotSampler.targetNick";
	public static final String channelCommand = "IrcBotSampler.channelCommand";
	public static final String PMCommand = "IrcBotSampler.PMCommand";
	public static final String channelMessage = "IrcBotSampler.channelMessage";
	public static final String channelAction = "IrcBotSampler.channelAction";
	public static final String channelNotice = "IrcBotSampler.channelNotice";
	public static final String PMMessage = "IrcBotSampler.PMMessage";
	public static final String PMAction = "IrcBotSampler.PMAction";
	public static final String operatorOp = "IrcBotSampler.operatorOp";
	public static final String operatorVoice = "IrcBotSampler.operatorVoice";
	public static final String operatorKick = "IrcBotSampler.operatorKick";
	public static final String operatorBan = "IrcBotSampler.operatorBan";
	public static final String userPart = "IrcBotSampler.userPart";
	public static final String userQuit = "IrcBotSampler.userQuit";
	private static int classCount = 0; // keep track of classes created
	protected IrcServer server;
	protected int botNumber;
	protected int lastItem = -1;
	protected static Random channelRandom = new Random();
	protected LinkedList<String> responseItems = new LinkedList<String>();
	protected LinkedList<String> responseTypes = new LinkedList<String>();

	public IrcBotSampler() {
		this.server = IrcBotGui.getServer();
		botNumber = classCount++;
	}

	public void init() {
		//Setup possible response list
		Map<String, Set<String>> responseMap = new HashMap();
		if (getPropertyAsBoolean(channelCommand))
			responseMap.put(channelCommand, generateSet(":${thisHostmask} PRIVMSG ${channel} :${command} ${random}"));
		if (getPropertyAsBoolean(PMCommand))
			responseMap.put(PMCommand, generateSet(":${thisHostmask} PRIVMSG ${targetNick} :${command} ${random}"));
		if (getPropertyAsBoolean(channelMessage))
			responseMap.put(channelMessage, generateSet(":${thisHostmask} PRIVMSG ${channel} :${random}"));
		if (getPropertyAsBoolean(channelAction))
			responseMap.put(channelAction, generateSet(":${thisHostmask} PRIVMSG ${channel} :\u0001ACTION ${random}\u0001"));
		if (getPropertyAsBoolean(channelNotice))
			responseMap.put(channelNotice, generateSet(":${thisHostmask} NOTICE ${channel} :${random}"));
		if (getPropertyAsBoolean(PMMessage))
			responseMap.put(PMMessage, generateSet(":${thisHostmask} PRIVMSG ${targetNick} :${random}"));
		if (getPropertyAsBoolean(PMAction))
			responseMap.put(PMAction, generateSet(":${thisHostmask} PRIVMSG ${targetNick} :\u0001ACTION ${random}\u0001"));
		if (getPropertyAsBoolean(operatorOp))
			responseMap.put(operatorOp, generateSet(":${thisHostmask} MODE ${channel} +o ${thisNick}", ":${thisHostmask} MODE ${channel} -o ${thisNick}"));
		if (getPropertyAsBoolean(operatorVoice))
			responseMap.put(operatorVoice, generateSet(":${thisHostmask} MODE ${channel} +v ${thisNick}", ":${thisHostmask} MODE ${channel} -v ${thisNick}"));
		if (getPropertyAsBoolean(operatorKick))
			responseMap.put(operatorKick, generateSet(":${thisHostmask} KICK ${channel} ${targetNick}: ${random}", ":${thisHostmask} JOIN :${channel}"));
		if (getPropertyAsBoolean(operatorBan))
			responseMap.put(operatorBan, generateSet(":${thisHostmask} MODE ${channel} +b ${thisNick}!*@*", ":${thisHostmask} MODE ${channel} -b ${thisNick}!*@*"));
		if (getPropertyAsBoolean(userPart))
			responseMap.put(userPart, generateSet(":${thisHostmask} PART ${channel}", ":${thisHostmask} JOIN :${channel}"));
		if (getPropertyAsBoolean(userQuit))
			responseMap.put(userQuit, generateSet(":${thisHostmask} QUIT :${random}", ":${thisHostmask} JOIN :${channel}"));

		//Randomly shuffle responses and compact response to a single response queue
		List<String> randomKeys = new ArrayList(responseMap.keySet());
		Collections.shuffle(randomKeys);
		for (String curKey : randomKeys)
			for (String curResponse : responseMap.get(curKey)) {
				responseTypes.add(curKey);
				responseItems.add(curResponse);
			}
	}

	@Override
	public SampleResult sample(Entry e) {
		SampleResult res = new SampleResult();
		res.setSuccessful(false); // Assume failure
		res.setSampleLabel(getName());

		try {
			if(responseItems.isEmpty()) {
				log.debug("Generating response items for IRC Sampler #" + botNumber);
				init();
				lastItem = -1;
			}
			
			//Make sure the server is setup
			if (server == null) {
				res.setResponseCode("400");
				res.setResponseMessage("Built In IRC server not started");
				res.setDataType(SampleResult.TEXT);
				return res;
			}

			//Make sure there are clients to talk to
			if (server.getClients().isEmpty()) {
				res.setResponseCode("404");
				res.setResponseMessage("No clients to talk to!");
				res.setDataType(SampleResult.TEXT);
				return res;
			}

			//Make sure the nick is set
			if (getPropertyAsString(botPrefix) == null) {
				res.setResponseCode("404");
				res.setResponseMessage("Target nick not set!");
				res.setDataType(SampleResult.TEXT);
				return res;
			}

			//Reset last item if nessesary
			if (lastItem + 1 >= responseItems.size())
				lastItem = -1;

			//Get next item in the list
			String lineItem = responseItems.get(lastItem + 1);
			String lineType = responseTypes.get(lastItem + 1);
			lastItem++;

			String uuid = UUID.randomUUID().toString();

			//Build the line to send
			String thisNickLine = getPropertyAsString(botPrefix) + botNumber;
			String thisHostmaskLine = thisNickLine + "!~jmeter@bots.jmeter";
			String channelLine = getPropertyAsString(channelPrefix) + channelRandom.nextInt(getPropertyAsInt(numChannels) + 1);
			String targetNickLine = getPropertyAsString(targetNick);
			String commandLine = getPropertyAsString(command);

			String line = lineItem;
			line = line.replace("${thisNick}", thisNickLine);
			line = line.replace("${thisHostmask}", thisHostmaskLine);
			line = line.replace("${channel}", channelLine);
			line = line.replace("${targetNick}", targetNickLine);
			line = line.replace("${random}", uuid);
			line = line.replace("${command}", commandLine);

			String requestData = "Unprocessed Line - " + lineItem + "\n\r"
					+ "${thisNick} - " + thisNickLine + "\n\r"
					+ "${thisHostmask} - " + thisHostmaskLine + "\n\r"
					+ "${channel} - " + channelLine + "\n\r"
					+ "${targetNick} - " + targetNickLine + "\n\r"
					+ "${random} - " + uuid + "\n\r"
					+ "${command} - " + commandLine + "\n\r"
					+ "Processed Line - " + line;
			res.setSamplerData(requestData);

			/*
			 * Perform the sampling
			 */
			res.sampleStart(); // Start timing
			WaitRequest request = server.waitFor(thisNickLine, uuid);
			server.sendToClients(line);
			request.getLatch().await();
			res.sampleEnd(); // End timimg

			/*
			 * Set up the sample result details
			 */
			res.setResponseData(request.getLine(), null);
			res.setDataType(SampleResult.TEXT);

			res.setResponseCodeOK();
			res.setSuccessful(true);
		} catch (Exception ex) {
			log.debug("", ex);
			res.setResponseCode("500");
			res.setResponseMessage(ex.toString());
			res.setResponseData("ERROR IN SAMPLING: " + ExceptionUtils.getFullStackTrace(ex), null);
			res.setDataType(SampleResult.TEXT);
		}
		return res;
	}

	protected Set<String> generateSet(String... responses) {
		Set<String> responseSet = new HashSet();
		responseSet.addAll(Arrays.asList(responses));
		return responseSet;
	}

	/*
	 * Helper method
	 */
	private void trace(String s) {
		String tl = getName();
		String tn = Thread.currentThread().getName();
		String th = this.toString();
		log.debug(tn + " (" + classCount + ") " + tl + " " + s + " " + th);
	}
}

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
import java.util.concurrent.CountDownLatch;
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
	protected int lastItem = 0;
	protected static Random channelRandom = new Random();
	protected String waitFor;
	protected CountDownLatch waitLatch;
	protected String response;
	LinkedList<String> responseItems;
	LinkedList<String> responseTypes;

	public IrcBotSampler() {
		this.server = IrcBotGui.getServer();
		botNumber = classCount++;
		trace("ExampleSampler()");
	}

	public void init() {
		//Setup possible response list
		Map<String, Set<String>> responseMap = new HashMap();
		if (!getPropertyAsBoolean(channelCommand))
			responseMap.put(channelCommand, generateSet("${thisHostmask} PRIVMSG ${channel} :${command} ${random}"));
		if (!getPropertyAsBoolean(PMCommand))
			responseMap.put(PMCommand, generateSet("${thisHostmask} PRIVMSG ${targetNick} :${command} ${random}"));
		if (!getPropertyAsBoolean(channelMessage))
			responseMap.put(channelMessage, generateSet("${thisHostmask} PRIVMSG ${channel} :${random}"));
		if (!getPropertyAsBoolean(channelAction))
			responseMap.put(channelAction, generateSet("${thisHostmask} PRIVMSG ${channel} :\u0001ACTION ${random}\u0001"));
		if (!getPropertyAsBoolean(channelNotice))
			responseMap.put(channelNotice, generateSet("${thisHostmask} NOTICE ${channel} :${random}"));
		if (!getPropertyAsBoolean(PMMessage))
			responseMap.put(PMMessage, generateSet("${thisHostmask} PRIVMSG ${targetNick} :${random}"));
		if (!getPropertyAsBoolean(PMAction))
			responseMap.put(PMAction, generateSet("${thisHostmask} PRIVMSG ${targetNick} :\u0001ACTION ${random}\u0001"));
		if (!getPropertyAsBoolean(operatorOp))
			responseMap.put(operatorOp, generateSet("${thisHostmask} MODE ${channel} +o ${thisNick}", "${thisHostmask} MODE ${channel} -o ${thisNick}"));
		if (!getPropertyAsBoolean(operatorVoice))
			responseMap.put(operatorVoice, generateSet("${thisHostmask} MODE ${channel} +v ${thisNick}", "${thisHostmask} MODE ${channel} -v ${thisNick}"));
		if (!getPropertyAsBoolean(operatorKick))
			responseMap.put(operatorKick, generateSet("${thisHostmask} KICK ${channel} ${targetNick}: ${random}", "${thisHostmask} JOIN :${channel}"));
		if (!getPropertyAsBoolean(operatorBan))
			responseMap.put(operatorBan, generateSet("${thisHostmask} MODE ${channel} +b ${thisNick}!*@*", "${thisHostmask} MODE ${channel} -b ${thisNick}!*@*"));
		if (!getPropertyAsBoolean(userPart))
			responseMap.put(userPart, generateSet("${thisHostmask} PART ${channel} :${random}", "${thisHostmask} JOIN :${channel}"));
		if (!getPropertyAsBoolean(userQuit))
			responseMap.put(userQuit, generateSet("${thisHostmask} QUIT :${random}", "${thisHostmask} JOIN :${channel}"));

		//Randomly shuffle responses and compact response to a single response queue
		List<String> randomKeys = new ArrayList(responseMap.keySet());
		Collections.shuffle(randomKeys);
		for (String curKey : randomKeys)
			for(String curResponse : responseMap.get(curKey)) {
				responseTypes.add(curKey);
				responseItems.add(curResponse);
			}
	}

	@Override
	public SampleResult sample(Entry e) {
		trace("sample()");
		SampleResult res = new SampleResult();
		res.setSuccessful(false); // Assume failure
		res.setSampleLabel(getName());

		//Reset last item if nessesary
		if(lastItem >= responseItems.size())
			lastItem = -1;
		
		//Get next item in the list
		String lineItem = responseItems.get(lastItem + 1); 
		String lineType = responseTypes.get(lastItem + 1);
		lastItem++;
		
		waitFor = UUID.randomUUID().toString();
		waitLatch = new CountDownLatch(1); 
		response = null;
		String thisNick = getPropertyAsString(botPrefix) + botNumber;
		
		//Build the line to send
		String line = lineItem;
		line = line.replace("${thisHostmask}", thisNick + "!~jmeter@bots.jmeter");
		line = line.replace("${thisNick}", thisNick);
		line = line.replace("${channel}", getPropertyAsString(channelPrefix) + channelRandom.nextInt(getPropertyAsInt(numChannels) + 1));
		line = line.replace("${targetNick}", getPropertyAsString(targetNick));
		line = line.replace("${random}", waitFor);
		line = line.replace("${command}", getPropertyAsString(command));
		
		/*
		 * Perform the sampling
		 */
		res.sampleStart(); // Start timing
		try {
			server.sendToClients(line);
			waitLatch.await();
			
			/*
			 * Set up the sample result details
			 */
			res.setSamplerData(line);
			res.setResponseData(response, null);
			res.setDataType(SampleResult.TEXT);

			res.setResponseCodeOK();
			res.setResponseMessage("OK");
		} catch (Exception ex) {
			log.debug("", ex);
			res.setResponseCode("500");
			res.setResponseMessage(ex.toString());
		}
		res.sampleEnd(); // End timimg
		return res;
	}

	public void acceptLine(String line) {
		if(line.contains(waitFor))
			waitLatch.countDown();
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

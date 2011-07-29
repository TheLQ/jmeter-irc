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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestListener;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

/**
 *
 * @author lordquackstar
 */
public class IrcBotSampler extends AbstractSampler implements TestListener, Comparable<IrcBotSampler> {
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
	protected int botNumber;
	protected IrcServer server = IrcBotGui.getServer();
	protected int lastItem = -1;
	protected List<String> responseItems = new ArrayList();
	protected static Random channelRandom = new Random();
	protected String thisNick;
	protected StringBuilder requestData;
	protected int requestDataLength;
	protected CountDownLatch latch;
	protected String responseLine;

	public IrcBotSampler() {
		botNumber = classCount++;
	}

	public void init() {
		//Pad nick with 0s to generate a unique botName
		thisNick = getPropertyAsString(botPrefix) + botNumber;
		thisNick = StringUtils.rightPad(thisNick, getPropertyAsString(botPrefix).length() + 9, "0");

		//Setup possible response list
		List<Set<String>> responseGroups = new ArrayList();
		if (getPropertyAsBoolean(channelCommand))
			responseGroups.add(generateResponseSet(":${thisHostmask} PRIVMSG ${channel} :${command} ${thisNick}"));
		if (getPropertyAsBoolean(PMCommand))
			responseGroups.add(generateResponseSet(":${thisHostmask} PRIVMSG ${targetNick} :${command} ${thisNick}"));
		if (getPropertyAsBoolean(channelMessage))
			responseGroups.add(generateResponseSet(":${thisHostmask} PRIVMSG ${channel} :${thisNick}"));
		if (getPropertyAsBoolean(channelAction))
			responseGroups.add(generateResponseSet(":${thisHostmask} PRIVMSG ${channel} :\u0001ACTION ${thisNick}\u0001"));
		if (getPropertyAsBoolean(channelNotice))
			responseGroups.add(generateResponseSet(":${thisHostmask} NOTICE ${channel} :${thisNick}"));
		if (getPropertyAsBoolean(PMMessage))
			responseGroups.add(generateResponseSet(":${thisHostmask} PRIVMSG ${targetNick} :${thisNick}"));
		if (getPropertyAsBoolean(PMAction))
			responseGroups.add(generateResponseSet(":${thisHostmask} PRIVMSG ${targetNick} :\u0001ACTION ${thisNick}\u0001"));
		if (getPropertyAsBoolean(operatorOp))
			responseGroups.add(generateResponseSet(":${thisHostmask} MODE ${channel} +o ${thisNick}", ":${thisHostmask} MODE ${channel} -o ${thisNick}"));
		if (getPropertyAsBoolean(operatorVoice))
			responseGroups.add(generateResponseSet(":${thisHostmask} MODE ${channel} +v ${thisNick}", ":${thisHostmask} MODE ${channel} -v ${thisNick}"));
		if (getPropertyAsBoolean(operatorKick))
			responseGroups.add(generateResponseSet(":${thisHostmask} KICK ${channel} ${targetNick}: ${thisNick}", ":${thisHostmask} JOIN :${channel}"));
		if (getPropertyAsBoolean(operatorBan))
			responseGroups.add(generateResponseSet(":${thisHostmask} MODE ${channel} +b ${thisNick}!*@*", ":${thisHostmask} MODE ${channel} -b ${thisNick}!*@*"));
		if (getPropertyAsBoolean(userPart))
			responseGroups.add(generateResponseSet(":${thisHostmask} PART ${channel}", ":${thisHostmask} JOIN :${channel}"));
		if (getPropertyAsBoolean(userQuit))
			responseGroups.add(generateResponseSet(":${thisHostmask} QUIT :${thisNick}", ":${thisHostmask} JOIN :${channel}"));

		//Randomly shuffle responses and compact response to a single response queue
		Collections.shuffle(responseGroups);
		for (Set<String> curGroup : responseGroups)
			for (String curResponse : curGroup)
				responseItems.add(curResponse);
	}

	@Override
	public SampleResult sample(Entry e) {
		SampleResult res = new SampleResult();
		res.setSuccessful(false); // Assume failure
		res.setSampleLabel(getName());

		try {
			if (responseItems.isEmpty()) {
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
			lastItem++;

			//Replace channel if nessesary
			if (lineItem.contains("${channel}")) {
				String channelLine = getPropertyAsString(channelPrefix) + channelRandom.nextInt(getPropertyAsInt(numChannels) + 1);
				lineItem = StringUtils.replace(lineItem, "${channel}", channelLine);
				requestData.append("${channel} - ").append(channelLine).append("\n\r");
			}
			requestData.append("Processed Line - ").append(lineItem);

			res.setSamplerData(requestData.toString());

			//Reset request data
			requestData.setLength(requestDataLength);

			/*
			 * Perform the sampling
			 */
			latch = new CountDownLatch(1);
			server.addSampler(this);
			res.sampleStart(); // Start timing
			server.sendToClients(lineItem);
			latch.await();
			res.sampleEnd(); // End timimg

			/*
			 * Set up the sample result details
			 */
			res.setResponseData(responseLine, null);
			res.setDataType(SampleResult.TEXT);

			res.setResponseCodeOK();
			res.setSuccessful(true);
		} catch (Exception ex) {
			log.debug("Exception encountered when executing Sample", ex);
			res.setResponseCode("500");
			res.setResponseMessage(ex.toString());
			res.setResponseData("ERROR IN SAMPLING: " + ExceptionUtils.getFullStackTrace(ex), null);
			res.setDataType(SampleResult.TEXT);
		}
		return res;
	}

	public boolean parseLine(String line) {
		if (line.contains(thisNick)) {
			responseLine = line;
			latch.countDown();
			return true;
		}
		return false;
	}

	protected Set<String> generateResponseSet(String... responses) {
		Set<String> responseSet = new HashSet();
		for (String curResponse : responses) {
			String thisHostmaskLine = thisNick + "!~jmeter@bots.jmeter";

			String targetNickLine = getPropertyAsString(targetNick);
			String commandLine = getPropertyAsString(command);
			curResponse = StringUtils.replace(curResponse, "${thisNick}", thisNick);
			curResponse = StringUtils.replace(curResponse, "${thisHostmask}", thisHostmaskLine);
			curResponse = StringUtils.replace(curResponse, "${targetNick}", targetNickLine);
			curResponse = StringUtils.replace(curResponse, "${command}", commandLine);
			if (requestData == null) {
				requestData = new StringBuilder().append("${thisNick} - ").append(thisNick).append("\n\r").append("${thisHostmask} - ").append(thisHostmaskLine).append("\n\r").append("${targetNick} - ").append(targetNickLine).append("\n\r").append("${command} - ").append(commandLine).append("\n\r");
				requestDataLength = requestData.length();
			}
			responseSet.add(curResponse);
		}
		return responseSet;
	}

	@Override
	public void testStarted() {
		
		System.out.println("IRC Bot Sampler " + botNumber + " - Test started");
	}

	@Override
	public void testEnded() {
		server.clearSamplers();
		System.out.println("IRC Bot Sampler " + botNumber + " - Test ended");
	}

	@Override
	public void testStarted(String host) {
		//Do nothing
	}

	@Override
	public void testEnded(String host) {
		//Do nothing
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		//Do nothing
	}

	@Override
	public int compareTo(IrcBotSampler o) {
		return botNumber - o.botNumber;
	}
}

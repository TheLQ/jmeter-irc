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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

	public IrcBotSampler(IrcServer server) {
		this.server = server;
		classCount++;
		trace("ExampleSampler()");
	}

	@Override
	public SampleResult sample(Entry e) {
		trace("sample()");
		SampleResult res = new SampleResult();
		res.setSuccessful(false); // Assume failure
		res.setSampleLabel(getName());

		//Setup possible response list
		Map<String, Set<String>> responses = new HashMap();
		if (!getPropertyAsBoolean(channelCommand))
			responses.put(channelCommand, generateSet("${thisHostmask} PRIVMSG ${channel} :${command} ${random}"));
		if (!getPropertyAsBoolean(PMCommand))
			responses.put(PMCommand, generateSet("${thisHostmask} PRIVMSG ${targetNick} :${command} ${random}"));
		if (!getPropertyAsBoolean(channelMessage))
			responses.put(channelMessage, generateSet("${thisHostmask} PRIVMSG ${channel} :${random}"));
		if (!getPropertyAsBoolean(channelAction))
			responses.put(channelAction, generateSet("${thisHostmask} PRIVMSG ${channel} :\u0001ACTION ${random}\u0001"));
		if (!getPropertyAsBoolean(channelNotice))
			responses.put(channelNotice, generateSet("${thisHostmask} NOTICE ${channel} :${random}"));
		if (!getPropertyAsBoolean(PMMessage))
			responses.put(PMMessage, generateSet("${thisHostmask} PRIVMSG ${targetNick} :${random}"));
		if (!getPropertyAsBoolean(PMAction))
			responses.put(PMAction, generateSet("${thisHostmask} PRIVMSG ${targetNick} :\u0001ACTION ${random}\u0001"));
		if (!getPropertyAsBoolean(operatorOp))
			responses.put(operatorOp, generateSet("${thisHostmask} MODE ${channel} +o ${thisNick}", "${thisHostmask} MODE ${channel} -o ${thisNick}"));
		if (!getPropertyAsBoolean(operatorVoice))
			responses.put(operatorVoice, generateSet("${thisHostmask} MODE ${channel} +v ${thisNick}", "${thisHostmask} MODE ${channel} -v ${thisNick}"));
		if (!getPropertyAsBoolean(operatorKick))
			responses.put(operatorKick, generateSet("${thisHostmask} KICK ${channel} ${targetNick}: ${random}", "${thisHostmask} JOIN :${channel}"));
		if (!getPropertyAsBoolean(operatorBan))
			responses.put(operatorBan, generateSet("${thisHostmask} MODE ${channel} +b ${targetNick}!*@*", "${thisHostmask} MODE ${channel} -b ${targetNick}!*@*"));
		if (!getPropertyAsBoolean(userPart))
			responses.put(userPart, generateSet("${thisHostmask} PART ${channel} :${random}", "${thisHostmask} JOIN :${channel}"));
		if (!getPropertyAsBoolean(userQuit))
			responses.put(userQuit, generateSet("${thisHostmask} QUIT :${random}", "${thisHostmask} JOIN :${channel}"));

		//Randomly shuffle responses
		List<String> randomKeys = new ArrayList(responses.keySet());
		Collections.shuffle(randomKeys);
		Map<String, Set<String>> newResponses = new LinkedHashMap<String, Set<String>>();
		for (String curKey : randomKeys)
			newResponses.put(curKey, responses.get(curKey));
		responses = newResponses;

		/*
		 * Perform the sampling
		 */
		res.sampleStart(); // Start timing
		try {


			response = Thread.currentThread().getName();

			/*
			 * Set up the sample result details
			 */
			res.setSamplerData(data);
			res.setResponseData(response, null);
			res.setDataType(SampleResult.TEXT);

			res.setResponseCodeOK();
			res.setResponseMessage("OK");
			isOK = true;
		} catch (Exception ex) {
			log.debug("", ex);
			res.setResponseCode("500");
			res.setResponseMessage(ex.toString());
		}
		res.sampleEnd(); // End timimg

		res.setSuccessful(isOK);

		return res;
	}

	public void acceptLine(String line) {
	}

	protected Set<String> generateSet(String... responses) {
		Set<String> responseSet = new HashSet();
		responseSet.addAll(Arrays.asList(responses));
		return responseSet;
	}

	protected String randomResponse() {
		return UUID.randomUUID().toString();
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

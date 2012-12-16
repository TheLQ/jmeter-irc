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
package org.apache.jmeter.protocol.irc.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import org.apache.jmeter.gui.util.HorizontalPanel;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;

/**
 *
 * @author lordquackstar
 */
public class IrcBotGui extends AbstractSamplerGui {
	protected JTextField botPrefix;
	protected JTextField channelPrefix;
	protected JTextField numChannels;
	protected JTextField command;
	protected JTextField targetNick;
	protected JTextField port;
	protected JCheckBox channelCommand;
	protected JCheckBox PMCommand;
	protected JCheckBox channelMessage;
	protected JCheckBox channelAction;
	protected JCheckBox channelNotice;
	protected JCheckBox PMMessage;
	protected JCheckBox PMAction;
	protected JCheckBox operatorOp;
	protected JCheckBox operatorVoice;
	protected JCheckBox operatorKick;
	protected JCheckBox operatorBan;
	protected JCheckBox userPart;
	protected JCheckBox userQuit;
	protected static IrcServer server;
	JLabel statusLabel;
	JLabel clientLabel;
	JButton startStopButton;

	public IrcBotGui() {
		// Standard setup
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH); // Add the standard title

		//Main Panel
		VerticalPanel mainPanel = new VerticalPanel();
		mainPanel.add(createServerPanel());
		mainPanel.add(createBotInfoPanel());
		mainPanel.add(createTestPanel());
		add(mainPanel, BorderLayout.CENTER);
	}

	protected Component createServerPanel() {
		JPanel ircServer = generatePanel(new BorderLayout(), "IRC Server");

		HorizontalPanel panel = new HorizontalPanel();

		statusLabel = new JLabel("Status: Stopped");
		panel.add(statusLabel);
		
		clientLabel = new JLabel("");
		updateClientConnected(false);
		panel.add(statusLabel);

		startStopButton = new JButton("Start");
		panel.add(startStopButton);
		startStopButton.setActionCommand("StartStopButton");
		startStopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("StartStopButton"))
					if (startStopButton.getText().equals("Start"))
						restartServer(Integer.parseInt(port.getText()));
					else if (startStopButton.getText().equals("Stop"))
						try {
							server.close();
							startStopButton.setText("Start");
							statusLabel.setText("Status: Stopped");
							statusLabel.setForeground(Color.black);
						} catch (IOException ex) {
							statusLabel.setText("Status: Error");
							statusLabel.setForeground(Color.red);
							statusLabel.setToolTipText(ex.getClass().getCanonicalName() + ": " + ex.getMessage());
							ex.printStackTrace();
						}
			}
		});

		panel.add(new JLabel("Host: 127.0.0.1"));
		panel.add(generateTextField(port = new JTextField("6667", 6), JMeterUtils.getResString("web_server_port")));
		port.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				int portValue = Integer.parseInt(port.getText());
				if (e.getComponent() == port && server.getPort() != portValue)
					//Recreate server with new port
					restartServer(portValue);
			}
		});

		ircServer.add(panel);
		return ircServer;
	}

	protected void restartServer(final int portValue) {
		new Thread() {
			@Override
			public void run() {
				try {
					if (server != null)
						server.close();
					server = new IrcServer(portValue, IrcBotGui.this);
					server.init();
				} catch (final IOException ex) {
					if (!server.isClosedGood())
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								statusLabel.setText("Status: Error");
								statusLabel.setForeground(Color.red);
								statusLabel.setToolTipText(ex.getClass().getCanonicalName() + ": " + ex.getMessage());
								ex.printStackTrace();
							}
						});
				}
			}
		}.start();
		statusLabel.setToolTipText("");
		startStopButton.setText("Stop");
		statusLabel.setText("Status: Started on port " + portValue);
		statusLabel.setForeground(new Color(0x00, 0xC0, 0x00));
		startStopButton.repaint();
	}

	/*
	 * Create a data input text field
	 *
	 * @return the panel for entering the data
	 */
	protected Component createBotInfoPanel() {
		JPanel botInfoPanel = generatePanel(new GridLayout(2, 3), "Bot Information");

		botInfoPanel.add(generateTextField(botPrefix = new JTextField("jmeterBot", 10), "Bot Prefix: "));
		botInfoPanel.add(generateTextField(channelPrefix = new JTextField("#jmeter", 10), "Channel Prefix: "));
		botInfoPanel.add(generateTextField(numChannels = new JTextField("1", 10), "Channels: "));
		botInfoPanel.add(generateTextField(command = new JTextField("?jmeter", 10), "Command: "));
		botInfoPanel.add(generateTextField(targetNick = new JTextField("", 10), "Target Nick: "));

		return botInfoPanel;
	}

	protected Component createTestPanel() {
		JPanel testPanel = generatePanel(null, "Possible Actions");

		//Build checkboxes into groups
		Map<String, Set<JCheckBox>> checkBoxGroups = new LinkedHashMap<String, Set<JCheckBox>>();
		checkBoxGroups.put("Commands", new LinkedHashSet<JCheckBox>() {
			{
				add(channelCommand = new JCheckBox("Channel Message"));
				add(PMCommand = new JCheckBox("Private Message"));
			}
		});
		checkBoxGroups.put("Random Messages", new LinkedHashSet<JCheckBox>() {
			{
				add(channelMessage = new JCheckBox("Channel Message"));
				add(channelNotice = new JCheckBox("Channel Notice"));
				add(channelAction = new JCheckBox("Channel Action"));
				add(PMMessage = new JCheckBox("Private Message"));
				add(PMAction = new JCheckBox("Private Action"));
			}
		});
		checkBoxGroups.put("Operator Actions", new LinkedHashSet<JCheckBox>() {
			{
				add(operatorOp = new JCheckBox("Operator-Deoperator (self)"));
				add(operatorVoice = new JCheckBox("Voice-Devoice (self)"));
				add(operatorKick = new JCheckBox("Kick-Join (self)"));
				add(operatorBan = new JCheckBox("Ban-Unban (self)"));
			}
		});
		checkBoxGroups.put("User Status", new LinkedHashSet<JCheckBox>() {
			{
				add(userPart = new JCheckBox("Part-Join"));
				add(userQuit = new JCheckBox("Quit-Join"));
			}
		});

		//Grab all checkboxes into one place
		final Set<JCheckBox> allCheckBoxes = new LinkedHashSet<JCheckBox>();
		for (Set<JCheckBox> curSet : checkBoxGroups.values())
			allCheckBoxes.addAll(curSet);

		//Build checkbox panel
		JPanel checkBoxPanelParent = new JPanel();
		for (Map.Entry<String, Set<JCheckBox>> curEntry : checkBoxGroups.entrySet()) {
			JPanel checkBoxPanel = generatePanel(null, curEntry.getKey());
			checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.PAGE_AXIS));
			for (JCheckBox curCheckBox : curEntry.getValue()) {
				//Check the box by default
				curCheckBox.setSelected(true);
				checkBoxPanel.add(curCheckBox);
			}
			checkBoxPanelParent.add(checkBoxPanel);
		}

		//Add to main panel
		testPanel.add(checkBoxPanelParent);

		//Toggle panel
		JPanel togglePanel = new JPanel();

		ActionListener checkBoxListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("CheckAll"))
					for (JCheckBox curBox : allCheckBoxes)
						curBox.setSelected(true);
				if (e.getActionCommand().equals("UnCheckAll"))
					for (JCheckBox curBox : allCheckBoxes)
						curBox.setSelected(false);
			}
		};
		JButton checkAll = new JButton("Check All");
		checkAll.setActionCommand("CheckAll");
		checkAll.addActionListener(checkBoxListener);
		togglePanel.add(checkAll);
		JButton unCheckAll = new JButton("Uncheck All");
		unCheckAll.setActionCommand("UnCheckAll");
		unCheckAll.addActionListener(checkBoxListener);
		togglePanel.add(unCheckAll);
		testPanel.add(togglePanel);

		return testPanel;
	}

	protected JPanel generatePanel(LayoutManager layout, String title) {
		JPanel panel = new JPanel(layout);
		if (layout == null)
			panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), title));
		return panel;
	}

	protected JPanel generateTextField(JTextField field, String labelName) {
		JPanel panel = new JPanel(new FlowLayout());
		JLabel label = new JLabel(labelName);
		label.setLabelFor(field);
		panel.add(label);
		panel.add(field);
		return panel;
	}
	
	protected void updateClientConnected(boolean hasClient) {
		if(hasClient)
			clientLabel.setText("Client connected");
		else
			clientLabel.setText("Client not connected");
	}

	@Override
	public String getStaticLabel() {
		return "IRC - Client";
	}

	@Override
	public String getLabelResource() {
		return "IRC - Client";
	}

	@Override
	public TestElement createTestElement() {
		IrcBotSampler sampler = new IrcBotSampler();
		modifyTestElement(sampler);
		return sampler;
	}

	@Override
	public void modifyTestElement(TestElement te) {
		te.clear();
		configureTestElement(te);
		te.setProperty(IrcBotSampler.botPrefix, botPrefix.getText());
		te.setProperty(IrcBotSampler.channelPrefix, channelPrefix.getText());
		te.setProperty(IrcBotSampler.numChannels, numChannels.getText());
		te.setProperty(IrcBotSampler.command, command.getText());
		te.setProperty(IrcBotSampler.targetNick, targetNick.getText());
		te.setProperty(IrcBotSampler.channelCommand, channelCommand.isSelected());
		te.setProperty(IrcBotSampler.PMCommand, PMCommand.isSelected());
		te.setProperty(IrcBotSampler.channelMessage, channelMessage.isSelected());
		te.setProperty(IrcBotSampler.channelAction, channelAction.isSelected());
		te.setProperty(IrcBotSampler.channelNotice, channelNotice.isSelected());
		te.setProperty(IrcBotSampler.PMMessage, PMMessage.isSelected());
		te.setProperty(IrcBotSampler.PMAction, PMAction.isSelected());
		te.setProperty(IrcBotSampler.operatorOp, operatorOp.isSelected());
		te.setProperty(IrcBotSampler.operatorVoice, operatorVoice.isSelected());
		te.setProperty(IrcBotSampler.operatorKick, operatorKick.isSelected());
		te.setProperty(IrcBotSampler.operatorBan, operatorBan.isSelected());
		te.setProperty(IrcBotSampler.userPart, userPart.isSelected());
		te.setProperty(IrcBotSampler.userQuit, userQuit.isSelected());
	}
	
	static IrcServer getServer() {
		return server;
	}
}

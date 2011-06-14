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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
		panel.add(generateTextField(new JTextField(15), JMeterUtils.getResString("web_server_domain")));
		panel.add(generateTextField(new JTextField(6), JMeterUtils.getResString("web_server_port")));

		ircServer.add(panel);
		return ircServer;
	}

	/*
	 * Create a data input text field
	 *
	 * @return the panel for entering the data
	 */
	protected Component createBotInfoPanel() {
		JPanel botInfoPanel = generatePanel(new GridLayout(2, 2), "Bot Information");

		botInfoPanel.add(generateTextField(botPrefix = new JTextField("jmeterBot", 10), "Bot Prefix: "));
		botInfoPanel.add(generateTextField(channelPrefix = new JTextField("#jmeter", 10), "Channel Prefix: "));
		botInfoPanel.add(generateTextField(numChannels = new JTextField("1", 10), "Channels: "));
		botInfoPanel.add(generateTextField(command = new JTextField("?jmeter", 10), "Command: "));

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
			for (JCheckBox curCheckBox : curEntry.getValue())
				checkBoxPanel.add(curCheckBox);
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

	@Override
	public String getStaticLabel() {
		return "IRC Bot";
	}

	@Override
	public String getLabelResource() {
		return "IRC Bot";
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
	}

	public static void main(String[] args) {
		JMeterUtils.getProperties("jmeter.properties");

		//Create and set up the window.
		JFrame frame = new JFrame("IRC Bot GUI");
		frame.setSize(700, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Add contents to the window.
		frame.add(new IrcBotGui());

		//Display the window.
		frame.setVisible(true);
	}
}

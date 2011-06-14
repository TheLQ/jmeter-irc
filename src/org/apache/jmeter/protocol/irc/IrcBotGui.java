package org.apache.jmeter.protocol.irc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import org.apache.jmeter.gui.ServerPanel;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;

/**
 *
 * @author lordquackstar
 */
public class IrcBotGui extends AbstractSamplerGui {
	private JTextArea data;

	public IrcBotGui() {
		// Standard setup
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH); // Add the standard title

		// Specific setup
		add(new ServerPanel(), BorderLayout.CENTER);
		add(createBotInfoPanel());
		add(createTestPanel());
	}

	/*
	 * Create a data input text field
	 *
	 * @return the panel for entering the data
	 */
	protected Component createBotInfoPanel() {
		JPanel botInfoPanel = generatePanel(null, "Bot Information");

		botInfoPanel.add(generateTextField(new JTextField("jmeterBot", 10), "Bot Prefix: "));
		botInfoPanel.add(generateTextField(new JTextField("#jmeter", 10), "Channel Prefix: "));

		return botInfoPanel;
	}

	protected Component createTestPanel() {
		JPanel testPanel = generatePanel(null, "Test items");

		//Build checkboxes into groups
		Map<String, Set<JCheckBox>> checkBoxGroups = new LinkedHashMap<String, Set<JCheckBox>>();
		checkBoxGroups.put("Messages", new LinkedHashSet<JCheckBox>() {
			{
				add(new JCheckBox("Channel Message"));
				add(new JCheckBox("Channel Notice"));
				add(new JCheckBox("Channel Action"));
				add(new JCheckBox("Private Message"));
				add(new JCheckBox("Private Action"));
			}
		});
		checkBoxGroups.put("Operator Actions", new LinkedHashSet<JCheckBox>() {
			{
				add(new JCheckBox("Operator-Deoperator (self)"));
				add(new JCheckBox("Voice-Devoice (self)"));
				add(new JCheckBox("Kick-Join (self)"));
				add(new JCheckBox("Ban-Unban (self)"));
			}
		});
		checkBoxGroups.put("User Status", new LinkedHashSet<JCheckBox>() {
			{
				add(new JCheckBox("Part-Join"));
				add(new JCheckBox("Quit-Join"));
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
			for(JCheckBox curCheckBox : curEntry.getValue())
				checkBoxPanel.add(curCheckBox);
			checkBoxPanelParent.add(checkBoxPanel);
		}

		//Add to main panel
		testPanel.add(checkBoxPanelParent);

		//Toggle panel
		JPanel togglePanel = new JPanel();

		JButton checkAll = new JButton("Check All");
		checkAll.setActionCommand("CheckAll");
		checkAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("CheckAll"))
					for (JCheckBox curBox : allCheckBoxes)
						curBox.setSelected(true);
			}
		});
		togglePanel.add(checkAll);

		JButton unCheckAll = new JButton("Uncheck All");
		unCheckAll.setActionCommand("UnCheckAll");
		unCheckAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("UnCheckAll"))
					for (JCheckBox curBox : allCheckBoxes)
						curBox.setSelected(false);
			}
		});
		togglePanel.add(unCheckAll);
		testPanel.add(togglePanel);

		return testPanel;
	}

	protected JPanel generatePanel(LayoutManager layout, String title) {
		JPanel panel = new JPanel(layout);
		if(layout == null)
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
	public String getLabelResource() {
		return "IRC Bot";
	}

	@Override
	public TestElement createTestElement() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void modifyTestElement(TestElement element) {
		throw new UnsupportedOperationException("Not supported yet.");
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

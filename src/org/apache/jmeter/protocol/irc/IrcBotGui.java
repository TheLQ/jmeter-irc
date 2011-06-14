package org.apache.jmeter.protocol.irc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import org.apache.jmeter.examples.sampler.ExampleSampler;
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
	}

	/*
	 * Create a data input text field
	 *
	 * @return the panel for entering the data
	 */
	private Component createBotInfoPanel() {
		JPanel botInfoPanel = generatePanel(null, "Bot Information");
		botInfoPanel.setLayout(new BoxLayout(botInfoPanel, BoxLayout.PAGE_AXIS));

		botInfoPanel.add(generateTextField(new JTextField("jmeterBot", 10), "Bot Prefix: "));
		botInfoPanel.add(generateTextField(new JTextField("#jmeter", 10), "Channel Prefix: "));

		return botInfoPanel;
	}

	protected JPanel generatePanel(LayoutManager layout, String title) {
		JPanel panel = new JPanel(layout);
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

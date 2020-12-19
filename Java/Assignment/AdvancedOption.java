package Assignment;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/*
 * �߼����û������ܣ�
 * 1.ѡ��������Լ��˿�
 * 2.�ı��ǳ�
 * 2.����Ͷ�ȡ�����ļ�
 */

public class AdvancedOption extends JFrame
{
	JLabel server_ip_label_;
	JTextField server_ip_field_;
	
	JLabel server_port_label_;
	JTextField server_port_field_;
	
	JLabel user_name_label_;
	JTextField user_name_field_;
	
	JButton apply_button_;
	JButton cancel_button_;
	/*
	 * settings:
	 * 1.SERVER_IP_ADDRESS
	 * 2.SERVER_PORT
	 * 3.USER_NAME
	 */
	Properties settings_;
	final File settings_path_ = new File("Client_Settings.properties");
	
	public AdvancedOption()
	{
		super("�߼�����");
		settings_ = new Properties();
		if(!readSettings())
		{
			//�½�properties�ļ���ʹ��Ĭ��ֵ
			settings_.clear();
			settings_.setProperty("SERVER_IP_ADDRESS", "127.0.0.1");
			settings_.setProperty("SERVER_PORT", "9999");
			settings_.setProperty("USER_NAME", "New User");
		}
		initializeUI();
	}
	
	/*
	 * �򿪸ô���
	 */
	public void display()
	{
		if(!readSettings())
		{
			//�½�properties�ļ���ʹ��Ĭ��ֵ
			settings_.clear();
			settings_.setProperty("SERVER_IP_ADDRESS", "127.0.0.1");
			settings_.setProperty("SERVER_PORT", "9999");
			settings_.setProperty("USER_NAME", "New User");
		}
		showSettings();
		setVisible(true);
	}
	
	/*
	 * ��ȡproperties
	 */
	public Properties getSettings()
	{
		return settings_;
	}
	
	/*
	 * UI��������ʾproperties�������(ÿ�δ򿪴���ʱ����)
	 */
	private void showSettings() 
	{
		//server_ip_field_
		server_ip_field_.setText(settings_.getProperty("SERVER_IP_ADDRESS"));
		
		//server_port_field_
		server_port_field_.setText(settings_.getProperty("SERVER_PORT"));
		
		//user_name_field_
		user_name_field_.setText(settings_.getProperty("USER_NAME"));
		
	}
	
	/*
	 * ��ȡ���ص�properties�ļ�
	 */
	private boolean readSettings() 
	{			
		//���Զ�ȡ�ļ���ȷ���ļ�����ָ����
		FileInputStream stream = null;
		boolean success = false;
		try{
			stream = new FileInputStream(settings_path_);
			settings_.load(stream);
			success = true;
		} catch(IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
                try {
                	stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		}
		
		return success;
	}
	
	/*
	 * �ı�properties
	 */
	public void modifySettings(Properties p)
	{
		settings_ = p;
	}
	
	/*
	 * ����properties�ļ�
	 */
	public void saveSettings()
	{
		//����û��ǳƳ���
		if(user_name_field_.getText().length() > 12 || user_name_field_.getText().isEmpty())
		{
			JOptionPane.showMessageDialog(null, "����ʧ�ܣ��û��ǳƳ���Ϊ1-12���ַ�");
			return;
		}
		
		//server_ip_field_
		settings_.setProperty("SERVER_IP_ADDRESS", server_ip_field_.getText());
		
		//server_port_field_
		settings_.setProperty("SERVER_PORT", server_port_field_.getText());
		
		//user_name_field_
		settings_.setProperty("USER_NAME", user_name_field_.getText());
		
		FileOutputStream stream = null;
		try{
			stream = new FileOutputStream(settings_path_);
			settings_.store(stream,"settings");
			JOptionPane.showMessageDialog(null, "����ɹ�");
		} catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "����ʧ��");
		} finally {
			if (stream != null) {
                try {
                	stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
		}
	}
	
	/*
	 * ��ʼ��UI����
	 */
	private void initializeUI()
	{
		//��ʾ����
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(425,225);
		setLocationRelativeTo(null); 
		setResizable(false);
		setLayout(new FlowLayout(FlowLayout.CENTER,10,10));
		
		//��ʼ������label��field
		server_ip_label_ = new JLabel("��������ַ",SwingConstants.RIGHT);
		server_ip_field_ = new JTextField();
		server_ip_label_.setPreferredSize(new Dimension(75,30));
		server_ip_field_.setPreferredSize(new Dimension(300,30));
		add(server_ip_label_);
		add(server_ip_field_);	
		
		server_port_label_ = new JLabel("�������˿�",SwingConstants.RIGHT);		
		server_port_field_ = new JTextField();
		server_port_label_.setPreferredSize(new Dimension(75,30));
		server_port_field_.setPreferredSize(new Dimension(300,30));
		add(server_port_label_);
		add(server_port_field_);
		
		
		user_name_label_ = new JLabel("�û��ǳ�",SwingConstants.RIGHT);
		user_name_field_ = new JTextField();
		user_name_label_.setPreferredSize(new Dimension(75,30));
		user_name_field_.setPreferredSize(new Dimension(300,30));
		add(user_name_label_);		
		add(user_name_field_);
		
		apply_button_ = new JButton("Ӧ��");
		cancel_button_ = new JButton("ȡ��");
		add(apply_button_);
		apply_button_.addActionListener(new ApplyButtonListener());
		add(cancel_button_);
		cancel_button_.addActionListener(new CancelButtonListener());
	}

	
	/*
	 * Ӧ�ð�ť�ļ�����
	 */
	private class ApplyButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveSettings();
		}
	}
	
	/*
	 * ȡ����ť�ļ�����
	 */
	private class CancelButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			//�����棬ֱ�ӹرմ���
			setVisible(false);
		}
	}

}
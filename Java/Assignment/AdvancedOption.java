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
 * 高级设置基础功能：
 * 1.选择服务器以及端口
 * 2.改变昵称
 * 2.保存和读取配置文件
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
		super("高级设置");
		settings_ = new Properties();
		if(!readSettings())
		{
			//新建properties文件，使用默认值
			settings_.clear();
			settings_.setProperty("SERVER_IP_ADDRESS", "127.0.0.1");
			settings_.setProperty("SERVER_PORT", "9999");
			settings_.setProperty("USER_NAME", "New User");
		}
		initializeUI();
	}
	
	/*
	 * 打开该窗口
	 */
	public void display()
	{
		if(!readSettings())
		{
			//新建properties文件，使用默认值
			settings_.clear();
			settings_.setProperty("SERVER_IP_ADDRESS", "127.0.0.1");
			settings_.setProperty("SERVER_PORT", "9999");
			settings_.setProperty("USER_NAME", "New User");
		}
		showSettings();
		setVisible(true);
	}
	
	/*
	 * 获取properties
	 */
	public Properties getSettings()
	{
		return settings_;
	}
	
	/*
	 * UI界面中显示properties里的数据(每次打开窗口时调用)
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
	 * 读取本地的properties文件
	 */
	private boolean readSettings() 
	{			
		//尝试读取文件，确保文件中有指定项
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
	 * 改变properties
	 */
	public void modifySettings(Properties p)
	{
		settings_ = p;
	}
	
	/*
	 * 保存properties文件
	 */
	public void saveSettings()
	{
		//检查用户昵称长度
		if(user_name_field_.getText().length() > 12 || user_name_field_.getText().isEmpty())
		{
			JOptionPane.showMessageDialog(null, "保存失败，用户昵称长度为1-12个字符");
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
			JOptionPane.showMessageDialog(null, "保存成功");
		} catch(IOException e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "保存失败");
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
	 * 初始化UI界面
	 */
	private void initializeUI()
	{
		//显示配置
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setSize(425,225);
		setLocationRelativeTo(null); 
		setResizable(false);
		setLayout(new FlowLayout(FlowLayout.CENTER,10,10));
		
		//初始化各个label与field
		server_ip_label_ = new JLabel("服务器地址",SwingConstants.RIGHT);
		server_ip_field_ = new JTextField();
		server_ip_label_.setPreferredSize(new Dimension(75,30));
		server_ip_field_.setPreferredSize(new Dimension(300,30));
		add(server_ip_label_);
		add(server_ip_field_);	
		
		server_port_label_ = new JLabel("服务器端口",SwingConstants.RIGHT);		
		server_port_field_ = new JTextField();
		server_port_label_.setPreferredSize(new Dimension(75,30));
		server_port_field_.setPreferredSize(new Dimension(300,30));
		add(server_port_label_);
		add(server_port_field_);
		
		
		user_name_label_ = new JLabel("用户昵称",SwingConstants.RIGHT);
		user_name_field_ = new JTextField();
		user_name_label_.setPreferredSize(new Dimension(75,30));
		user_name_field_.setPreferredSize(new Dimension(300,30));
		add(user_name_label_);		
		add(user_name_field_);
		
		apply_button_ = new JButton("应用");
		cancel_button_ = new JButton("取消");
		add(apply_button_);
		apply_button_.addActionListener(new ApplyButtonListener());
		add(cancel_button_);
		cancel_button_.addActionListener(new CancelButtonListener());
	}

	
	/*
	 * 应用按钮的监视器
	 */
	private class ApplyButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveSettings();
		}
	}
	
	/*
	 * 取消按钮的监视器
	 */
	private class CancelButtonListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent arg0) {
			//不保存，直接关闭窗口
			setVisible(false);
		}
	}

}
package Assignment;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

public class LoadDialog extends JFrame
{
	private JLabel  room_uid_label_ = new JLabel("房间号",SwingConstants.RIGHT);;
	private JTextField room_uid_field_ = new JTextField();
	
	private JLabel  room_password_label_ = new JLabel("房间密码",SwingConstants.RIGHT);
	private JTextField room_password_field_ = new JTextField();
	
	private JButton advanced_option_ = new JButton("高级设置");
	private JButton confirm_button_ = new JButton("确认");
	private JButton create_button_ = new JButton("创建");
	
	private AdvancedOption settings_;
	private Client client_;
	
	public LoadDialog()
	{
		super("欢迎使用 网络191 黄有亮 1906200004 聊天室应用 -载入界面");
		initializeUI();
		
		settings_ = new AdvancedOption();
	}
	
	private void initializeUI()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(400,170);
		setLocationRelativeTo(null); 
		setResizable(false);
		setLayout(new FlowLayout(FlowLayout.CENTER,10,10));
		
		//初始化各个label与field
		room_uid_label_.setPreferredSize(new Dimension(50,30));
		room_uid_field_.setPreferredSize(new Dimension(300,30));
		add(room_uid_label_);
		add(room_uid_field_);	
		
		room_password_label_.setPreferredSize(new Dimension(50,30));
		room_password_field_.setPreferredSize(new Dimension(300,30));
		add(room_password_label_);
		add(room_password_field_);	
		
		advanced_option_.addActionListener(new AdvancedOptionButtonActionListener());
		add(advanced_option_);
		
		confirm_button_.addActionListener(new ConfirmButtonActionListener());
		add(confirm_button_);		
		
		create_button_.addActionListener(new CreateButtonActionListener());
		add(create_button_);
		
		
		room_uid_field_.addFocusListener(new TextFieldListener(room_uid_field_,"请输入聊天室名称"));
		room_password_field_.addFocusListener(new TextFieldListener(room_password_field_,"请输入聊天室密码，若无则为空"));
	}
	
	private boolean try_connect(HeaderConstants connect_type, ObjectOutputStream writer,ObjectInputStream reader)
	{
		Properties pro = settings_.getSettings();
		
		//检验房间号和密码
		String room_uid = room_uid_field_.getText();
		String room_password = room_password_field_.getText();
		String user_name = pro.getProperty("USER_NAME");

		DataFrame response_frame = null;
		try {
			writer.writeObject(DataFrame.makeRequestFrame(connect_type,user_name,room_uid,room_password));
			response_frame =(DataFrame) reader.readObject(); 
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		} 
		
		if(response_frame == null)
			return false;
		
		if(!response_frame.isResponseServer())
			return false;
		
		if(response_frame.getData().charAt(0) != '0')
		{
			switch(response_frame.getData().charAt(0)) {
			case '1':
				JOptionPane.showMessageDialog(null, "聊天室不存在");
				return false;
			case '2':
				JOptionPane.showMessageDialog(null, "聊天室密码错误");
				return false;
			case '3':
				JOptionPane.showMessageDialog(null, "该聊天室已有该用户名，请更改后重新连接");
				return false;
			case '4':
				JOptionPane.showMessageDialog(null, "已经存在该房间");
				return false;
			case '5':
				JOptionPane.showMessageDialog(null, "帧传输出错");
				return false;
			case '6':
				JOptionPane.showMessageDialog(null, "服务器读取帧出错");
				return false;
			default:
				return false;
			}
		}
		
		//TODO 读取端口
		
		return true;
	}
	
	private boolean validityCheck()
	{
		//检验房间号和密码
		String room_uid = room_uid_field_.getText();
		if(room_uid.length() < 1 || room_uid.length() > 6)
		{
			JOptionPane.showMessageDialog(null, "房间名应为1-6个字符");
			return false;
		}
		
		String room_password = room_password_field_.getText();
		if(room_password.length() < 1 || room_password.length() > 14)
		{
			JOptionPane.showMessageDialog(null, "房间密码应为1-14个字符");
			return false;
		}
		
		String user_name = settings_.getSettings().getProperty("USER_NAME");
		if(user_name.length() < 1 || user_name.length() > 12)
		{
			JOptionPane.showMessageDialog(null, "用户名长度应为1-12个字符");
			return false;
		}
		
		return true;
	}
	
	private class TextFieldListener implements FocusListener
	{
		private String hint_;
		private JTextField text_field_;
		
		public TextFieldListener(JTextField text_field,String hint)
		{
			this.text_field_ = text_field;
			this.hint_ = hint;
			
			//默认显示
			text_field_.setText(hint_);
			text_field_.setForeground(Color.GRAY);
		}
		
		@Override
		public void focusGained(FocusEvent event)
		{
			//如有提示，则取消提示
			if(text_field_.getText().equals(hint_))
			{
				text_field_.setText("");
				text_field_.setForeground(Color.BLACK);
			}
		}
		
		@Override
		public void focusLost(FocusEvent event)
		{
			//如无提示，则新增提示
			if(text_field_.getText().equals(""))
			{
				text_field_.setForeground(Color.GRAY);
				text_field_.setText(hint_);
			}
		}
	}
	
	private class ConfirmButtonActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			if(!validityCheck())
				return;
				
			//先尝试连接服务器
			Properties pro = settings_.getSettings();
			String ip_address = pro.getProperty("SERVER_IP_ADDRESS");
			String port = pro.getProperty("SERVER_PORT");
			
			Socket socket = new Socket();
			ObjectOutputStream writer = null;
			ObjectInputStream reader = null;
			boolean writer_init = true;
			InetSocketAddress server_address = new InetSocketAddress(ip_address,Integer.valueOf(port));
			try {
				socket.connect(server_address,50);
				writer = new ObjectOutputStream(socket.getOutputStream());
				reader = new ObjectInputStream(socket.getInputStream());
			} catch(SocketTimeoutException e) {
				//连接失败
				//e.printStackTrace();
			} catch(Exception e){
				//e.printStackTrace();
				writer_init = false;
			} finally {
				if(socket.isConnected() && writer_init)
				{
					if(try_connect(HeaderConstants.REQUEST_CONNECT,writer,reader))
					{
						//新建线程运行客户端
						setVisible(false);
						JOptionPane.showMessageDialog(null, "连接成功");
						client_ = new Client(socket,reader,writer,room_uid_field_.getText(),settings_);
						client_.setVisible(true);					
					} else {
						try {
							writer.close();
							reader.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} else {
					//连接失败
					JOptionPane.showMessageDialog(null, "服务器连接失败");
				}
			}
		}
	}
	
	private class AdvancedOptionButtonActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			settings_.display();
		}
	}
	
	private class CreateButtonActionListener implements ActionListener
	{
		@Override
		public void actionPerformed(ActionEvent event)
		{
			if(!validityCheck())
				return;
			
			//先尝试连接服务器
			Properties pro = settings_.getSettings();
			String ip_address = pro.getProperty("SERVER_IP_ADDRESS");
			String port = pro.getProperty("SERVER_PORT");
			
			Socket socket = new Socket();
			ObjectOutputStream writer = null;
			ObjectInputStream reader = null;
			boolean writer_init = true;
			InetSocketAddress server_address = new InetSocketAddress(ip_address,Integer.valueOf(port));
			try {
				socket.connect(server_address,50);
				writer = new ObjectOutputStream(socket.getOutputStream());
				reader = new ObjectInputStream(socket.getInputStream());
			} catch(SocketTimeoutException e) {
				//e.printStackTrace();
			} catch(Exception e){
				e.printStackTrace();
				writer_init = false;
			} finally {
				if(socket.isConnected() && writer_init)
				{
					if(try_connect(HeaderConstants.REQUEST_CREATE,writer,reader))
					{
						//新建线程运行客户端
						setVisible(false);
						JOptionPane.showMessageDialog(null, "房间创建成功");
						client_ = new Client(socket,reader,writer,room_uid_field_.getText(),settings_);
						client_.setVisible(true);						
					}
				} else {
					//连接失败
					JOptionPane.showMessageDialog(null, "服务器连接失败");
				}
			}
		}
	}
	
	public static void main(String[] args)
	{
		LoadDialog ld = new LoadDialog();
		ld.show();
	}
}
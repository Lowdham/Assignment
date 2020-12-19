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
	private JLabel  room_uid_label_ = new JLabel("�����",SwingConstants.RIGHT);;
	private JTextField room_uid_field_ = new JTextField();
	
	private JLabel  room_password_label_ = new JLabel("��������",SwingConstants.RIGHT);
	private JTextField room_password_field_ = new JTextField();
	
	private JButton advanced_option_ = new JButton("�߼�����");
	private JButton confirm_button_ = new JButton("ȷ��");
	private JButton create_button_ = new JButton("����");
	
	private AdvancedOption settings_;
	private Client client_;
	
	public LoadDialog()
	{
		super("��ӭʹ�� ����191 ������ 1906200004 ������Ӧ�� -�������");
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
		
		//��ʼ������label��field
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
		
		
		room_uid_field_.addFocusListener(new TextFieldListener(room_uid_field_,"����������������"));
		room_password_field_.addFocusListener(new TextFieldListener(room_password_field_,"���������������룬������Ϊ��"));
	}
	
	private boolean try_connect(HeaderConstants connect_type, ObjectOutputStream writer,ObjectInputStream reader)
	{
		Properties pro = settings_.getSettings();
		
		//���鷿��ź�����
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
				JOptionPane.showMessageDialog(null, "�����Ҳ�����");
				return false;
			case '2':
				JOptionPane.showMessageDialog(null, "�������������");
				return false;
			case '3':
				JOptionPane.showMessageDialog(null, "�����������и��û���������ĺ���������");
				return false;
			case '4':
				JOptionPane.showMessageDialog(null, "�Ѿ����ڸ÷���");
				return false;
			case '5':
				JOptionPane.showMessageDialog(null, "֡�������");
				return false;
			case '6':
				JOptionPane.showMessageDialog(null, "��������ȡ֡����");
				return false;
			default:
				return false;
			}
		}
		
		//TODO ��ȡ�˿�
		
		return true;
	}
	
	private boolean validityCheck()
	{
		//���鷿��ź�����
		String room_uid = room_uid_field_.getText();
		if(room_uid.length() < 1 || room_uid.length() > 6)
		{
			JOptionPane.showMessageDialog(null, "������ӦΪ1-6���ַ�");
			return false;
		}
		
		String room_password = room_password_field_.getText();
		if(room_password.length() < 1 || room_password.length() > 14)
		{
			JOptionPane.showMessageDialog(null, "��������ӦΪ1-14���ַ�");
			return false;
		}
		
		String user_name = settings_.getSettings().getProperty("USER_NAME");
		if(user_name.length() < 1 || user_name.length() > 12)
		{
			JOptionPane.showMessageDialog(null, "�û�������ӦΪ1-12���ַ�");
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
			
			//Ĭ����ʾ
			text_field_.setText(hint_);
			text_field_.setForeground(Color.GRAY);
		}
		
		@Override
		public void focusGained(FocusEvent event)
		{
			//������ʾ����ȡ����ʾ
			if(text_field_.getText().equals(hint_))
			{
				text_field_.setText("");
				text_field_.setForeground(Color.BLACK);
			}
		}
		
		@Override
		public void focusLost(FocusEvent event)
		{
			//������ʾ����������ʾ
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
				
			//�ȳ������ӷ�����
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
				//����ʧ��
				//e.printStackTrace();
			} catch(Exception e){
				//e.printStackTrace();
				writer_init = false;
			} finally {
				if(socket.isConnected() && writer_init)
				{
					if(try_connect(HeaderConstants.REQUEST_CONNECT,writer,reader))
					{
						//�½��߳����пͻ���
						setVisible(false);
						JOptionPane.showMessageDialog(null, "���ӳɹ�");
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
					//����ʧ��
					JOptionPane.showMessageDialog(null, "����������ʧ��");
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
			
			//�ȳ������ӷ�����
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
						//�½��߳����пͻ���
						setVisible(false);
						JOptionPane.showMessageDialog(null, "���䴴���ɹ�");
						client_ = new Client(socket,reader,writer,room_uid_field_.getText(),settings_);
						client_.setVisible(true);						
					}
				} else {
					//����ʧ��
					JOptionPane.showMessageDialog(null, "����������ʧ��");
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
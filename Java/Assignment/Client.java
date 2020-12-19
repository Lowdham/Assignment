package Assignment;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import java.net.Socket;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

public class Client extends JFrame
{
	//显示
	private JTextArea text_print_area_ = new JTextArea();
	
	//输入
	private JTextArea text_input_area_ = new JTextArea();
	
	//滚动条
	private JScrollPane text_print_area_scroll_ = new JScrollPane(text_print_area_);
	//设置
	private AdvancedOption settings_;
	
	private Socket socket_;
	private ObjectOutputStream writer_;
	private ObjectInputStream reader_;

	private MessageListener listener_;
	private TrackFrameSender track_;
	
	private boolean client_stop_ = false;
	
	public Client(Socket socket,ObjectInputStream reader,ObjectOutputStream writer,String room_uid,AdvancedOption settings)
	{	
		super("欢迎使用 网络191 黄有亮 1906200004 聊天室应用" + " 房间号[" + room_uid.trim() + "],用户[" + settings.getSettings().getProperty("USER_NAME") + "]");
		super.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				super.windowClosing(e);
				disconnect();
			}
		});
		
		socket_ = socket;
		reader_ = reader;
		writer_ = writer;
		settings_ = settings;
		
		listener_ = new MessageListener();
		track_ = new TrackFrameSender(room_uid,settings_.getSettings().getProperty("USER_NAME"));
		
		Thread listen = new Thread(listener_);
		Thread track = new Thread(track_);
		
		listen.start();
		track.start();
		
		initializeUI();
	}
	
	private void initializeUI()
	{
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800,600);
		setLocationRelativeTo(null);
		setResizable(false);
		setLayout(new FlowLayout(FlowLayout.CENTER,10,10));
		
		text_print_area_.setPreferredSize(new Dimension(750,400));
		text_print_area_.setLineWrap(true);
		text_print_area_.setEditable(false);
		
		add(text_print_area_scroll_);
		
		
		text_input_area_.setPreferredSize(new Dimension(750,125));
		text_input_area_.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		add(text_input_area_);
		
		text_input_area_.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent event) {
				if(event.getKeyChar() == KeyEvent.VK_ENTER && event.isControlDown() )
					sendMessage();
			}

			@Override
			public void keyReleased(KeyEvent event) {
			}

			@Override
			public void keyTyped(KeyEvent event) {

			}
			
		});
	}
	
	private synchronized void write(DataFrame frame) throws IOException
	{
		if(!client_stop_ && !socket_.isInputShutdown())
			writer_.writeObject(frame);
	}
	
	private void disconnect()
	{
		try {
			if(socket_ != null && !socket_.isClosed())
			{
				if(!socket_.isInputShutdown())
				{
					write(DataFrame.makeResponseFrame(HeaderConstants.REQUEST_QUIT,""));
					socket_.shutdownInput();
				}
				
				if(!socket_.isOutputShutdown())
					socket_.shutdownOutput();
				
				if(!socket_.isClosed())
					socket_.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			client_stop_ = true;
			listener_.stop_ = true;
			track_.stop_ = true;
		}
	}

	private void sendMessage()
	{
		String user_name = settings_.getSettings().getProperty("USER_NAME");
		String message = text_input_area_.getText();
		text_input_area_.setText("");
		try {
			write(DataFrame.makeTransportFrame(user_name,message));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "发送失败");
		}
	}
	
	private class MessageListener implements Runnable
	{
		//状态
		private boolean stop_ = false;
		
		@Override
		public void run()
		{
			while(true)
			{
				try {
					if(stop_)
						return;
					
					DataFrame frame = (DataFrame)reader_.readObject();
					if(frame.isResponseServer())
						processResponse(frame);
					else if(frame.isTransport())
						processMessage(frame);
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					//e.printStackTrace();
					disconnect();
				}
			}
		}
		
		private void processResponse(DataFrame frame)
		{
			switch (frame.getErrorCode()) {
			case '7':
				//断开连接
				disconnect();
				return;
			default:
				return;
			}
		}
		
		private void processMessage(DataFrame frame)
		{
			
			String sender_name = frame.getUserName();
			String message = frame.getContent();
			
			text_print_area_.append(sender_name.trim() + ":" + message + "\n");
		}
	}
	
	private class TrackFrameSender implements Runnable
	{
		//连接的房间号
		private String room_uid_;
		
		//用户昵称
		private String user_name_;
		
		//状态
		private boolean stop_ = false;
		
		public TrackFrameSender(String room_uid, String user_name)
		{
			room_uid_ = room_uid;
			user_name_ = user_name;
		}
		
		@Override
		public void run()
		{
			try {
				while(true)
				{					
					Thread.sleep(5000);
					if(!stop_)
						write(DataFrame.makeTrackFrame(room_uid_, user_name_));
					else
						return;
				} 
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				//无法发送心跳包，则断开连接
				e1.printStackTrace();
				disconnect();
			}
		
		}
	}

}
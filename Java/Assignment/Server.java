package Assignment;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;


import static java.lang.System.out;

/*
 * 服务器维护一个线程池，每个聊天室对应一个线程。
 */
public class Server
{
	//通讯端口
	private static final int port_ = 21325;
	
	/*
	 * 线程池，每个用户有一个线程，每个房间也有一个线程
	 */
	private ExecutorService thread_pool_ = Executors.newCachedThreadPool(); 
	
	//
	private Map<String,UserThread> user_thread_ = new ConcurrentHashMap<>();
	
	// 
	private Map<String,RoomThread> room_thread_ = new ConcurrentHashMap<>();
	
	//
	private Lock lock_ = new ReentrantLock();
	
	//
	private Condition cont_ = lock_.newCondition();
	
	//房间号与房间对象
	private Map<String,Room>  container_ = new ConcurrentHashMap<>();
	
	//服务器状态
	enum Status {
		FUNCTION,
		STOP;
	}
	private Status server_status_ = Status.FUNCTION;
	
	
	public Server()
	{
		thread_pool_.execute(new ConnectionPortListener());
		thread_pool_.execute(new TrackThread());
	}

	private void dismissRoom(String room_uid)
	{
		room_thread_.remove(room_uid);
		container_.remove(room_uid);
		System.out.println("Room [" + room_uid.trim() + "] has been dismissed");
	}
	
	/*
	 * 连接端口的监视器,只处理连接和创建请求
	 */
	class ConnectionPortListener implements Runnable
	{
		class retType
		{
			byte ret_code_;
			DataFrame frame_;
			
			public retType(byte code, DataFrame frame)
			{
				ret_code_ = code;
				frame_ = frame;
			}
		}
		
		@Override
		public void run()
		{
			ServerSocket server_socket = null;
			Socket client = null;
			try {
				server_socket = new ServerSocket(port_);
				while(true)
				{
					client = server_socket.accept();
					ObjectInputStream reader = new ObjectInputStream(client.getInputStream());
					ObjectOutputStream writer = new ObjectOutputStream(client.getOutputStream());
					//检查请求是否合法
					retType check = tryAccept(reader,writer);
					if(check.ret_code_ != 0x0) {
						//异常，发送错误信息至客户端并阻塞在下一次accept						
						DataFrame error_response = DataFrame.makeResponseFrame(HeaderConstants.RESPONSE_SERVER,
																			   String.valueOf(check.ret_code_));
						
						writer.writeObject(error_response);
						reader.close();
						writer.close();
						client.close();
						continue;
					}

					//处理请求
					processFrame(check.frame_,client,writer,reader);
				}
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
					try {
						if(server_socket != null && !server_socket.isClosed())
							server_socket.close();
						
						if(client !=null && !client.isClosed())
							client.close();
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("服务器已经退出");
					}
			}
		}
				
		/* @描述: 尝试接受请求
		 * @返回值	0x0:正常
		 * 			0x1:房间不存在
		 * 			0x2:密码错误
		 * 			0x3:用户昵称重复
		 * 			0x4:已存在该房间
		 * 			0x5:帧错误
		 * 			0x6:读取错误
		 */
		private retType tryAccept(ObjectInputStream reader, ObjectOutputStream writer)
		{
			try {
				DataFrame frame = (DataFrame) reader.readObject();
				return new retType(checkFrame(frame),frame);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return new retType((byte) 0x6,new DataFrame());
		}

		/* @描述	: 检查请求是否合法
		 * @返回值:	0x0:正常
		 * 			0x1:房间不存在
		 * 			0x2:密码错误
		 * 			0x3:用户昵称重复
		 * 			0x4:已存在该房间
		 * 			0x5:帧错误
		 * 			0x6:保留
		 */
		private byte checkFrame(DataFrame request)
		{
			if(request.isRequestConnect()) {
				//连接到已有房间
				
				//先获得房间号
				String room_uid = request.getRoomUid();
				
				Room room = container_.get(room_uid);
				
				//房间不存在
				if(room == null)
					return 0x1;
				
				//密码错误
				if(!request.getRoomPassword().equals(room.password_))
					return 0x2;
				
				//用户昵称重复
				if(room.users_.get(request.getUserName()) != null)
					return 0x3;
				
				return 0x0;
			} else if (request.isRequestCreate()) {
				//创建房间
				//已有房间
				if(container_.get(request.getRoomUid()) != null)
					return 0x4;
				else
					return 0x0;
			}

			return 0x5;
		}
	
		/*
		 * @描述:处理请求帧
		 */
		private void processFrame(DataFrame request_frame,
								  Socket client,
								  ObjectOutputStream writer,
								  ObjectInputStream reader) throws IOException
		{
			if(request_frame.isRequestConnect()) {
				//在房间中注册该用户的Socket
				String room_uid = request_frame.getRoomUid();
				User new_user = new User(request_frame.getUserName(),client,writer,reader);
				Room room = container_.get(room_uid);
				room.users_.put(request_frame.getUserName(),new_user);
				
				//调用,并注册相关线程
				UserThread new_user_thread = new UserThread(room,new_user);
				thread_pool_.execute(new_user_thread);
				user_thread_.put(new_user.user_name_, new_user_thread);
				
				//连接成功
				new_user.writer_.writeObject(DataFrame.makeResponseFrame(HeaderConstants.RESPONSE_SERVER,"0"));
				System.out.println("连接成功，用户:" + request_frame.getUserName());
			} else if (request_frame.isRequestCreate()) {
				//新建房间，初始化并注册该用户的Socket
				String room_uid = request_frame.getRoomUid();
				User new_user = new User(request_frame.getUserName(),client,writer,reader);
				Room new_room = new Room(request_frame.getRoomPassword(),
						 				 new LinkedList<Message>(),
						 				 new ConcurrentHashMap<>(),
						 				 new ReentrantLock());
				new_room.users_.put(request_frame.getUserName(),new_user);
				container_.put(room_uid,new_room);
				
				//调用相关线程
				RoomThread new_room_thread = new RoomThread(room_uid,new_room);
				UserThread new_user_thread = new UserThread(new_room,new_user);
				thread_pool_.execute(new_room_thread);
				thread_pool_.execute(new_user_thread);
				user_thread_.put(new_user.user_name_, new_user_thread);
				room_thread_.put(room_uid, new_room_thread);
				
				//新建成功
				new_user.writer_.writeObject(DataFrame.makeResponseFrame(HeaderConstants.RESPONSE_SERVER,"0"));
				System.out.println("房间创建成功，创建者:" + request_frame.getUserName() + "\t房间UID:" + room_uid+ "\t房间密码:" + request_frame.getRoomPassword());
			}
		}

	}
	
	/*
	 * 心跳检测线程，检测所有用户的心跳包(每隔特定时间后检测一次)
	 */
	class TrackThread implements Runnable
	{
		private static final int interval_ = 3 * 1000;	//10s
		
		@Override
		public void run()
		{
			while(true)
			{
				try {
					Thread.sleep(interval_);
					
					//服务器退出
					if(server_status_ == Status.STOP)
						return;
					
					//检查心跳包
					for(String key: container_.keySet())
					{
						Room current_room = container_.get(key);
						processRoom(current_room);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		//处理房间内心跳包队列
		private void processRoom(Room room)
		{
			for(String key : room.users_.keySet())
			{
				//对于每个用户，在心跳队列中寻找其心跳包，若无则调用其keepTrack方法。
				//若方法返回false，则表明用户失去连接。
				User current_user = room.users_.get(key);
				boolean received = true;
				
				if(!room.track_queue_.contains(key))
					received = false;
				else
					room.track_queue_.remove(key);
				
				if(!current_user.keepTrack(received))
					lostTrack(room, current_user);

			}
		}
		
		private void lostTrack(Room room, User user)
		{
			System.out.println(user.user_name_.trim()+" lost the connection");
			user_thread_.get(user.user_name_).disconnect();
			room.users_.remove(user.user_name_);
		}
	}
	
	/*
	 * 用户线程
	 */
	class UserThread implements Runnable
	{
		//用户所在的房间
		private Room room_;
		
		//用户
		private User user_;
		
		//
		private boolean internal_stop_ = false;
		
		public UserThread(Room room, User user)
		{
			room_ = room;
			user_ = user;
		}
		
		@Override
		public void run()
		{
			System.out.println(user_.user_name_.trim() + "'s thread started.");
			while(true)
			{
				try {
					if((server_status_ == Status.STOP) || internal_stop_)
					{
						disconnect();
						System.out.println("User [" + user_.user_name_.trim() + "]'s thread has terminated.");
						return;
					}
					
					DataFrame frame = (DataFrame) user_.reader_.readObject();			
					processFrame(frame);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					//e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		public void disconnect()
		{
			if(internal_stop_)
				return;
			
			try {
				if(!user_.socket_.isOutputShutdown()){
					user_.writer_.writeObject(DataFrame.makeResponseFrame(HeaderConstants.REQUEST_QUIT, "7"));
					user_.writer_.close();
				}
				
				if(!user_.socket_.isInputShutdown())
					user_.reader_.close();
				
				if(!user_.socket_.isClosed())
					user_.socket_.close();
			} catch (SocketException e) {
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				room_.userQuit(user_.user_name_);
				if(room_.status_ == RoomStatus.DEAD)
				{
					room_.lock_.lock();
					room_.cond_.signal();
					room_.lock_.unlock();
				}
				internal_stop_ = true;
			}
		}
		
		private void processFrame(DataFrame frame)
		{
			if(frame.isTrack()) {
				//如果是心跳包，则放入房间的心跳包队列中
				room_.track_queue_.offer(frame.getUserName());
				
			} else if (frame.isTransport()) {
				//如果是消息包，则将消息放入房间的消息队列中，并通知房间线程处理
				System.out.println("Message received [" + frame.getUserName().trim() +"]:[" + frame.getContent().trim() + "]");				
				Message new_message = new Message(frame.getUserName(),frame.getContent());
				room_.message_queue_.offer(new_message);
				room_.lock_.lock();
				room_.cond_.signal();
				room_.lock_.unlock();
			} else if (frame.isRequestQuit()) {
				disconnect();
			}
		}
	}
	
	/*
	 * 房间线程
	 * 功能:房间解散、消息群发
	 */
	class RoomThread implements Runnable
	{
		private String room_uid_;
		private Room room_;
		
		public RoomThread(String room_uid,Room source)
		{
			room_uid_ = room_uid;
			room_ = source;
		}
		
		@Override
		public void run()
		{
			while(true)
			{
				try {
					room_.lock_.lock();
					room_.cond_.await();
					
					//房间解散(待做)
					if(room_.status_ == RoomStatus.DEAD)
					{	
						dismissRoom(room_uid_);
						System.out.println("Room [" + room_uid_.trim() + "]'s thread has terminated.");
						return;
					}
					
					//处理消息队列中的数据
					while(!room_.message_queue_.isEmpty())
					{
						Message message = room_.message_queue_.poll();
						for(String key: room_.users_.keySet())
						{
							try {
								room_.users_.get(key).writer_.writeObject(DataFrame.makeTransportFrame(message));
							} catch (IOException e) {
								// 直接跳过当前用户
								e.printStackTrace();
							}
						}
					}
				} catch (InterruptedException e) {
					//中断后查看是否停止线程
					//房间解散
					if(room_.status_ == RoomStatus.DEAD)
					{
						dismissRoom(room_uid_);
						System.out.println("Room [" + room_uid_.trim() + "]'s thread has terminated.");
						return;
					}
					e.printStackTrace();
				}
			}
		}

	}
	
	public void getServer()
	{
		try {
			lock_.lock();
			cont_.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		Server server = new Server();
		server.getServer();
	}
}


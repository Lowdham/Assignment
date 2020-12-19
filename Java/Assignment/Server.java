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
 * ������ά��һ���̳߳أ�ÿ�������Ҷ�Ӧһ���̡߳�
 */
public class Server
{
	//ͨѶ�˿�
	private static final int port_ = 21325;
	
	/*
	 * �̳߳أ�ÿ���û���һ���̣߳�ÿ������Ҳ��һ���߳�
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
	
	//������뷿�����
	private Map<String,Room>  container_ = new ConcurrentHashMap<>();
	
	//������״̬
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
	 * ���Ӷ˿ڵļ�����,ֻ�������Ӻʹ�������
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
					//��������Ƿ�Ϸ�
					retType check = tryAccept(reader,writer);
					if(check.ret_code_ != 0x0) {
						//�쳣�����ʹ�����Ϣ���ͻ��˲���������һ��accept						
						DataFrame error_response = DataFrame.makeResponseFrame(HeaderConstants.RESPONSE_SERVER,
																			   String.valueOf(check.ret_code_));
						
						writer.writeObject(error_response);
						reader.close();
						writer.close();
						client.close();
						continue;
					}

					//��������
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
						System.out.println("�������Ѿ��˳�");
					}
			}
		}
				
		/* @����: ���Խ�������
		 * @����ֵ	0x0:����
		 * 			0x1:���䲻����
		 * 			0x2:�������
		 * 			0x3:�û��ǳ��ظ�
		 * 			0x4:�Ѵ��ڸ÷���
		 * 			0x5:֡����
		 * 			0x6:��ȡ����
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

		/* @����	: ��������Ƿ�Ϸ�
		 * @����ֵ:	0x0:����
		 * 			0x1:���䲻����
		 * 			0x2:�������
		 * 			0x3:�û��ǳ��ظ�
		 * 			0x4:�Ѵ��ڸ÷���
		 * 			0x5:֡����
		 * 			0x6:����
		 */
		private byte checkFrame(DataFrame request)
		{
			if(request.isRequestConnect()) {
				//���ӵ����з���
				
				//�Ȼ�÷����
				String room_uid = request.getRoomUid();
				
				Room room = container_.get(room_uid);
				
				//���䲻����
				if(room == null)
					return 0x1;
				
				//�������
				if(!request.getRoomPassword().equals(room.password_))
					return 0x2;
				
				//�û��ǳ��ظ�
				if(room.users_.get(request.getUserName()) != null)
					return 0x3;
				
				return 0x0;
			} else if (request.isRequestCreate()) {
				//��������
				//���з���
				if(container_.get(request.getRoomUid()) != null)
					return 0x4;
				else
					return 0x0;
			}

			return 0x5;
		}
	
		/*
		 * @����:��������֡
		 */
		private void processFrame(DataFrame request_frame,
								  Socket client,
								  ObjectOutputStream writer,
								  ObjectInputStream reader) throws IOException
		{
			if(request_frame.isRequestConnect()) {
				//�ڷ�����ע����û���Socket
				String room_uid = request_frame.getRoomUid();
				User new_user = new User(request_frame.getUserName(),client,writer,reader);
				Room room = container_.get(room_uid);
				room.users_.put(request_frame.getUserName(),new_user);
				
				//����,��ע������߳�
				UserThread new_user_thread = new UserThread(room,new_user);
				thread_pool_.execute(new_user_thread);
				user_thread_.put(new_user.user_name_, new_user_thread);
				
				//���ӳɹ�
				new_user.writer_.writeObject(DataFrame.makeResponseFrame(HeaderConstants.RESPONSE_SERVER,"0"));
				System.out.println("���ӳɹ����û�:" + request_frame.getUserName());
			} else if (request_frame.isRequestCreate()) {
				//�½����䣬��ʼ����ע����û���Socket
				String room_uid = request_frame.getRoomUid();
				User new_user = new User(request_frame.getUserName(),client,writer,reader);
				Room new_room = new Room(request_frame.getRoomPassword(),
						 				 new LinkedList<Message>(),
						 				 new ConcurrentHashMap<>(),
						 				 new ReentrantLock());
				new_room.users_.put(request_frame.getUserName(),new_user);
				container_.put(room_uid,new_room);
				
				//��������߳�
				RoomThread new_room_thread = new RoomThread(room_uid,new_room);
				UserThread new_user_thread = new UserThread(new_room,new_user);
				thread_pool_.execute(new_room_thread);
				thread_pool_.execute(new_user_thread);
				user_thread_.put(new_user.user_name_, new_user_thread);
				room_thread_.put(room_uid, new_room_thread);
				
				//�½��ɹ�
				new_user.writer_.writeObject(DataFrame.makeResponseFrame(HeaderConstants.RESPONSE_SERVER,"0"));
				System.out.println("���䴴���ɹ���������:" + request_frame.getUserName() + "\t����UID:" + room_uid+ "\t��������:" + request_frame.getRoomPassword());
			}
		}

	}
	
	/*
	 * ��������̣߳���������û���������(ÿ���ض�ʱ�����һ��)
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
					
					//�������˳�
					if(server_status_ == Status.STOP)
						return;
					
					//���������
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
		
		//������������������
		private void processRoom(Room room)
		{
			for(String key : room.users_.keySet())
			{
				//����ÿ���û���������������Ѱ�����������������������keepTrack������
				//����������false��������û�ʧȥ���ӡ�
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
	 * �û��߳�
	 */
	class UserThread implements Runnable
	{
		//�û����ڵķ���
		private Room room_;
		
		//�û�
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
				//�����������������뷿���������������
				room_.track_queue_.offer(frame.getUserName());
				
			} else if (frame.isTransport()) {
				//�������Ϣ��������Ϣ���뷿�����Ϣ�����У���֪ͨ�����̴߳���
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
	 * �����߳�
	 * ����:�����ɢ����ϢȺ��
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
					
					//�����ɢ(����)
					if(room_.status_ == RoomStatus.DEAD)
					{	
						dismissRoom(room_uid_);
						System.out.println("Room [" + room_uid_.trim() + "]'s thread has terminated.");
						return;
					}
					
					//������Ϣ�����е�����
					while(!room_.message_queue_.isEmpty())
					{
						Message message = room_.message_queue_.poll();
						for(String key: room_.users_.keySet())
						{
							try {
								room_.users_.get(key).writer_.writeObject(DataFrame.makeTransportFrame(message));
							} catch (IOException e) {
								// ֱ��������ǰ�û�
								e.printStackTrace();
							}
						}
					}
				} catch (InterruptedException e) {
					//�жϺ�鿴�Ƿ�ֹͣ�߳�
					//�����ɢ
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


package Assignment;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Room
{
	//��������
	public String password_;
	
	//��Ϣ����
	public Queue<Message> message_queue_;
	
	//����������
	public Queue<String> track_queue_;
	
	//�û�
	public Map<String,User> users_;
	
	//֪ͨ�õ���
	public Lock lock_;
	
	//�ź���
	public Condition cond_;
	
	//����״̬
	public RoomStatus status_;
	
	public Room(String password,Queue<Message> mq, Map<String,User> users, Lock lock)
	{
		password_ = password;
		message_queue_ = mq;
		track_queue_ = new ConcurrentLinkedQueue<String>();
		users_ = users;
		lock_ = lock;
		cond_ = lock_.newCondition();
		status_ = RoomStatus.FUNCTION;
	}

	public void userQuit(String user_name)
	{
		System.out.println("User [" + user_name.trim() + "] quits");
		users_.remove(user_name);

		//
		if(users_.isEmpty())
			status_ = RoomStatus.DEAD;
		
	}
		
}
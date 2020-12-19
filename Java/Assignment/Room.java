package Assignment;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Room
{
	//房间密码
	public String password_;
	
	//消息队列
	public Queue<Message> message_queue_;
	
	//心跳包队列
	public Queue<String> track_queue_;
	
	//用户
	public Map<String,User> users_;
	
	//通知用的锁
	public Lock lock_;
	
	//信号量
	public Condition cond_;
	
	//房间状态
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
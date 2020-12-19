package Assignment;

import java.io.Serializable;

public class DataFrame implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private HeaderConstants header_;
	
	/*		数据帧格式
	 * [0][1-(length_+1)]
	 *  a   l       data
	 *  
	 *  当header_ = REQUEST_CONNECT/REQUEST_CREATE时
	 *  data_[0-11]:用户名(限制为1-12个字符),这里时检查聊天室内用户名是否重复
	 *  data_[12-17]:聊天室号码(6)
	 *  data_[18-31]:聊天室密码(14)
	 *  
	 *  当header_ = TRACK_CONNECT时
	 *  data_[0-11]:用户名
	 *  data_[12-17]:聊天室号码
	 *  
	 *  当header_ = RESPONSE_SERVER时
	 *  服务端发送:
	 *  data_[0]:错误代码	0:正常
	 * 						1:房间不存在
	 * 						2:密码错误
	 * 						3:用户昵称重复
	 * 						4:已存在该房间
	 * 						5:帧错误
	 * 						6:读取错误
	 * 						7:断开连接
	 *  data_[1-5]:心跳包端口
	 *  data_[6-10]:消息通讯端口
	 * 
	 *  当header_ = TRANSPORT时,
	 *  data_[0-11]:用户昵称
	 *  data_[12-(length_+12)]:通讯数据
	 *  
	 */ 
	
	//数据帧数据
	private String data_;
	
	public DataFrame()
	{
		header_ = HeaderConstants.UNDEFINED;
	}

	public void setHeader(HeaderConstants h)
	{
		header_ = h;
	}

	public void setData(String str)
	{
		data_ = str;
	}
	
	public HeaderConstants type()
	{
		return header_;
	}
	
	public String getData()
	{
		return data_;
	}
	
	public boolean isValid()
	{
		return header_ != HeaderConstants.UNDEFINED;
	}
	
	public boolean isRequestConnect()
	{
		return header_ == HeaderConstants.REQUEST_CONNECT;
	}
	
	public boolean isRequestQuit()
	{
		return header_ == HeaderConstants.REQUEST_QUIT;
	}
	
	public boolean isResponseServer()
	{
		return header_ == HeaderConstants.RESPONSE_SERVER;
	}
	
	public boolean isTransport()
	{
		return header_ == HeaderConstants.TRANSPORT_DATA;
	}

	public boolean isRequestCreate()
	{
		return header_ == HeaderConstants.REQUEST_CREATE;
	}
	
	public boolean isTrack()
	{
		return header_ == HeaderConstants.TRACK_CONNECT;
	}
	
	public String getUserName()
	{
		return data_.substring(0,12);
	}
	
	public String getRoomUid()
	{
		if((isRequestCreate() || isRequestConnect()) && data_.length() == 32)
			return data_.substring(12,18);
		else
			return new String();
	}
	
	public String getRoomPassword()
	{
		if((isRequestCreate() || isRequestConnect()) && data_.length() == 32)
			return data_.substring(18,32);
		else
			return new String();
	}
	
	public String getContent()
	{
		if((isTransport() || isResponseServer()) && data_.length() > 12)
			return data_.substring(12, data_.length());
		else
			return new String();
	}

	public int getTrackPort()
	{
		if(!isResponseServer())
			return -1;
		else
			return Integer.valueOf(data_.substring(1, 5));
	}
	
	public int getErrorCode()
	{
		if(isResponseServer())
			return Integer.valueOf(data_.charAt(0));
		else
			return -1;
	}
	
	public static DataFrame makeRequestFrame(HeaderConstants type,String user_name,String room_uid,String room_password)
	{
		DataFrame ret = new DataFrame();
		ret.setHeader(type);
		String user_name_formatted = String.format("%1$-12s", user_name);
		String room_uid_formatted = String.format("%1$-6s", room_uid);
		String room_password_formatted = String.format("%1$-14s", room_password);
		ret.setData(user_name_formatted + room_uid_formatted + room_password_formatted);
		return ret;
	}
	
	public static DataFrame makeResponseFrame(HeaderConstants type,String content)
	{
		DataFrame response_frame = new DataFrame();
		response_frame.setHeader(type);
		response_frame.setData(content);
		return response_frame;
	}
	
	public static DataFrame makeTransportFrame(String user_name,String content)
	{
		DataFrame ret = new DataFrame();
		ret.setHeader(HeaderConstants.TRANSPORT_DATA);
		String user_name_formatted = String.format("%1$-13s", user_name);
		ret.setData(user_name_formatted + content);
		return ret;
	}
	
	public static DataFrame makeTransportFrame(Message message)
	{
		return makeTransportFrame(message.user_name_,message.content_);
	}
	
	public static DataFrame makeTrackFrame(String room_uid,String user_name)
	{
		DataFrame ret = new DataFrame();
		ret.setHeader(HeaderConstants.TRACK_CONNECT);
		String user_name_formatted = String.format("%1$-12s", user_name);
		String room_uid_formatted = String.format("%1$-6s", room_uid);
		ret.setData(user_name_formatted + room_uid_formatted);
		return ret;
	}
	
	@Override
	public String toString() {
		return new String(header_ + data_);
	}
}
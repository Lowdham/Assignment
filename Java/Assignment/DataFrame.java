package Assignment;

import java.io.Serializable;

public class DataFrame implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private HeaderConstants header_;
	
	/*		����֡��ʽ
	 * [0][1-(length_+1)]
	 *  a   l       data
	 *  
	 *  ��header_ = REQUEST_CONNECT/REQUEST_CREATEʱ
	 *  data_[0-11]:�û���(����Ϊ1-12���ַ�),����ʱ������������û����Ƿ��ظ�
	 *  data_[12-17]:�����Һ���(6)
	 *  data_[18-31]:����������(14)
	 *  
	 *  ��header_ = TRACK_CONNECTʱ
	 *  data_[0-11]:�û���
	 *  data_[12-17]:�����Һ���
	 *  
	 *  ��header_ = RESPONSE_SERVERʱ
	 *  ����˷���:
	 *  data_[0]:�������	0:����
	 * 						1:���䲻����
	 * 						2:�������
	 * 						3:�û��ǳ��ظ�
	 * 						4:�Ѵ��ڸ÷���
	 * 						5:֡����
	 * 						6:��ȡ����
	 * 						7:�Ͽ�����
	 *  data_[1-5]:�������˿�
	 *  data_[6-10]:��ϢͨѶ�˿�
	 * 
	 *  ��header_ = TRANSPORTʱ,
	 *  data_[0-11]:�û��ǳ�
	 *  data_[12-(length_+12)]:ͨѶ����
	 *  
	 */ 
	
	//����֡����
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
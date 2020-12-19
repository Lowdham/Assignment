package Assignment;

public enum HeaderConstants{
	REQUEST_CONNECT,	//请求连接
	REQUEST_CREATE,	//请求创建房间
	REQUEST_QUIT,		//用户退出
	RESPONSE_SERVER,	//请求时服务器的回应
	RESPONSE_CLIENT,	//客户端发送
	TRANSPORT_DATA,	//数据传输
	TRACK_CONNECT,		//心跳包
	UNDEFINED;			//非法
}
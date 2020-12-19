package Assignment;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class User
{
	public String user_name_;
	public Socket socket_;
	public ObjectOutputStream writer_;
	public ObjectInputStream reader_;
	private int track_lost_;
	private static final int track_lost_max = 3;
	
	public User(String user_name,
				Socket socket,
				ObjectOutputStream writer,
				ObjectInputStream reader) throws IOException
	{
		user_name_ = user_name;
		socket_ = socket;
		writer_ = writer;
		reader_ = reader;
		track_lost_ = 0;
	}
	
	public boolean keepTrack(boolean is_track_received)
	{
		if(is_track_received) {
			track_lost_ = 0;
			return true;
		} else
			track_lost_++;
		
		if(track_lost_ < track_lost_max)
			return true;
		else
		 return false;
	}
}
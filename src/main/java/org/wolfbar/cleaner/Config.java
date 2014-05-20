package org.wolfbar.cleaner;

import java.io.File;
import java.util.List;

import org.wolfbar.cleaner.config.FileConfiguration;
import org.wolfbar.cleaner.config.YamlConfiguration;

public class Config
{
	// Client Params
	public final int maxTotalConnections;
	public final int maxPerRouterConnections;
	
	public final int BufferSizeKb;

	public final int socketTimeoutTime;
	public final int connectTimeoutTime;
	
	public final int threads;
	
	// User
	public final String baiduId;
	public final String baiduUss;
	
	// Target
	public final String barName;
	public final List<String> banKeyWords;
	
	
	public Config()
	{
		FileConfiguration config = new YamlConfiguration(new File("config.yml"));
		config.load();
		
		maxTotalConnections = config.getInt("ClientParam.MaxTotalConnections", 1000);
		maxPerRouterConnections = config.getInt("ClientParam.MaxPerRouterConnections", 1000);

		BufferSizeKb = config.getInt("ClientParam.BufferSizeKb", 512);

		socketTimeoutTime = config.getInt("ClientParam.SocketTimeoutTime", 5000);
		connectTimeoutTime = config.getInt("ClientParam.ConnectTimeoutTime", 5000);
		
		threads = config.getInt("ClientParam.Threads", 20);
		
		baiduId = config.getString("User.BaiduId");
		baiduUss = config.getString("User.BaiduUss");
		
		barName = config.getString("Target.BarName");
		banKeyWords = config.getStringList("Target.BanKeyWords");
	}
}

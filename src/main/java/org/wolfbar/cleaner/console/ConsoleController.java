package org.wolfbar.cleaner.console;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConsoleController
{
	private final Thread controlThread;
	private final Map<Integer, Runnable> commandCallbacks;
	
	private boolean isRunning;
	
	
	public ConsoleController()
	{
		controlThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				int lastKey = 0;
				while (isRunning)
				{
					try
					{
						if (System.in.available() <= 0)
						{
							Thread.sleep(10);
							continue;
						}
						
						int key = System.in.read();
						System.in.skip(System.in.available());
						if (key == lastKey)
						{
							processCommand(key);
							lastKey = 0;
							continue;
						}
						else
						{
							if (commandCallbacks.containsKey(key)) System.out.printf("请再次键入 %c 以确认命令。\n", key);
						}
						lastKey = key;
					}
					catch (IOException | InterruptedException e)
					{
						
					}
				}
			}
		});
		commandCallbacks = Collections.synchronizedMap(new HashMap<>());
	}
	
	public void addCommand(int key, Runnable runnable)
	{
		commandCallbacks.put(key, runnable);
	}
	
	public void startup()
	{
		isRunning = true;
		controlThread.start();
	}
	
	public void shutdown()
	{
		isRunning = false;
		
		if (Thread.currentThread() != controlThread)
		{
			try
			{
				controlThread.join();
			}
			catch (InterruptedException e) { }
		}
	}
	
	private void processCommand(int key)
	{
		Runnable callback = commandCallbacks.get(key);
		if (callback != null) callback.run();
	}
}

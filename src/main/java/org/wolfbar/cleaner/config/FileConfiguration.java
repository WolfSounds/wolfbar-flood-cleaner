package org.wolfbar.cleaner.config;

import java.io.File;

public interface FileConfiguration extends StreamConfiguration
{
	void setFile(File file);
	File getFile();
	
	void save();
	void load();
}

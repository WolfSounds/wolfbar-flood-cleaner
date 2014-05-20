package org.wolfbar.cleaner.config;

import java.io.InputStream;
import java.io.OutputStream;

public interface StreamConfiguration extends Configuration
{
	void read(InputStream stream);
	void write(OutputStream stream);
}

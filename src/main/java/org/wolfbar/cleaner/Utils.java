package org.wolfbar.cleaner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.LocalTime;

import org.apache.commons.lang3.StringUtils;

public class Utils
{
	@SuppressWarnings("unused")
	private static class LoggerPrintStream
	{
		public final PrintStream stream;
		public final PrintStream origin;
		public final File file;

		private FileOutputStream output;
		private boolean isFirstChar = true;


		public LoggerPrintStream(PrintStream origin, File file, String tag) throws FileNotFoundException
		{
			this.origin = origin;
			this.stream = new PrintStream(new OutputStream()
			{
				@Override
				public void write(int b) throws IOException
				{
					synchronized (file)
					{
						if (isFirstChar)
						{
							byte[] date;
							if (tag == null) date = ("[" + LocalTime.now().toString() + "] ").getBytes(CHARSET_UTF_8);
							else date = ("[" + LocalTime.now().toString() + "][" + tag + "] ").getBytes(CHARSET_UTF_8);
							origin.write(date);
							output.write(date);
							isFirstChar = false;
						}

						origin.write(b);
						output.write(b);
					}

					if (b == '\n') isFirstChar = true;
				}
			});
			this.file = file;
			this.output = new FileOutputStream(file, false);
		}
	}

	public static final Charset CHARSET_UTF_8 = Charset.forName("UTF-8");


	private static LoggerPrintStream out;
	private static LoggerPrintStream err;


	public static void installLogger()
	{
		if (out == null)
		{
			try
			{
				out = new LoggerPrintStream(System.err, new File("wolfbar-flood-cleaner.log"), null);
				System.setOut(out.stream);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}

		if (err == null)
		{
			try
			{
				err = new LoggerPrintStream(System.err, new File("wolfbar-flood-cleaner.err.log"), "ERR");
				System.setErr(err.stream);
			}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static String filterFilename(String in)
	{
		return StringUtils.replaceChars(in, "\\/:*", "＼／：＊");
	}

	public static String filterUrl(String in)
	{
		return StringUtils.trim(in).replace(" ", "%20");
	}
}

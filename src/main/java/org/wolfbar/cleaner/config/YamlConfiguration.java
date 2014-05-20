package org.wolfbar.cleaner.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

public class YamlConfiguration extends MapConfiguration implements FileConfiguration
{
	private Yaml yaml;
	private File file;
	
	
	public YamlConfiguration()
	{
		super();
		initialize();
	}
	
	public YamlConfiguration(MapConfiguration config)
	{
		super(config.getRoot());
		initialize();
	}
	
	public YamlConfiguration(File file)
	{
		setFile(file);
		initialize();
	}
	
	private void initialize()
	{
		DumperOptions options = new DumperOptions();
		options.setIndent(2);
		options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		
		yaml = new Yaml(new SafeConstructor(), new Representer(), options);
	}
	
	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void read(InputStream in)
	{
		Object obj = yaml.load(in);
		if (obj instanceof Map) setRoot((Map<String, Object>) obj);
		else setRoot(new HashMap<String, Object>());
	}
	
	@Override
	public void write(OutputStream out)
	{
		Writer writer = new OutputStreamWriter(out);
		yaml.dump(getRoot(), writer);
	}
	
	@Override
	public void setFile(File file)
	{
		this.file = file;
	}
	
	@Override
	public File getFile()
	{
		return file;
	}
	
	@Override
	public void save()
	{
		try
		{
			OutputStream out = new FileOutputStream(file);
			write(out);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void load()
	{
		InputStream in;
		try
		{
			in = new FileInputStream(file);
			read(in);
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}
}

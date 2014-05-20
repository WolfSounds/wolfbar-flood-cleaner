package org.wolfbar.cleaner.config;

import java.util.Collection;
import java.util.List;

public interface Configuration
{
	public static final class ConfigurationPair
	{
		private String path;
		private Object value;
		
		public ConfigurationPair(String path, Object value)
		{
			this.path = path;
			this.value = value;
		}
		
		public String getPath()
		{
			return path;
		}
		
		public Object getValue()
		{
			return value;
		}
	}
	
	
	boolean contains(String path);
	
	Object get(String path);
	void set(String path, Object value);
	
	Configuration getSection(String path);
	
	Collection<String> getKeyList();
	Collection<String> getKeyList(String path);
	
	void setDefault(String path, Object value);	
	Object getDefault(String path);
	
	String getString(String path);
	String getString(String path, String def);
	void setString(String path, Object value);
	boolean isString(String path);
	
	int getInt(String path);
	int getInt(String path, int def);
	void setInt(String path, int value);
	boolean isInt(String path);
	
	long getLong(String path);
	long getLong(String path, long def);
	void setLong(String path, long value);
	boolean isLong(String path);
	
	float getFloat(String path);
	float getFloat(String path, float def);
	void setFloat(String path, float value);
	boolean isFloat(String path);
	
	double getDouble(String path);
	double getDouble(String path, double def);
	void setDouble(String path, double value);
	boolean isDouble(String path);
	
	boolean getBoolean(String path);
	boolean getBoolean(String path, boolean def);
	void setBoolean(String path, boolean value);
	boolean isBoolean(String path);
	
	List<?> getList(String path);
	List<?> getList(String path, List<?> def);
	void setList(String path, List<?> value);
	boolean isList(String path);

	List<String> getStringList(String path);
	List<String> getStringList(String path, List<String> def);

	List<Integer> getIntList(String path);
	List<Integer> getIntList(String path, List<Integer> def);

	List<Float> getFloatList(String path);
	List<Float> getFloatList(String path, List<Float> def);

	List<Double> getDoubleList(String path);
	List<Double> getDoubleList(String path, List<Double> def);
	
	List<Boolean> getBooleanList(String path);
	List<Boolean> getBooleanList(String path, List<Boolean> def);
}

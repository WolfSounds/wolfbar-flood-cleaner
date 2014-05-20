package org.wolfbar.cleaner.tieba;

public class Topic
{
	public final long id;
	public final String title;
	public final String author;
	public final int replies;
	public final String shortTime;
	
	public Topic(long id, String title, String author, int replies, String shortTime)
	{
		this.id = id;
		this.title = title;
		this.author = author;
		this.replies = replies;
		this.shortTime = shortTime;
	}
}

package org.wolfbar.cleaner.tieba;

import java.util.ArrayList;
import java.util.List;

public class Post
{
	public final Topic topic;
	public final String tbs;
	public final int forumId;
	public final long id;
	public final int floor;
	public final String author;
	public final int authorLevel;
	public final String date;
	public final String text;
	public final List<Post> subs;
	
	public Post(Topic topic, String tbs, int forumId, long id, int floor, String author, int authorLevel, String date, String text)
	{
		this.topic = topic;
		this.tbs = tbs;
		this.forumId = forumId;
		this.id = id;
		this.floor = floor;
		this.author = author;
		this.authorLevel = authorLevel;
		this.date = date;
		this.text = text;
		this.subs = new ArrayList<>();
	}
}

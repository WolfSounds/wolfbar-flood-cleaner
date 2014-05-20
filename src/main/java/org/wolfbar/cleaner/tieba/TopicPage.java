package org.wolfbar.cleaner.tieba;

import java.util.ArrayList;
import java.util.List;

public class TopicPage
{
	public final Topic topic;
	public final List<Post> posts;
	
	public final int page;
	public final int pages;
	
	public TopicPage(Topic topic, int page, int pages)
	{
		this.topic = topic;
		this.posts = new ArrayList<>();
		
		this.page = page;
		this.pages = pages;
	}
}

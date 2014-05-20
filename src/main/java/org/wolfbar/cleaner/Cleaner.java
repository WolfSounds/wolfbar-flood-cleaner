package org.wolfbar.cleaner;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.naming.TimeLimitExceededException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.wolfbar.cleaner.console.ConsoleController;
import org.wolfbar.cleaner.http.DocumentUtils;
import org.wolfbar.cleaner.http.DocumentUtils.PageGetter;
import org.wolfbar.cleaner.tieba.BarUtils;
import org.wolfbar.cleaner.tieba.Post;
import org.wolfbar.cleaner.tieba.Topic;
import org.wolfbar.cleaner.tieba.TopicPage;

public class Cleaner
{
	private static final String NAME = "狼吧洪水清理器";


	public static void main(String[] args) throws Exception
	{
		Utils.installLogger();
		System.out.printf("%s by WolfSounds.\n\n", NAME);

		Cleaner defencer = new Cleaner();

		ConsoleController control = new ConsoleController();
		control.startup();
		control.addCommand('s', () ->
		{
			defencer.stop();
		});

		defencer.run();
		control.shutdown();
	}


	private Config config;

	private HttpClientContext clientContext;
	private PageGetter pageGetter;

	private Map<Long, Topic> checkedTopics;
	private Set<String> bannedUsers;
	private OutputStream banOutput;

	private Set<Long> checkedPosts;
	private BlockingDeque<Post> deleteQueue;

	private boolean isDefenceMode;
	private LocalDateTime lastDefenceTime;

	private long gets = 0;
	private long deletes = 0;
	private long bans = 0;
	private long retries = 0;
	private long errors = 0;


	public Cleaner()
	{
		config = new Config();
		if (StringUtils.isBlank(config.baiduId) || StringUtils.isBlank(config.baiduUss))
		{
			System.out.println("无登录信息可用。");
			System.exit(0);
		}

		if (StringUtils.isBlank(config.barName) || config.banKeyWords.isEmpty())
		{
			System.out.println("未设定目标吧，或未设定关键字。");
			System.exit(0);
		}

		System.out.printf("工作线程数: %d\n", config.threads);
		System.out.printf("目标贴吧: %s\n", config.barName);
		for (String word : config.banKeyWords)
		{
			if (StringUtils.isBlank(word))
			{
				System.out.println("关键字不能为空。");
				System.exit(0);
			}

			// System.out.printf("关键字: %s\n", word);
		}
		System.out.println();

		clientContext = new HttpClientContext(config.maxTotalConnections, config.maxPerRouterConnections, config.threads + 1, config.socketTimeoutTime, config.connectTimeoutTime, config.BufferSizeKb);

		BasicClientCookie baiduIdCookie = new BasicClientCookie("BAIDUID", config.baiduId);
		baiduIdCookie.setDomain(".baidu.com");
		baiduIdCookie.setPath("/");
		clientContext.cookieStore.addCookie(baiduIdCookie);

		BasicClientCookie baiduUssCookie = new BasicClientCookie("BDUSS", config.baiduUss);
		baiduUssCookie.setDomain(".baidu.com");
		baiduUssCookie.setPath("/");
		clientContext.cookieStore.addCookie(baiduUssCookie);

		pageGetter = DocumentUtils.pageGetter(clientContext.client, (url, statusCode) ->
		{
			errors++;
			return true;
		}, (url) ->
		{
			gets++;
			// System.out.printf("获取 %s ...\n", url);
		}, (url) ->
		{
			retries++;
			return true;
		});

		try
		{
			checkedTopics = Collections.synchronizedMap(new HashMap<>());
			bannedUsers = Collections.synchronizedSet(new HashSet<>());
			banOutput = new FileOutputStream("ban.txt", true);

			checkedPosts = Collections.synchronizedSet(new HashSet<>());
			deleteQueue = new LinkedBlockingDeque<>();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public boolean checkTopics(Collection<Topic> topics) throws IOException
	{
		for (Topic topic : topics)
		{
			Topic old = checkedTopics.get(topic.id);
			if (old != null && !StringUtils.isBlank(old.shortTime) && old.shortTime.equals(topic.shortTime)) continue;

			Runnable topicTask = () ->
			{
				TopicPage firstPage = BarUtils.getTopicPage(pageGetter, topic, 0);
				for (int nowPage=0; nowPage < firstPage.pages; nowPage++)
				{
					final int curPage = nowPage+1;
					Runnable task = () ->
					{
						try
						{
							TopicPage page;

							if (curPage == 1) page = firstPage;
							else page = BarUtils.getTopicPage(pageGetter, topic, curPage);

							List<Post> allPosts = new ArrayList<>();
							allPosts.addAll(page.posts);
							for (Post post : page.posts) allPosts.addAll(post.subs);

							for (Post post : allPosts)
							{
								if (checkedPosts.contains(post.id)) continue;
								checkedPosts.add(post.id);

								for (String word : config.banKeyWords)
								{
									if (!post.text.contains(word)) continue;
									if (isDefenceMode == false)
									{
										isDefenceMode = true;
										System.out.println("进入积极防御模式。");
									}

									lastDefenceTime = LocalDateTime.now();

									deleteQueue.offer(post);

									System.out.printf("╔ 回复者: %s, 等级: %d, 时间: %s, 包含内容: %s\n", post.author, post.authorLevel, post.date, StringUtils.abbreviate(word, 10));
									System.out.printf("╚ 所在帖子: %s, 作者: %s, 回复数: %s, 楼层: %d\n", StringUtils.abbreviate(topic.title, 20), topic.author, topic.replies, post.floor);

									break;
								}
							}
						}
						catch (RejectedExecutionException e)
						{
							e.printStackTrace();
							return;
						}
					};

					clientContext.executor.execute(task);
				}

				checkedTopics.put(topic.id, topic);
			};

			clientContext.executor.execute(topicTask);
		}

		return false;
	}

	private Runnable deleteThread = () ->
	{
		while (!clientContext.executor.isTerminating() && !clientContext.executor.isShutdown())
		{
			Post post = null;

			try
			{
				post = deleteQueue.poll(100, TimeUnit.MILLISECONDS);
				if (post == null) continue;

				if (bannedUsers.contains(post.author))
				{
					if (BarUtils.delete(clientContext.client, post, config.barName) == false) throw new TimeLimitExceededException();
					deletes++;
				}
				else
				{
					if (BarUtils.ban(clientContext.client, post))
					{
						bannedUsers.add(post.author);
						bans++;

						banOutput.write((post.author + ", ").getBytes("utf-8"));

						if (BarUtils.delete(clientContext.client, post, config.barName) == false) throw new TimeLimitExceededException();
						deletes++;
					}
					else throw new TimeLimitExceededException();;
				}
			}
			catch (InterruptedException e)
			{
				return;
			}
			catch (IOException e)
			{
				e.printStackTrace();
				continue;
			}
			catch (TimeLimitExceededException e)
			{
				try { Thread.sleep(5000); } catch (InterruptedException ex) { return; }
				if (post != null) deleteQueue.offerFirst(post);
				continue;
			}
		}
	};

	public void run() throws InterruptedException
	{
		clientContext.executor.execute(deleteThread);
		lastDefenceTime = LocalDateTime.MIN;

		boolean isFirst = true;
		System.out.println("执行初次扫描……");

		while (!clientContext.executor.isTerminating() && !clientContext.executor.isShutdown())
		{
			start(5);
			if (isDefenceMode) Thread.sleep(60000);
			else for (int i=0; i<5 && isDefenceMode == false; i++) Thread.sleep(60000);

			while (clientContext.executor.getActiveCount() + clientContext.executor.getQueue().size() > 1)
			{
				int tasks = clientContext.executor.getActiveCount() + clientContext.executor.getQueue().size();
				System.out.printf("已扫描 %d 个主题，已检查帖子 %d 个，已获取页面 %d 个，总完成任务 %d 个，目前任务 %d 个，已删除帖子 %d 条，已封禁账号 %d 个，累计获取重试 %d 次，累计网络错误 %d 次。\n",
					checkedTopics.size(), checkedPosts.size(), gets, clientContext.executor.getCompletedTaskCount(), tasks, deletes, bans, retries, errors);
				Thread.sleep(60000);
			}

			System.out.printf("已扫描 %d 个主题，已检查帖子 %d 个，已获取页面 %d 个，总完成任务 %d 个，已删除帖子 %d 条，已封禁账号 %d 个，累计获取重试 %d 次，累计网络错误 %d 次。\n",
				checkedTopics.size(), checkedPosts.size(), gets, clientContext.executor.getCompletedTaskCount(), deletes, bans, retries, errors);

			if (isDefenceMode == true && lastDefenceTime.plusMinutes(10).isBefore(LocalDateTime.now()))
			{
				isDefenceMode = false;
				System.out.println("退出积极防御模式。");
			}

			if (isFirst) System.out.println("初次扫描完成，开始进行增量更新扫描。");
			isFirst = false;
		}
	}

	public boolean start(int pages)
	{
		try
		{
			for (int i=0; i<pages; i++)
			{
				List<Topic> topics = BarUtils.getTopicList(pageGetter, config.barName, i * 50);
				checkTopics(topics);
			}
		}
		catch (RejectedExecutionException | IOException e)
		{
			System.out.println("操作已取消。");
			e.printStackTrace();
			return false;
		}

		return false;
	}

	public void stop()
	{
		try
		{
			System.out.printf("已取消 %d 项动作，正在等待 %d 项动作完成……\n", clientContext.executor.getQueue().size(), clientContext.executor.getActiveCount());
			clientContext.executor.getQueue().clear();
			clientContext.executor.shutdown();
			while (!clientContext.executor.isTerminated()) clientContext.executor.awaitTermination(10, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}

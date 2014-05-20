package org.wolfbar.cleaner.tieba;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.wolfbar.cleaner.http.DocumentUtils.PageGetter;

import com.cedarsoftware.util.io.JsonReader;

public class BarUtils
{
	public static final String ADDRESS = "tieba.baidu.com";
	public static final String URL = "http://" + ADDRESS + "/";
	
	
	public static String getUrl(String path)
	{
		return URL + path;
	}
	
	public static String getUrl(String pathFormat, Object ...args)
	{
		return URL + String.format(pathFormat, args);
	}
	
	public static boolean delete(HttpClient client, Post post, String barName)
	{
		if (StringUtils.isEmpty(post.tbs) || post.forumId == 0)
		{
			System.out.println("> 帖子上下文数据不正确，无法封禁。");
			return false;
		}
		
		HttpPost request = null;
		HttpResponse response;
		
		try
		{
			request = new HttpPost(URL + "f/commit/post/delete");
			
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("commit_fr", "pb"));
			params.add(new BasicNameValuePair("ie", "utf-8"));
			params.add(new BasicNameValuePair("tbs", post.tbs));
			params.add(new BasicNameValuePair("kw", barName));
			params.add(new BasicNameValuePair("fid", Integer.toString(post.forumId)));
			params.add(new BasicNameValuePair("tid", Long.toString(post.topic.id)));
			params.add(new BasicNameValuePair("is_vipdel", "0"));
			params.add(new BasicNameValuePair("pid", Long.toString(post.id)));
			params.add(new BasicNameValuePair("is_finf", "false"));
			
			UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(params, "utf-8");
			request.setEntity(postEntity);
			
			// System.out.printf("> 删除 %s 的回帖 ...\n", post.author);
			response = client.execute(request);
			
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				System.out.printf("> 删除 %s 的回帖失败。\n", post.author);
				return false;
			}
			
			String result = StringEscapeUtils.unescapeJson(EntityUtils.toString(response.getEntity(), "utf-8"));
			if (!StringUtils.isEmpty(result)) System.out.printf("> 删除 %s 回帖: ", post.author, result);
			
			if (!result.contains("\"err_code\":0")) return false;
			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			if (request != null) request.abort();
		}
		
		return false;
	}
	
	public static boolean ban(HttpClient client, Post post)
	{
		if (StringUtils.isEmpty(post.tbs) || post.forumId == 0)
		{
			System.out.println("> 帖子上下文数据不正确，无法封禁。");
			return false;
		}
		
		HttpPost request = null;
		HttpResponse response;
		
		try
		{
			request = new HttpPost(URL + "pmc/blockid");
			
			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("user_name[]", post.author));
			params.add(new BasicNameValuePair("day", "1"));
			params.add(new BasicNameValuePair("fid", Integer.toString(post.forumId)));
			params.add(new BasicNameValuePair("tbs", post.tbs));
			params.add(new BasicNameValuePair("pid", Long.toString(post.id)));
			params.add(new BasicNameValuePair("ie", "utf-8"));
			params.add(new BasicNameValuePair("reason", "恶意刷屏、挖坟、水贴、抢楼，给予封禁处罚。"));
			UrlEncodedFormEntity postEntity = new UrlEncodedFormEntity(params, "utf-8");
			request.setEntity(postEntity);
			
			// System.out.printf("> 封禁 %s ...\n", post.author);
			response = client.execute(request);
			
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200)
			{
				System.out.printf("> 封禁 %s 失败。\n", post.author);
				return false;
			}
			
			String result = StringEscapeUtils.unescapeJson(EntityUtils.toString(response.getEntity(), "gbk"));
			System.out.printf("> 封禁 %s 结果: %s\n", post.author, result);
			
			if (result.contains("成功")) return true;
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		finally
		{
			if (request != null) request.abort();
		}
		
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public static List<Topic> getTopicList(PageGetter getter, String barName, int start) throws IOException
	{
		String url = start == 0 ? getUrl("f?ie=utf-8&kw=%s", barName) : getUrl("f?ie=utf-8&kw=%s&pn=%d", barName, start);
		Document page = getter.get(url);
		
		Element tbAdminManageElement = page.select(".tbAdminManage").first();
		if (tbAdminManageElement == null)
		{
			System.out.println("* 登录信息过期，或者不是小吧/吧主！");
			System.exit(0);
		}
		
		List<Topic> topics = new ArrayList<>();
		
		Elements posts = page.select(".j_thread_list");
		
		Element bigrenderElement = page.select(".threadlist_bright code#frslistCodeArea").first();
		if (bigrenderElement != null)
		{
			String bigrender = bigrenderElement.html();
			Document documentBigrender = Jsoup.parse(bigrender.substring(5, bigrender.length()-3));
			Elements postsBigrender = documentBigrender.select(".j_thread_list");
			posts.addAll(postsBigrender);
		}
		
		for (Element post : posts)
		{
			Element titleElement = post.select(".threadlist_title a").first();
			String title = StringUtils.abbreviate(titleElement.text(), 20);
							
			String dataField = post.attr("data-field");
			if (StringUtils.isBlank(dataField)) continue;
			Map<String, Object> dataFieldMap = (Map<String, Object>) JsonReader.jsonToMaps(dataField);
			
			String authorName = dataFieldMap.get("author_name").toString();
			long tid = NumberUtils.toLong(dataFieldMap.get("id").toString());
			int replyNum = NumberUtils.toInt(dataFieldMap.get("reply_num").toString());
			
			Element replyDateElement = post.select(".threadlist_reply_date").first();
			String shortTime = replyDateElement == null ? "" : replyDateElement.text().trim();
			
			topics.add(new Topic(tid, title, authorName, replyNum, shortTime));
		}
		
		return topics;
	}
	
	@SuppressWarnings("unchecked")
	public static TopicPage getTopicPage(PageGetter getter, Topic topic, int page)
	{
		if (page <= 0) page = 1;
		
		try
		{
			String url = page == 1 ? getUrl("p/%d", topic.id) : getUrl("p/%d?pn=%d", topic.id, page);
			Document pageDocument = getter.get(url);
			
			String tbs = "";
			int forumId = 0;
			
			Elements scriptElements = pageDocument.select("head > script");
			for (Element element : scriptElements)
			{
				String code = element.html();
				if (!code.startsWith("var PageData =")) continue;
				
				String keyWord = "'tbs'  : \"";
				int start = code.indexOf(keyWord, 0) + keyWord.length();
				int end = code.indexOf('\"', start);
				
				if(start == -1 || end == -1 || end - start > 32) continue;
				
				tbs = code.substring(start, end);
				break;
			}
			
			scriptElements = pageDocument.select("body div script");
			for (Element element : scriptElements)
			{
				String code = element.html();
				if (!code.startsWith("var commonPageData =")) continue;
				
				String keyWord = "forum_id:";
				int start = code.indexOf(keyWord, 0) + keyWord.length();
				int end = code.indexOf(',', start);
				
				if (start == -1 || end == -1 || end - start > 10) continue;
				
				String data = code.substring(start, end);
				if (!NumberUtils.isDigits(data)) continue;
				
				forumId = NumberUtils.toInt(data);
				break;
			}
			
			Elements posts = pageDocument.select("div.l_post");
			Element bigrenderElement = pageDocument.select("textarea#pblistCodeArea.bigrendertextarea").first();
			if (bigrenderElement != null)
			{
				String bigrender = pageDocument.select("textarea#pblistCodeArea.bigrendertextarea").first().text();
				Document documentBigrender = Jsoup.parse(bigrender);
				Elements postsBigrender = documentBigrender.select("div.l_post");
				posts.addAll(postsBigrender);
			}
			
			int pages = NumberUtils.toInt(pageDocument.select(".l_reply_num span:nth-child(2)").first().text());
			
			TopicPage topicPage = new TopicPage(topic, page, pages);
			
			for (Element element : posts)
			{
				String dataField = element.attr("data-field");
				Map<String, Object> dataFieldMap = (Map<String, Object>) JsonReader.jsonToMaps(dataField);
				
				long postId = NumberUtils.toLong(((Map<String, Object>) dataFieldMap.get("content")).get("post_id").toString());
				int floor = NumberUtils.toInt(((Map<String, Object>) dataFieldMap.get("content")).get("post_no").toString());
				String authorName = Objects.toString(((Map<String, Object>) dataFieldMap.get("author")).get("user_name"), "?");
				int lv = NumberUtils.toInt(Objects.toString(((Map<String, Object>) dataFieldMap.get("author")).get("level_id"), "0"));
				String date = ((Map<String, Object>) dataFieldMap.get("content")).get("date").toString();
				String postText = element.select(".d_post_content_main .d_post_content").first().text();
				
				Post post = new Post(topic, tbs, forumId, postId, floor, authorName, lv, date, postText);
				topicPage.posts.add(post);
				
				int commentPage = 1;
				while (true)
				{
					String commentUrl = getUrl("p/comment?tid=%d&pid=%d&pn=%d", topic.id, post.id, commentPage);
					Document commentDoc = getter.get(commentUrl);
					
					Elements comments = commentDoc.select("li.lzl_single_post");
					if (comments.isEmpty()) break;
					
					for (Element comment : comments)
					{
						String subDataField = comment.attr("data-field");
						Map<String, Object> subDataFieldMap = (Map<String, Object>) JsonReader.jsonToMaps(subDataField);
						
						long subPostId = NumberUtils.toLong(subDataFieldMap.get("spid").toString());
						
						String name = comment.select(".j_user_card").text();
						String text = comment.select(".lzl_content_main").text();
						
						Post subPost = new Post(topic, tbs, forumId, subPostId, -1, name, 0, "", text);
						post.subs.add(subPost);
					}
					commentPage++;
				}
			}
			
			return topicPage;
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			System.err.println("格式不匹配： " + (page == 1 ? getUrl("p/%d", topic.id) : getUrl("p/%d?pn=%d", topic.id, page)));
		}
		catch (RejectedExecutionException | IOException e)
		{
			e.printStackTrace();
		}

		return null;
	}
}

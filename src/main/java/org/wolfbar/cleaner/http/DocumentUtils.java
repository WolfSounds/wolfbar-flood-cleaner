package org.wolfbar.cleaner.http;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.wolfbar.cleaner.Utils;

public class DocumentUtils
{
	@FunctionalInterface
	public interface PageGetter
	{
		Document get(String url) throws RequestAbortedException;
	}
	
	public interface GetFailedHandler extends BiFunction<String, Integer, Boolean> {};

	public static PageGetter pageGetter(HttpClient client, GetFailedHandler failedHandler)
	{
		return (url) -> getPage(client, url, failedHandler);
	}
	
	public static PageGetter pageGetter(HttpClient client, GetFailedHandler failedHandler, Consumer<String> startHandler, Function<String, Boolean> retryHandler)
	{
		return (url) -> getPage(client, url, failedHandler, startHandler, retryHandler);
	}
	
	public static Document getPage(HttpClient client, String url, GetFailedHandler failedHandler) throws RequestAbortedException
	{
		return getPage(client, url, failedHandler, (u) ->
		{
			System.out.printf("获取 %s ...\n", u);
		}, (u) ->
		{
			System.out.printf("重新获取 %s ...\n", u);
			return true;
		});
	}
	
	public static Document getPage
	(HttpClient client, String url, GetFailedHandler failedHandler, Consumer<String> startHandler, Function<String, Boolean> retryHandler)
	throws RequestAbortedException
	{
		HttpGet request = null;
		HttpResponse response;
		String htmlContext;
		
		boolean isFirst = true;
		while (true)
		{
			try
			{
				request = new HttpGet(Utils.filterUrl(url));
				
				if (isFirst) startHandler.accept(url);
				else if (!retryHandler.apply(url)) return null;
				
				response = client.execute(request);
				isFirst = false;
				
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != HttpStatus.SC_OK)
				{
					boolean retry = failedHandler.apply(url, statusCode);
					if (retry) continue;
					return null;
				}
				
				HttpEntity entity = response.getEntity();
				htmlContext = EntityUtils.toString(entity);
				break;
			}
			catch (RequestAbortedException e)
			{
				throw e;
			}
			catch (IOException e)
			{
				continue;
			}
			finally
			{
				if (request != null) request.abort();
			}
		}
		
		return Jsoup.parse(htmlContext);
	}
}

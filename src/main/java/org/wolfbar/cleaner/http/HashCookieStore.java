package org.wolfbar.cleaner.http;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.annotation.GuardedBy;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

@ThreadSafe
public class HashCookieStore implements CookieStore, Serializable
{
	private static final long serialVersionUID = 415711683780784657L;
	
	
	@GuardedBy("this")
	private final Map<String, Map<String, Cookie>> cookieMaps;
	
	
	public HashCookieStore()
	{
		this.cookieMaps = new HashMap<>();
	}
	
	/**
	 * Adds an {@link Cookie HTTP cookie}, replacing any existing equivalent cookies. If the given cookie has already expired it will not be added, but existing values will still be removed.
	 * 
	 * @param cookie the {@link Cookie cookie} to be added
	 * 
	 * @see #addCookies(Cookie[])
	 * 
	 */
	public synchronized void addCookie(Cookie cookie)
	{
		if (cookie != null)
		{
			// first remove any old cookie that is equivalent
			Map<String, Cookie> cookieMap = cookieMaps.get(cookie.getDomain());
			if (cookieMap == null)
			{
				cookieMap = new HashMap<>();
				cookieMaps.put(cookie.getDomain(), cookieMap);
			}
			
			cookieMap.remove(cookie.getName());
			if (!cookie.isExpired(new Date()))
			{
				cookieMap.put(cookie.getName(), cookie);
			}
		}
	}
	
	/**
	 * Adds an array of {@link Cookie HTTP cookies}. Cookies are added individually and in the given array order. If any of the given cookies has already expired it will not be added, but existing values will still be removed.
	 * 
	 * @param cookies the {@link Cookie cookies} to be added
	 * 
	 * @see #addCookie(Cookie)
	 * 
	 */
	public synchronized void addCookies(Cookie[] cookies)
	{
		if (cookies != null)
		{
			for (Cookie cooky : cookies)
			{
				this.addCookie(cooky);
			}
		}
	}
	
	/**
	 * Returns an immutable array of {@link Cookie cookies} that this HTTP state currently contains.
	 * 
	 * @return an array of {@link Cookie cookies}.
	 */
	public synchronized List<Cookie> getCookies()
	{
		// create defensive copy so it won't be concurrently modified
		List<Cookie> cookies = new ArrayList<Cookie>();
		
		for (Map<String, Cookie> map : cookieMaps.values())
		{
			for (Cookie cookie : map.values())
			{
				cookies.add(cookie);
			}
		}
		
		return cookies;
	}
	
	/**
	 * Removes all of {@link Cookie cookies} in this HTTP state that have expired by the specified {@link java.util.Date date}.
	 * 
	 * @return true if any cookies were purged.
	 * 
	 * @see Cookie#isExpired(Date)
	 */
	public synchronized boolean clearExpired(final Date date)
	{
		if (date == null) { return false; }
		boolean removed = false;
		for (Iterator<Map<String, Cookie>> it = cookieMaps.values().iterator(); it.hasNext();)
		{
			Map<String, Cookie> map = it.next();
			
			for (Iterator<Cookie> itMap = map.values().iterator(); itMap.hasNext();)
			if (itMap.next().isExpired(date))
			{
				itMap.remove();
				removed = true;
			}
			
			if(map.size() == 0) it.remove();
		}
		return removed;
	}
	
	/**
	 * Clears all cookies.
	 */
	public synchronized void clear()
	{
		cookieMaps.clear();
	}
	
	public synchronized Cookie getCookie(String domain, String name)
	{
		Map<String, Cookie> map = cookieMaps.get(domain);
		if (map == null) return null;
		
		return map.get(name);
	}
	
	@Override
	public synchronized String toString()
	{
		return cookieMaps.toString();
	}
}

package org.wolfbar.cleaner;

import java.util.regex.Pattern;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

public class ElementUtils
{
	private static final Pattern WHITESPACE_BLOCK = Pattern.compile("[ \\t\\x0B\\f]+");
	
	
	public static String text(Element element)
	{
		final StringBuilder accum = new StringBuilder();
		new NodeTraversor(new NodeVisitor()
		{
			public void head(Node node, int depth)
			{
				if (node instanceof TextNode)
				{
					TextNode textNode = (TextNode) node;
					String str = textNode.getWholeText();
					str = WHITESPACE_BLOCK.matcher(str).replaceAll(" ");
					accum.append(str);
				}
			}
			
			public void tail(Node node, int depth)
			{
			}
		}).traverse(element);
		return accum.toString().trim();
	}
}

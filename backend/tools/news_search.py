import feedparser
import urllib.parse
from typing import List, Dict, Any

def search_google_news(query: str, max_results: int = 5) -> List[Dict[str, Any]]:
    """Searches Google News RSS feed for articles matching a given topic or query.

    Args:
        query: The search term or topic to query news for, e.g. "Fukushima water release" or "room temperature superconductor".
        max_results: Maximum number of news articles to return (default 5).

    Returns:
        List of dictionaries containing title, publisher, link, and published date.
    """
    encoded_query = urllib.parse.quote(query)
    rss_url = f"https://news.google.com/rss/search?q={encoded_query}&hl=en-US&gl=US&ceid=US:en"
    
    feed = feedparser.parse(rss_url)
    results = []
    
    for entry in feed.entries[:max_results]:
        source_title = entry.source.title if hasattr(entry, 'source') and hasattr(entry.source, 'title') else "News Source"
        results.append({
            "title": entry.title,
            "publisher": source_title,
            "url": entry.link,
            "date": getattr(entry, 'published', 'Recent')
        })
        
    return results

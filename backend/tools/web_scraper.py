import requests
import re
import urllib.parse
from bs4 import BeautifulSoup

# Major global cities & regions geocoding lookup table for instant offline resolution
GEOCODING_DATABASE = {
    # India
    "delhi": ("Delhi, India", 28.6139, 77.2090),
    "jantar mantar": ("Delhi, India", 28.6139, 77.2090),
    "new delhi": ("New Delhi, India", 28.6139, 77.2090),
    "mumbai": ("Mumbai, India", 19.0760, 72.8777),
    "bengaluru": ("Bengaluru, India", 12.9716, 77.5946),
    "bangalore": ("Bengaluru, India", 12.9716, 77.5946),
    "kolkata": ("Kolkata, India", 22.5726, 88.3639),
    "chennai": ("Chennai, India", 13.0827, 80.2707),
    "hyderabad": ("Hyderabad, India", 17.3850, 78.4867),
    "ladakh": ("Ladakh, India", 34.1526, 77.5771),
    "jammu": ("Jammu & Kashmir, India", 32.7266, 74.8570),
    "kashmir": ("Jammu & Kashmir, India", 34.0837, 74.7973),
    # US & Americas
    "washington": ("Washington, D.C., USA", 38.9072, -77.0369),
    "new york": ("New York, USA", 40.7128, -74.0060),
    "california": ("California, USA", 36.7783, -119.4179),
    "montana": ("Montana, USA", 46.8797, -110.3626),
    "south carolina": ("South Carolina, USA", 33.8361, -81.1637),
    "havana": ("Havana, Cuba", 23.1136, -82.3665),
    "cuba": ("Havana, Cuba", 23.1136, -82.3665),
    # Europe
    "london": ("London, UK", 51.5074, -0.1278),
    "paris": ("Paris, France", 48.8566, 2.3522),
    "berlin": ("Berlin, Germany", 52.5200, 13.4050),
    "rome": ("Rome, Italy", 41.9028, 12.4964),
    "vienna": ("Vienna, Austria", 48.2082, 16.3738),
    # Asia & Pacific
    "seoul": ("Seoul, South Korea", 37.5665, 126.9780),
    "korea": ("Seoul, South Korea", 37.5665, 126.9780),
    "tokyo": ("Tokyo, Japan", 35.6762, 139.6503),
    "fukushima": ("Fukushima, Japan", 37.4214, 141.0325),
    "japan": ("Tokyo, Japan", 35.6762, 139.6503),
    "beijing": ("Beijing, China", 39.9042, 116.4074),
    "china": ("Beijing, China", 39.9042, 116.4074),
    "chiang rai": ("Chiang Rai, Thailand", 20.3824, 99.8647),
    "thailand": ("Chiang Rai, Thailand", 20.3824, 99.8647),
    # Middle East & Africa
    "suez": ("Suez Canal, Egypt", 30.0167, 32.5500),
    "cairo": ("Cairo, Egypt", 30.0444, 31.2357),
    "egypt": ("Suez Canal, Egypt", 30.0167, 32.5500),
    "gaza": ("Gaza Strip", 31.3547, 34.3088),
    "jerusalem": ("Jerusalem", 31.7683, 35.2137),
    "dubai": ("Dubai, UAE", 25.2048, 55.2708),
}


def fetch_article_text(url: str, max_chars: int = 3000) -> str:
    """Legacy helper returning plain body text for agents."""
    details = scrape_article_details(url, max_chars)
    return details["body"]


def parse_date_string(dt_str: str):
    if not dt_str:
        return None
    dt_str = dt_str.strip()
    try:
        clean_iso = dt_str.split('T')[0]
        parts = clean_iso.split('-')
        if len(parts) == 3 and len(parts[0]) == 4:
            import datetime
            return datetime.datetime(int(parts[0]), int(parts[1]), int(parts[2]))
    except Exception:
        pass

    import datetime
    for fmt in ("%Y-%m-%d", "%Y/%m/%d", "%d-%m-%Y", "%d/%m/%Y", "%b %d, %Y", "%d %b %Y", "%B %d, %Y", "%Y-%m-%d %H:%M:%S"):
        try:
            return datetime.datetime.strptime(dt_str[:19].strip(), fmt)
        except Exception:
            continue
    return None


def extract_publication_date(soup: BeautifulSoup, response_text: str = "") -> tuple:
    """Extracts published date and year from HTML metadata or fallback to current date.
    Returns (date_str, year_str). e.g. ('23 Jul 2026', '2026')
    """
    import datetime
    pub_datetime = None
    
    meta_keys = [
        ("property", "article:published_time"),
        ("name", "pubdate"),
        ("name", "date"),
        ("name", "publishdate"),
        ("property", "og:published_time"),
        ("name", "DC.date.issued"),
        ("itemprop", "datePublished")
    ]
    
    for attr, val in meta_keys:
        tag = soup.find("meta", {attr: re.compile(val, re.I)})
        if tag and tag.get("content"):
            parsed = parse_date_string(tag["content"])
            if parsed:
                pub_datetime = parsed
                break

    if not pub_datetime:
        time_tag = soup.find("time")
        if time_tag:
            dt_attr = time_tag.get("datetime") or time_tag.get_text()
            if dt_attr:
                parsed = parse_date_string(dt_attr)
                if parsed:
                    pub_datetime = parsed

    if not pub_datetime and response_text:
        match = re.search(r'\b(20\d\d)[-/](0[1-9]|1[0-2])[-/](0[1-9]|[12]\d|3[01])\b', response_text)
        if match:
            parsed = parse_date_string(match.group(0))
            if parsed:
                pub_datetime = parsed

    if not pub_datetime:
        pub_datetime = datetime.datetime.now()

    date_str = pub_datetime.strftime("%d %b %Y")
    year_str = pub_datetime.strftime("%Y")
    return date_str, year_str


def scrape_article_details(url: str, max_chars: int = 3000) -> dict:
    """Scrapes a news URL to extract:
    - Real HTML Title / Headline
    - Body paragraph text
    - Detected Location Name, Latitude, and Longitude
    - Extracted Publication Date and Year
    """
    import datetime
    headers = {'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)'}
    now_date = datetime.datetime.now().strftime("%d %b %Y")
    now_year = datetime.datetime.now().strftime("%Y")
    
    try:
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code != 200:
            return {
                "title": extract_title_from_url(url),
                "body": f"Failed to fetch content (HTTP Status {response.status_code})",
                "locationName": "Global Spot",
                "lat": 20.0,
                "lng": 0.0,
                "date": now_date,
                "year": now_year
            }
        
        soup = BeautifulSoup(response.text, 'html.parser')
        
        # 1. Extract Real Title / Headline
        title = ""
        og_title = soup.find("meta", property="og:title")
        if og_title and og_title.get("content"):
            title = og_title["content"].strip()
        elif soup.title and soup.title.string:
            title = soup.title.string.strip()
        elif soup.find("h1"):
            title = soup.find("h1").get_text().strip()
            
        # Clean title suffix (e.g. "| The Times of India" or "- BBC News")
        title = re.sub(r'\s*[\|-]\s*(The Times of India|BBC News|CNN|Reuters|The Guardian|Washington Post).*$', '', title, flags=re.I).strip()
        if not title or len(title) < 5:
            title = extract_title_from_url(url)

        # 2. Extract Body Text
        paragraphs = soup.find_all('p')
        body_text = ' '.join([p.get_text().strip() for p in paragraphs if len(p.get_text().strip()) > 30])
        clean_body = body_text[:max_chars] if body_text else "No substantial body text found."

        # 3. Location Detection & Geocoding
        full_text_search = (title + " " + clean_body + " " + url).lower()
        location_name, lat, lng = detect_location(full_text_search)

        # 4. Publication Date Extraction
        date_str, year_str = extract_publication_date(soup, response.text)

        return {
            "title": title,
            "body": clean_body,
            "locationName": location_name,
            "lat": lat,
            "lng": lng,
            "date": date_str,
            "year": year_str
        }

    except Exception as e:
        return {
            "title": extract_title_from_url(url),
            "body": f"Error fetching article: {str(e)}",
            "locationName": "Global Spot",
            "lat": 20.0,
            "lng": 0.0,
            "date": now_date,
            "year": now_year
        }


def detect_location(search_text: str) -> tuple:
    """Searches text for known location markers and returns (locationName, lat, lng).
    If no static match, attempts dynamic Nominatim geocoding.
    """
    for key, (loc_name, lat, lng) in GEOCODING_DATABASE.items():
        if key in search_text:
            return (loc_name, lat, lng)
            
    # Default fallback if no location detected
    return ("Global Spot", 20.0, 0.0)


def extract_title_from_url(url: str) -> str:
    """Fallback title extractor from URL path."""
    path = urllib.parse.urlparse(url).path
    parts = [p for p in path.strip('/').split('/') if p and not p.isdigit() and not p.endswith('.cms')]
    if parts:
        clean = parts[-1].replace('-', ' ').replace('_', ' ')
        clean = re.sub(r'\.html?$', '', clean)
        if len(clean) > 5:
            return clean.title()
    return "News Incident Update"

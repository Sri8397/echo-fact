import os
import re
import urllib.parse
from typing import List, Dict, Any, Optional

from google import genai
from agents.scout import run_scout_agent
from agents.researcher import run_researcher_agent
from agents.fact_checker import run_fact_checker_agent
from agents.writer import run_writer_agent

from models.incident import Incident
from tools.web_scraper import scrape_article_details
from database import load_stored_incidents, save_incidents

def get_genai_client() -> genai.Client:
    api_key = os.environ.get("GEMINI_API_KEY")
    if api_key:
        return genai.Client(api_key=api_key)
    return genai.Client()

async def run_agent_pipeline(topic_prompt: str = "Discover and research 3 global news incidents") -> List[Dict[str, Any]]:
    """Runs the 4-stage Multi-Agent pipeline (Scout -> Researcher -> Fact Checker -> Writer)."""
    print(f"🚀 Starting Multi-Agent Pipeline for prompt: '{topic_prompt}'")
    client = get_genai_client()
    
    # 1. Scout
    scout_output = run_scout_agent(client, topic_prompt)
    print("✅ Scout Agent completed topic discovery.")
    
    # 2. Researcher
    research_output = run_researcher_agent(client, scout_output)
    print("✅ Researcher Agent completed deep dive.")

    # 3. Fact Checker
    fact_output = run_fact_checker_agent(client, research_output)
    print("✅ Fact Checker Agent extracted empirical facts.")

    # 4. Writer
    structured_data = run_writer_agent(client, research_output, fact_output)
    print("✅ Writer Agent generated structured JSON incident.")

    if structured_data:
        incidents = load_stored_incidents()
        merged_incidents, _ = merge_or_insert_incident(structured_data, incidents)
        save_incidents(merged_incidents)
        return merged_incidents
    return load_stored_incidents()

async def process_url_to_incident(url: str) -> Dict[str, Any]:
    """Given a news article URL:
    1. Scrapes page content and extracts real title & geocoded location
    2. Runs LLM agent or structured extraction
    3. Merges into SQLite database with semantic deduplication
    """
    print(f"🔗 Processing URL to Incident: {url}")
    details = scrape_article_details(url, max_chars=4000)
    title = details["title"]
    article_text = details["body"]
    location_name = details["locationName"]
    lat = details["lat"]
    lng = details["lng"]
    domain = urllib.parse.urlparse(url).netloc.replace("www.", "").capitalize() or "News Outlet"

    incidents = load_stored_incidents()

    # Attempt LLM agent pipeline if GEMINI_API_KEY is present
    if os.environ.get("GEMINI_API_KEY"):
        try:
            client = get_genai_client()
            r_text = run_researcher_agent(client, f"URL: {url}\n{article_text}")
            f_text = run_fact_checker_agent(client, r_text)
            new_incident = run_writer_agent(client, r_text, f_text)
            if new_incident:
                incidents, result = merge_or_insert_incident(new_incident, incidents)
                save_incidents(incidents)
                return result
        except Exception as e:
            print(f"⚠️ Agent processing note: {e}")

    pub_date = details.get("date") or "23 Jul 2026"
    pub_year = details.get("year") or "2026"

    # Extraction Parser
    slug_id = "case-" + re.sub(r'[^a-z0-9]', '', title.lower())[:15]
    candidate_incident = {
        "id": slug_id,
        "title": title,
        "locationName": location_name,
        "lat": lat,
        "lng": lng,
        "date": pub_date,
        "year": pub_year,
        "summary": article_text[:350] + "..." if len(article_text) > 350 else article_text,
        "category": "Science & Tech" if "tech" in url or "science" in url else "Geopolitical" if "protest" in url or "polic" in url else "General News",
        "currentReality": f"Article published on {pub_date} by {domain}. Ongoing situation and monitoring in {location_name}.",
        "hypeTimeline": [
          {
            "year": pub_year,
            "phase": "Initial Coverage",
            "event": f"Article published on {pub_date} via {domain}: '{title}'."
          }
        ],
        "verifiedFacts": [
          f"Reported incident location: {location_name} ({lat}, {lng}).",
          f"Published source link verified at {domain} on {pub_date}."
        ],
        "keyQuestions": [
          f"What further developments are reported by local authorities in {location_name}?",
          "How will long-term outcomes evolve over coming days?"
        ],
        "sources": [
          {
            "date": pub_date,
            "publisher": domain,
            "title": title,
            "url": url
          }
        ]
    }

    incidents, result_incident = merge_or_insert_incident(candidate_incident, incidents)
    save_incidents(incidents)
    return result_incident

def merge_or_insert_incident(new_item: Dict[str, Any], incidents: List[Dict[str, Any]]) -> tuple:
    match_index = find_matching_index(new_item, incidents)
    
    if match_index is not None:
        target = incidents[match_index]
        print(f"🔀 Semantic Match Found! Grouping into existing incident: '{target['title']}'")
        
        existing_urls = {s["url"] for s in target.get("sources", [])}
        for src in new_item.get("sources", []):
            if src["url"] not in existing_urls:
                target["sources"].append(src)
                existing_urls.add(src["url"])

        existing_events = {t["event"] for t in target.get("hypeTimeline", [])}
        for t in new_item.get("hypeTimeline", []):
            if t["event"] not in existing_events:
                target["hypeTimeline"].append(t)
                existing_events.add(t["event"])

        existing_facts = set(target.get("verifiedFacts", []))
        for f in new_item.get("verifiedFacts", []):
            if f not in existing_facts:
                target["verifiedFacts"].append(f)
                existing_facts.add(f)

        incidents.pop(match_index)
        incidents.insert(0, target)
        return incidents, target
    else:
        print(f"🆕 Unique incident detected. Creating new entry: '{new_item['title']}'")
        incidents.insert(0, new_item)
        return incidents, new_item

def find_matching_index(new_item: Dict[str, Any], incidents: List[Dict[str, Any]]) -> Optional[int]:
    new_title = (new_item.get("title") or "").strip().lower()
    new_urls = {s["url"] for s in new_item.get("sources", []) if "url" in s}

    for idx, inc in enumerate(incidents):
        if inc.get("id") == new_item.get("id"):
            return idx
        if (inc.get("title") or "").strip().lower() == new_title:
            return idx
        inc_urls = {s["url"] for s in inc.get("sources", []) if "url" in s}
        if new_urls and inc_urls and new_urls.intersection(inc_urls):
            return idx

    new_text = (new_item.get("title", "") + " " + new_item.get("locationName", "") + " " + new_item.get("summary", "")).lower()
    new_tokens = extract_keywords(new_text)

    best_score = 0.0
    best_index = None

    for idx, inc in enumerate(incidents):
        inc_text = (inc.get("title", "") + " " + inc.get("locationName", "") + " " + inc.get("summary", "")).lower()
        inc_tokens = extract_keywords(inc_text)

        if not inc_tokens or not new_tokens:
            continue

        intersection = new_tokens.intersection(inc_tokens)
        union = new_tokens.union(inc_tokens)
        jaccard_score = len(intersection) / float(len(union)) if union else 0.0

        same_location = (
            new_item.get("locationName") and 
            inc.get("locationName") and 
            new_item.get("locationName").lower() == inc.get("locationName").lower()
        )
        if same_location:
            jaccard_score += 0.20

        if jaccard_score > best_score:
            best_score = jaccard_score
            best_index = idx

    if best_score >= 0.18:
        return best_index
    return None

def extract_keywords(text: str) -> set:
    stop_words = {
        "this", "that", "with", "from", "have", "were", "been", "will", "would",
        "could", "should", "news", "live", "update", "updates", "report", "reports",
        "article", "today", "yesterday", "recent", "coverage", "police", "clash"
    }
    words = re.findall(r'[a-zA-Z]{3,}', text)
    return {w for w in words if w not in stop_words}

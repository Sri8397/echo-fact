from google import genai
from google.genai import types

def run_researcher_agent(client: genai.Client, candidate_topics: str) -> str:
    """Researcher Agent: deep-dives incident timelines, URLs, and summaries."""
    system_instruction = (
        "You are a Research Journalist Agent. Given candidate topics: "
        "1. Search for news articles spanning from initial event to current status. "
        "2. Extract real source URLs, publisher names, titles, and dates. Never invent or hallucinate URLs. "
        "3. Construct a chronological event timeline (year, phase, event description). "
        "4. Summarize what happened when it occurred, and where it stands today."
    )
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=f"Research these candidate topics and gather source links + timeline:\n{candidate_topics}",
        config=types.GenerateContentConfig(
            system_instruction=system_instruction
        )
    )
    return response.text or ""

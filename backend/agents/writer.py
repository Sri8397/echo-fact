from google import genai
from google.genai import types
from models.incident import Incident
import json

def run_writer_agent(client: genai.Client, research_text: str, fact_text: str) -> dict:
    """Writer Agent: uses response_schema with Incident Pydantic model to emit structured JSON."""
    system_instruction = (
        "You are a Schema Compiler Agent. Format research and facts into a single, "
        "strictly-compliant JSON object matching the `Incident` schema. "
        "Ensure all required fields are present: id, title, locationName, lat, lng, date, year, summary, "
        "category, currentReality, hypeTimeline, verifiedFacts, keyQuestions, and sources. "
        "Ensure category is one of: 'Science & Tech', 'Disaster & Rescue', 'Geopolitical', 'Environmental'."
    )
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=f"Compile into a single valid Incident object:\nRESEARCH:\n{research_text}\nFACTS & QUESTIONS:\n{fact_text}",
        config=types.GenerateContentConfig(
            system_instruction=system_instruction,
            response_mime_type="application/json",
            response_schema=Incident
        )
    )
    if response.parsed:
        return response.parsed.model_dump() if hasattr(response.parsed, "model_dump") else dict(response.parsed)
    return json.loads(response.text)

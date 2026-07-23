from google import genai
from google.genai import types

def run_fact_checker_agent(client: genai.Client, research_text: str) -> str:
    """Fact Checker Agent: extracts empirical facts and key open questions without judgment."""
    system_instruction = (
        "You are a Neutral Fact Extraction Agent. Given research text and sources: "
        "1. Extract only verifiable, empirical data points (measurements, dates, official audit results) as verifiedFacts. "
        "2. Generate 3 open-ended, objective keyQuestions for readers to explore. "
        "3. STRICT MANDATE: Do NOT pass editorial judgment, call earlier news 'hype', or score media accuracy."
    )
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=f"Extract verified hard facts and key open questions from this research:\n{research_text}",
        config=types.GenerateContentConfig(
            system_instruction=system_instruction
        )
    )
    return response.text or ""

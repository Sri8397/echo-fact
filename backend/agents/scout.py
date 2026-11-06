from google import genai
from google.genai import types

def run_scout_agent(client: genai.Client, topic_prompt: str) -> str:
    """News Scout Agent: identifies candidate news incidents matching target categories."""
    system_instruction = (
        "You are a News Scout Agent. Your job is to identify global news incidents "
        "that match one of these categories: 'Science & Tech', 'Disaster & Rescue', "
        "'Geopolitical', 'Environmental'. "
        "Focus on events that had significant public interest and have long-term updates or reality checks."
    )
    response = client.models.generate_content(
        model="gemini-2.5-flash",
        contents=f"Identify candidate topics for: {topic_prompt}",
        config=types.GenerateContentConfig(
            system_instruction=system_instruction
        )
    )
    return response.text or ""

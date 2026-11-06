import pydantic
from typing import List

class TimelineEvent(pydantic.BaseModel):
    year: str
    phase: str
    event: str

class Source(pydantic.BaseModel):
    date: str
    publisher: str
    title: str
    url: str

class Incident(pydantic.BaseModel):
    id: str
    title: str
    locationName: str
    lat: float
    lng: float
    date: str
    year: str
    summary: str
    category: str
    currentReality: str
    hypeTimeline: List[TimelineEvent]
    verifiedFacts: List[str]
    keyQuestions: List[str]
    sources: List[Source]

class IncidentList(pydantic.BaseModel):
    incidents: List[Incident]

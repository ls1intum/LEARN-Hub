Extract the educational activity from this text and return JSON only.

Required JSON structure:
{{
  "data": {{
    "name": "activity name",
    "description": "brief description",
    "ageMin": 6-15,
    "ageMax": 6-15,
    "format": "unplugged|digital|hybrid",
    "bloomLevel": "remember|understand|apply|analyze|evaluate|create",
    "durationMinMinutes": 5-300,
    "durationMaxMinutes": optional number,
    "resourcesNeeded": optional array from ["computers", "tablets", "handouts", "blocks", "electronics", "stationery"],
    "topics": optional array from ["decomposition", "patterns", "abstraction", "algorithms"],
    "mentalLoad": optional "low|medium|high",
    "physicalEnergy": optional "low|medium|high",
    "prepTimeMinutes": optional number,
    "cleanupTimeMinutes": optional number,
    "source": optional string
  }},
  "confidence": 0.0-1.0
}}

Notes:
- For optional arrays, use [] if information is not clear
- Choose closest matching value from allowed options
- Output only the JSON object, no explanation

Text:
{pdfText}

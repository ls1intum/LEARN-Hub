Extract the educational activity metadata from the text below and return JSON only.

Required JSON structure:
{{
  "data": {{
    "name": "Aktivitätsname auf Deutsch",
    "description": "Kurzbeschreibung auf Deutsch",
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
- name and description must be written in German
- source should also be in German if possible
- Categorical values (format, bloomLevel, mentalLoad, physicalEnergy, items in resourcesNeeded and topics) must use the exact English option values listed above — do not translate them
- For optional arrays, use [] if the information is not clear from the text
- Choose the closest matching value from the allowed options
- Output only the JSON object, no explanation

Text:
{pdfText}

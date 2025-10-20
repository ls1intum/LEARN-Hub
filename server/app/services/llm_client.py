from __future__ import annotations

import json
import logging
import re

from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnableSequence
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, field_validator

from app.core.models import (
    ActivityFormat,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    EnergyLevel,
)
from app.utils.config import Config

logger = logging.getLogger(__name__)

# Allowed values (kept compact for prompt + validators)
FORMATS = [f.value for f in ActivityFormat]
RESOURCES = [r.value for r in ActivityResource]
BLOOMS = [b.value for b in BloomLevel]
TOPICS = [t.value for t in ActivityTopic]
ENERGY_LEVELS = [e.value for e in EnergyLevel]


class ThinkingParser:
    """Parser for thinking model output that extracts the final answer from thinking blocks."""

    def __init__(self):
        # Common patterns for thinking model output (expanded for 4B models)
        self.thinking_patterns = [
            r"<thinking>(.*?)</thinking>",
            r"<thought>(.*?)</thought>",
            r"<reasoning>(.*?)</reasoning>",
            r"<think>(.*?)</think>",
            r"<analysis>(.*?)</analysis>",
            r"<process>(.*?)</process>",
            r"<step>(.*?)</step>",
            r"<work>(.*?)</work>",
        ]

        # Patterns for final answer extraction (expanded for 4B models)
        self.answer_patterns = [
            r"<answer>(.*?)</answer>",
            r"<final_answer>(.*?)</final_answer>",
            r"<result>(.*?)</result>",
            r"<output>(.*?)</output>",
            r"<json>(.*?)</json>",
            r"Final answer:\s*(.*?)(?:\n\n|\Z)",
            r"Answer:\s*(.*?)(?:\n\n|\Z)",
            r"Result:\s*(.*?)(?:\n\n|\Z)",
            r"Output:\s*(.*?)(?:\n\n|\Z)",
            r"JSON:\s*(.*?)(?:\n\n|\Z)",
            r"Here is the result:\s*(.*?)(?:\n\n|\Z)",
            r"The answer is:\s*(.*?)(?:\n\n|\Z)",
        ]

    def parse_thinking_output(self, text: str) -> str:
        """
        Parse thinking model output to extract the final answer.

        Args:
            text: Raw output from thinking model

        Returns:
            Cleaned final answer text
        """
        if not text:
            return ""

        # First, try to extract answer using answer patterns
        for pattern in self.answer_patterns:
            matches = re.findall(pattern, text, re.DOTALL | re.IGNORECASE)
            if matches:
                answer = matches[-1].strip()  # Take the last match
                if answer:
                    return self._clean_answer(answer)

        # If no answer tags found, look for JSON in the text
        json_match = self._extract_json_from_text(text)
        if json_match:
            return json_match

        # If no structured answer found, return the text after removing thinking blocks
        cleaned_text = self._remove_thinking_blocks(text)
        return self._clean_answer(cleaned_text)

    def _extract_json_from_text(self, text: str) -> str | None:
        """Extract JSON from text if present."""
        # Look for JSON blocks (expanded patterns for 4B models)
        json_patterns = [
            r"```json\s*(.*?)\s*```",
            r"```\s*(.*?)\s*```",
            r"<json>(.*?)</json>",
            r"<output>(.*?)</output>",
            r"<result>(.*?)</result>",
            r"\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}",  # More robust JSON object pattern
        ]

        for pattern in json_patterns:
            matches = re.findall(pattern, text, re.DOTALL)
            for match in matches:
                try:
                    # Clean the match
                    cleaned_match = match.strip()

                    # Try to parse as JSON
                    json_data = json.loads(cleaned_match)
                    if isinstance(json_data, dict):
                        return json.dumps(json_data, indent=2)
                except json.JSONDecodeError:
                    # Try to fix common JSON issues
                    try:
                        # Fix common issues like trailing commas, missing quotes
                        fixed_json = self._fix_common_json_issues(cleaned_match)
                        json_data = json.loads(fixed_json)
                        if isinstance(json_data, dict):
                            return json.dumps(json_data, indent=2)
                    except json.JSONDecodeError:
                        continue

        return None

    def _fix_common_json_issues(self, json_str: str) -> str:
        """Fix common JSON issues that 4B models might produce."""
        # Remove trailing commas
        json_str = re.sub(r",\s*}", "}", json_str)
        json_str = re.sub(r",\s*]", "]", json_str)

        # Fix unquoted keys
        json_str = re.sub(r"(\w+):", r'"\1":', json_str)

        # Fix single quotes to double quotes
        json_str = json_str.replace("'", '"')

        return json_str

    def _remove_thinking_blocks(self, text: str) -> str:
        """Remove thinking blocks from text."""
        for pattern in self.thinking_patterns:
            text = re.sub(pattern, "", text, flags=re.DOTALL | re.IGNORECASE)
        return text

    def _clean_answer(self, answer: str) -> str:
        """Clean and normalize the final answer."""
        if not answer:
            return ""

        # Remove common prefixes
        prefixes_to_remove = [
            "Final answer:",
            "Answer:",
            "Result:",
            "Here's the answer:",
            "The answer is:",
        ]

        for prefix in prefixes_to_remove:
            if answer.lower().startswith(prefix.lower()):
                answer = answer[len(prefix) :].strip()

        # Remove excessive whitespace
        answer = re.sub(r"\s+", " ", answer).strip()

        return answer


class ExtractionResult(BaseModel):
    """Result of activity data extraction with overall confidence."""

    data: ActivityData
    confidence: float  # Overall confidence level (0.0 to 1.0)


class ActivityData(BaseModel):
    """Simplified activity data model matching database schema exactly."""

    # Required fields
    name: str
    description: str
    age_min: int
    age_max: int
    format: str
    bloom_level: str
    duration_min_minutes: int
    document_id: int = 1  # Default document ID for extracted activities

    # Optional fields
    source: str | None = None
    duration_max_minutes: int | None = None
    resources_needed: list[str] = []
    topics: list[str] = []
    mental_load: str | None = None
    physical_energy: str | None = None
    prep_time_minutes: int | None = None
    cleanup_time_minutes: int | None = None

    @field_validator("description")
    @classmethod
    def _validate_description(cls, v: str) -> str:
        """Validate description length and content."""
        if not v or not v.strip():
            raise ValueError("Description is required")
        v = v.strip()
        if len(v) < 25:
            raise ValueError("Description must be at least 25 characters")
        if len(v) > 1000:
            raise ValueError("Description must be at most 1000 characters")
        return v

    @field_validator("resources_needed", "topics", mode="before")
    @classmethod
    def _normalize_lists(cls, v):
        """Convert single strings to lists and normalize."""
        if v is None:
            return []
        if isinstance(v, str):
            return [v.strip().lower()]
        if isinstance(v, list):
            return [item.strip().lower() for item in v if item and isinstance(item, str)]
        return []


class LLMClient:
    def __init__(self):
        # Load configuration
        self.config = Config.get_instance()

        # Initialize the thinking parser
        self.thinking_parser = ThinkingParser()

        # Validate configuration
        if not self.config.llm_base_url or not self.config.llm_api_key or not self.config.llm_model_name:
            raise ValueError(
                "LLM configuration is incomplete. Please set LLM_BASE_URL, LLM_API_KEY, "
                "and LLM_MODEL_NAME environment variables."
            )

        # Initialize the OpenAI-style LLM client (optimized for 4B thinking model)
        self.llm = ChatOpenAI(
            model=self.config.llm_model_name,
            temperature=0.1,  # Slightly higher for 4B models to avoid repetition
            base_url=self.config.llm_base_url,
            api_key=self.config.llm_api_key,
            max_tokens=2000,  # Reduced for 4B model efficiency
            timeout=60,  # Longer timeout for thinking models
            max_retries=2,  # Retry on failures
        )

        # Wrap the LLM for structured output parsing via Pydantic
        self.structured_llm = self.llm.with_structured_output(ActivityData)

        # Define the prompt template (simplified for 4B thinking model)
        allowed_values_note = (
            "format: " + ", ".join(FORMATS) + " | "
            "bloom_level: " + ", ".join(BLOOMS) + " | "
            "resources: " + ", ".join(RESOURCES) + " | "
            "topics: " + ", ".join(TOPICS) + " | "
            "energy: " + ", ".join(ENERGY_LEVELS)
        )

        self.prompt_template = PromptTemplate(
            input_variables=["pdf_text"],
            template=(
                "Extract activity data from this educational PDF text. Return structured data.\n\n"
                "REQUIRED FIELDS:\n"
                "- name: Activity title\n"
                "- description: 25-75 word summary\n"
                "- age_min, age_max: Student ages (6-15)\n"
                "- format: unplugged/digital/hybrid\n"
                "- bloom_level: remember/understand/apply/analyze/evaluate/create\n"
                "- duration_min_minutes: Activity length (5-300)\n"
                "- topics: MUST have at least 1 topic, prefer 1-2 topics\n\n"
                "OPTIONAL FIELDS:\n"
                "- resources_needed: List of needed items\n"
                "- mental_load, physical_energy: low/medium/high\n"
                "- prep_time_minutes, cleanup_time_minutes: Setup/cleanup time\n"
                "- source: Where activity comes from\n\n"
                "ALLOWED VALUES: {allowed_values}\n\n"
                "IMPORTANT RULES:\n"
                "1. Use exact values from allowed lists\n"
                "2. TOPICS: Be conservative - only assign topics that are clearly relevant. "
                "Every activity MUST have at least 1 topic, but prefer 1-2 topics maximum.\n"
                "3. AGES: Think deeply about age appropriateness. When inferring ages, "
                "err on the side of larger age ranges to be inclusive. "
                "If unsure between ages 8-10 vs 8-12, choose 8-12.\n"
                "4. If unsure about other fields, use null\n"
                "5. Keep descriptions concise\n\n"
                "PDF TEXT:\n{pdf_text}"
            ),
            partial_variables={"allowed_values": allowed_values_note},
        )

        # Create a RunnableSequence: prompt -> structured LLM
        self.chain = RunnableSequence(
            self.prompt_template,
            self.structured_llm,
        )

    def extract_activity_data(self, pdf_text: str, document_id: int | None = None) -> ExtractionResult:
        """Extract activity data from PDF text with overall confidence level."""
        try:
            # Clean and preprocess the text
            cleaned_text = self._preprocess_text(pdf_text)

            # Run the runnable sequence: returns ActivityData instance
            result: ActivityData = self.chain.invoke({"pdf_text": cleaned_text})

            # Override document_id if provided
            if document_id is not None:
                result.document_id = document_id

            # Calculate overall confidence
            confidence = self._calculate_overall_confidence(result, cleaned_text)

            logger.info(f"Successfully extracted activity data: {result.name} (confidence: {confidence:.2f})")
            return ExtractionResult(data=result, confidence=confidence)

        except Exception as e:
            logger.error(f"Failed to extract activity data with structured output: {e}")

            # Fallback: try direct LLM call and parse thinking output
            try:
                result = self._extract_with_thinking_parsing(cleaned_text, document_id)
                # Override document_id if provided
                if document_id is not None:
                    result.document_id = document_id
                confidence = self._calculate_overall_confidence(result, cleaned_text)
                return ExtractionResult(data=result, confidence=confidence)
            except Exception as fallback_error:
                logger.error(f"Fallback extraction also failed: {fallback_error}")

                # Return a minimal valid result rather than failing completely
                fallback_data = ActivityData(
                    name="Unknown Activity",
                    description="An educational activity with details to be determined from the source material.",
                    age_min=8,
                    age_max=12,
                    format="unplugged",
                    bloom_level="understand",
                    duration_min_minutes=30,
                    topics=["algorithms"],  # Ensure at least one topic
                    document_id=document_id or 1,  # Use provided document_id or default to 1
                )
                return ExtractionResult(data=fallback_data, confidence=0.3)  # Low confidence for fallback

    def _extract_with_thinking_parsing(self, pdf_text: str, document_id: int | None = None) -> ActivityData:
        """Fallback method that handles thinking model output directly."""
        try:
            # Create a simplified prompt for 4B thinking model
            json_prompt = f"""
Extract activity data from this PDF text. Return as JSON.

REQUIRED: name, description (25-75 words), age_min, age_max, format, bloom_level, duration_min_minutes, topics
OPTIONAL: resources_needed, mental_load, physical_energy, prep_time_minutes, cleanup_time_minutes, source

VALUES:
format: {', '.join(FORMATS)}
bloom_level: {', '.join(BLOOMS)}
resources: {', '.join(RESOURCES)}
topics: {', '.join(TOPICS)}
energy: {', '.join(ENERGY_LEVELS)}

IMPORTANT:
- TOPICS: Be conservative - only assign clearly relevant topics. Must have at least 1 topic, prefer 1-2 maximum.
- AGES: Think deeply about age appropriateness. When inferring, err on larger age ranges to be inclusive.

Return valid JSON only.

PDF TEXT:
{pdf_text}
"""

            # Get raw response from LLM
            response = self.llm.invoke(json_prompt)
            raw_content = response.content

            # Parse thinking output to extract final answer
            cleaned_content = self.thinking_parser.parse_thinking_output(raw_content)

            # Try multiple parsing strategies
            json_data = None

            # Strategy 1: Direct JSON parsing
            try:
                json_data = json.loads(cleaned_content)
            except json.JSONDecodeError:
                pass

            # Strategy 2: Extract JSON from text
            if not json_data:
                json_match = self.thinking_parser._extract_json_from_text(cleaned_content)
                if json_match:
                    try:
                        json_data = json.loads(json_match)
                    except json.JSONDecodeError:
                        pass

            # Strategy 3: Try to fix and parse
            if not json_data:
                try:
                    fixed_json = self.thinking_parser._fix_common_json_issues(cleaned_content)
                    json_data = json.loads(fixed_json)
                except json.JSONDecodeError:
                    pass

            # Strategy 4: Extract key-value pairs manually
            if not json_data:
                json_data = self._extract_key_value_pairs(cleaned_content, document_id)

            if not json_data:
                raise ValueError("Could not extract valid JSON from model response")

            # Validate and fix the extracted data
            json_data = self._validate_and_fix_extracted_data(json_data, document_id)

            # Create ActivityData from JSON
            activity_data = ActivityData(**json_data)
            logger.info(f"Successfully extracted activity data via thinking parsing: {activity_data.name}")
            return activity_data

        except Exception as e:
            logger.error(f"Thinking parsing extraction failed: {e}")
            raise

    def _preprocess_text(self, text: str) -> str:
        """Clean and preprocess PDF text for better extraction."""
        import re

        # Remove excessive whitespace
        text = re.sub(r"\s+", " ", text).strip()

        # Fix common OCR issues
        text = re.sub(r"([a-z])([A-Z])", r"\1 \2", text)  # Add space between camelCase

        # Remove page numbers and headers/footers (simple patterns)
        text = re.sub(r"^\d+\s*$", "", text, flags=re.MULTILINE)
        text = re.sub(r"Page \d+", "", text, flags=re.IGNORECASE)

        # Remove common PDF artifacts
        text = re.sub(r"[^\w\s\.\,\!\?\:\;\-\(\)]", " ", text)

        return text.strip()

    def _extract_key_value_pairs(self, text: str, document_id: int | None = None) -> dict | None:
        """Extract key-value pairs from text when JSON parsing fails."""
        try:
            # Look for common patterns like "key: value"
            patterns = [
                r"(\w+):\s*([^\n,]+)",
                r'"(\w+)":\s*([^,\n}]+)',
                r"(\w+)\s*=\s*([^\n,]+)",
            ]

            extracted = {}
            for pattern in patterns:
                matches = re.findall(pattern, text, re.IGNORECASE)
                for key, value in matches:
                    key = key.strip().lower()
                    value = value.strip().strip("\"'")

                    # Convert to appropriate types
                    if key in [
                        "age_min",
                        "age_max",
                        "duration_min_minutes",
                        "duration_max_minutes",
                        "prep_time_minutes",
                        "cleanup_time_minutes",
                        "document_id",
                    ]:
                        try:
                            extracted[key] = int(value)
                        except ValueError:
                            continue
                    elif key in ["resources_needed", "topics"]:
                        # Handle lists
                        if value.startswith("[") and value.endswith("]"):
                            try:
                                extracted[key] = json.loads(value)
                            except json.JSONDecodeError:
                                extracted[key] = [v.strip().strip("\"'") for v in value[1:-1].split(",")]
                        else:
                            extracted[key] = [value]
                    else:
                        extracted[key] = value

            # Ensure required fields have defaults
            if "name" not in extracted:
                extracted["name"] = "Extracted Activity"
            if "description" not in extracted:
                extracted["description"] = "An educational activity extracted from the source material."
            if "age_min" not in extracted:
                extracted["age_min"] = 8
            if "age_max" not in extracted:
                extracted["age_max"] = 12
            if "format" not in extracted:
                extracted["format"] = "unplugged"
            if "bloom_level" not in extracted:
                extracted["bloom_level"] = "understand"
            if "duration_min_minutes" not in extracted:
                extracted["duration_min_minutes"] = 30
            if "document_id" not in extracted:
                extracted["document_id"] = document_id or 1
            if "topics" not in extracted or not extracted["topics"]:
                # Default topic assignment based on content
                desc = extracted.get("description", "").lower()
                if "programming" in desc or "code" in desc:
                    extracted["topics"] = ["algorithms"]
                else:
                    extracted["topics"] = ["algorithms"]  # Default fallback

            return extracted if extracted else None

        except Exception as e:
            logger.warning(f"Key-value extraction failed: {e}")
            return None

    def _validate_and_fix_extracted_data(self, data: dict, document_id: int | None = None) -> dict:
        """Validate and fix extracted data to ensure it meets requirements."""
        # Ensure required fields exist
        required_fields = {
            "name": "Extracted Activity",
            "description": "An educational activity extracted from the source material.",
            "age_min": 8,
            "age_max": 12,
            "format": "unplugged",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "document_id": document_id or 1,
            "topics": ["algorithms"],  # Default topic
        }

        for field, default in required_fields.items():
            if field not in data or data[field] is None:
                data[field] = default

        # Validate and fix format
        if data["format"] not in FORMATS:
            data["format"] = "unplugged"

        # Validate and fix bloom_level
        if data["bloom_level"] not in BLOOMS:
            data["bloom_level"] = "understand"

        # Validate and fix energy levels
        if "mental_load" in data and data["mental_load"] not in ENERGY_LEVELS:
            data["mental_load"] = None
        if "physical_energy" in data and data["physical_energy"] not in ENERGY_LEVELS:
            data["physical_energy"] = None

        # Validate age ranges
        if not isinstance(data["age_min"], int) or data["age_min"] < 6:
            data["age_min"] = 6
        if not isinstance(data["age_max"], int) or data["age_max"] > 15:
            data["age_max"] = 12
        if data["age_min"] > data["age_max"]:
            data["age_min"], data["age_max"] = data["age_max"], data["age_min"]

        # Validate duration
        if not isinstance(data["duration_min_minutes"], int) or data["duration_min_minutes"] < 5:
            data["duration_min_minutes"] = 30
        if data["duration_min_minutes"] > 300:
            data["duration_min_minutes"] = 300

        # Validate description length
        if len(data["description"]) < 25:
            data["description"] = (
                data["description"] + " This activity provides hands-on learning opportunities for students."
            )
        if len(data["description"]) > 1000:
            data["description"] = data["description"][:997] + "..."

        # Ensure lists are properly formatted
        if "resources_needed" not in data:
            data["resources_needed"] = []
        if "topics" not in data:
            data["topics"] = []

        # Filter resources and topics to only include valid values
        if isinstance(data["resources_needed"], list):
            data["resources_needed"] = [r for r in data["resources_needed"] if r in RESOURCES]
        if isinstance(data["topics"], list):
            data["topics"] = [t for t in data["topics"] if t in TOPICS]

        # ENFORCE TOPIC REQUIREMENT: Every activity must have at least 1 topic
        if not data["topics"] or len(data["topics"]) == 0:
            # If no topics found, assign a default based on the activity type
            if "programming" in data["description"].lower() or "code" in data["description"].lower():
                data["topics"] = ["algorithms"]
            elif "robot" in data["description"].lower() or "mechanical" in data["description"].lower():
                data["topics"] = ["patterns"]
            elif "break down" in data["description"].lower() or "step" in data["description"].lower():
                data["topics"] = ["decomposition"]
            else:
                # Default to algorithms as it's the most general CS topic
                data["topics"] = ["algorithms"]

        # LIMIT TOPICS: Prefer 1-2 topics maximum (be conservative)
        if len(data["topics"]) > 2:
            # Keep only the first 2 topics to be conservative
            data["topics"] = data["topics"][:2]

        return data

    def _calculate_overall_confidence(self, activity_data: ActivityData, original_text: str) -> float:
        """Calculate overall confidence level for extracted activity data."""
        # Calculate confidence based on key field quality and completeness
        confidence_factors = []

        # Name quality (0.0 to 1.0)
        if activity_data.name and len(activity_data.name) > 3:
            confidence_factors.append(0.8)
        else:
            confidence_factors.append(0.3)

        # Age range validity (0.0 to 1.0)
        if (
            activity_data.age_min
            and activity_data.age_max
            and 6 <= activity_data.age_min <= 15
            and 6 <= activity_data.age_max <= 15
        ):
            confidence_factors.append(0.7)
        else:
            confidence_factors.append(0.4)

        # Format validity (0.0 to 1.0)
        if activity_data.format in FORMATS:
            confidence_factors.append(0.8)
        else:
            confidence_factors.append(0.2)

        # Bloom level validity (0.0 to 1.0)
        if activity_data.bloom_level in BLOOMS:
            confidence_factors.append(0.8)
        else:
            confidence_factors.append(0.2)

        # Duration validity (0.0 to 1.0)
        if activity_data.duration_min_minutes and 5 <= activity_data.duration_min_minutes <= 300:
            confidence_factors.append(0.7)
        else:
            confidence_factors.append(0.4)

        # Topics presence and validity (0.0 to 1.0)
        if activity_data.topics and len(activity_data.topics) > 0:
            valid_topics = [t for t in activity_data.topics if t in TOPICS]
            if len(valid_topics) > 0:
                confidence_factors.append(0.6)
            else:
                confidence_factors.append(0.3)
        else:
            confidence_factors.append(0.3)

        # Description quality (0.0 to 1.0)
        if activity_data.description and 25 <= len(activity_data.description) <= 1000:
            confidence_factors.append(0.6)
        else:
            confidence_factors.append(0.3)

        # Calculate weighted average (required fields have higher weight)
        required_field_weights = [1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.8]  # topics and description slightly lower
        weighted_sum = sum(
            conf * weight for conf, weight in zip(confidence_factors, required_field_weights, strict=True)
        )
        total_weight = sum(required_field_weights)

        overall_confidence = weighted_sum / total_weight

        # Ensure confidence is between 0.0 and 1.0
        return max(0.0, min(1.0, overall_confidence))

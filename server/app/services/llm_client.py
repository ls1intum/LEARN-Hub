from __future__ import annotations

import logging
import os
import re
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import TimeoutError as FuturesTimeoutError

from langchain_core.prompts import PromptTemplate
from langchain_core.runnables import RunnableSequence
from langchain_ollama import ChatOllama
from pydantic import BaseModel, Field, field_validator

from app.core.constants import (
    ACTIVITY_DESCRIPTION_MAX_CHARS,
    ACTIVITY_DESCRIPTION_MIN_CHARS,
    ACTIVITY_MAX_AGE,
    ACTIVITY_MAX_DURATION,
    ACTIVITY_MIN_AGE,
    ACTIVITY_MIN_DURATION,
    TOPICS_MAX_COUNT,
)
from app.core.models import ActivityFormat, ActivityModel, ActivityResource, ActivityTopic, BloomLevel, EnergyLevel
from app.utils.config import Config

logger = logging.getLogger(__name__)


class LLMAuthenticationError(Exception):
    pass


class LLMServiceError(Exception):
    pass


class LLMTimeoutError(Exception):
    pass


FORMATS = [f.value for f in ActivityFormat]
RESOURCES = [r.value for r in ActivityResource]
BLOOMS = [b.value for b in BloomLevel]
TOPICS = [t.value for t in ActivityTopic]
ENERGY_LEVELS = [e.value for e in EnergyLevel]


class ExtractionResult(BaseModel):
    data: ActivityModel
    confidence: float = Field(ge=0.0, le=1.0)

    @field_validator("data", mode="before")
    @classmethod
    def _normalize(cls, v):
        if isinstance(v, ActivityModel):
            return v
        d = dict(v or {})
        name = (d.get("name") or "").strip() or "Extracted Activity"
        desc = (d.get("description") or "").strip()
        if len(desc) < ACTIVITY_DESCRIPTION_MIN_CHARS:
            desc = (desc + " This activity provides a concise summary for teachers.").strip()
        if len(desc) > ACTIVITY_DESCRIPTION_MAX_CHARS:
            desc = desc[: ACTIVITY_DESCRIPTION_MAX_CHARS - 3] + "..."

        def clamp(n, lo, hi, default):
            try:
                n = int(n)
            except Exception:
                n = default
            return max(lo, min(hi, n))

        age_min = clamp(d.get("age_min"), ACTIVITY_MIN_AGE, ACTIVITY_MAX_AGE, 8)
        age_max = clamp(d.get("age_max"), age_min, ACTIVITY_MAX_AGE, max(age_min, 10))
        dur_min = clamp(d.get("duration_min_minutes"), ACTIVITY_MIN_DURATION, ACTIVITY_MAX_DURATION, 30)
        fmt_raw = str(d.get("format", "")).lower()
        if any(k in fmt_raw for k in ("hybrid",)) or (
            "digital" in fmt_raw and any(k in fmt_raw for k in ("physical", "paper", "unplugged"))
        ):
            fmt = "hybrid"
        elif any(k in fmt_raw for k in ("digital", "computer", "tablet", "device")):
            fmt = "digital"
        else:
            fmt = "unplugged"
        bloom = "understand"
        for token in re.split(r"[,\|/]+", str(d.get("bloom_level", "")).lower()):
            t = token.strip()
            if t in BLOOMS:
                bloom = t
                break
            if t.startswith("analys"):
                bloom = "analyze"
                break
            if t.startswith("creat"):
                bloom = "create"
                break
            if t.startswith("underst"):
                bloom = "understand"
                break
            if t.startswith("rememb"):
                bloom = "remember"
                break
            if t.startswith("appl"):
                bloom = "apply"
                break
            if t.startswith("evalu"):
                bloom = "evaluate"
                break
        raw_topics = d.get("topics") or []
        if isinstance(raw_topics, str):
            raw_topics = [raw_topics]
        mapped = []
        for t in raw_topics:
            tt = str(t).strip().lower()
            if tt in TOPICS:
                mapped.append(tt)
                continue
            if "decompos" in tt:
                mapped.append("decomposition")
            elif "pattern" in tt:
                mapped.append("patterns")
            elif any(k in tt for k in ("abstract", "representation", "model", "data rep")):
                mapped.append("abstraction")
            elif any(k in tt for k in ("algorithm", "procedure", "step")):
                mapped.append("algorithms")
        topics = [t for i, t in enumerate(mapped) if t in TOPICS and t not in mapped[:i]]
        topics = topics[:TOPICS_MAX_COUNT]
        raw_res = d.get("resources_needed") or []
        if isinstance(raw_res, str):
            raw_res = [raw_res]
        resources = [str(r).strip().lower() for r in raw_res if str(r).strip().lower() in RESOURCES]
        ml = d.get("mental_load")
        pl = d.get("physical_energy")
        ml = str(ml).strip().lower() if isinstance(ml, str) and str(ml).strip().lower() in ENERGY_LEVELS else None
        pl = str(pl).strip().lower() if isinstance(pl, str) and str(pl).strip().lower() in ENERGY_LEVELS else None

        def to_int_opt(x):
            try:
                return int(x) if x is not None else None
            except Exception:
                return None

        out = {
            "name": name,
            "description": desc,
            "age_min": age_min,
            "age_max": age_max,
            "format": fmt,
            "resources_needed": resources,
            "bloom_level": bloom,
            "duration_min_minutes": dur_min,
            "duration_max_minutes": to_int_opt(d.get("duration_max_minutes")),
            "topics": topics,
            "mental_load": ml,
            "physical_energy": pl,
            "prep_time_minutes": to_int_opt(d.get("prep_time_minutes")),
            "cleanup_time_minutes": to_int_opt(d.get("cleanup_time_minutes")),
        }
        src = d.get("source")
        if isinstance(src, str) and src.strip():
            out["source"] = src.strip()
        return out

    @field_validator("confidence")
    @classmethod
    def _clamp_conf(cls, v: float) -> float:
        try:
            v = float(v)
        except Exception:
            v = 0.5
        return max(0.0, min(1.0, v))


class LLMClient:
    def __init__(self):
        cfg = Config.get_instance()
        if not cfg.llm_base_url or not cfg.llm_api_key or not cfg.llm_model_name:
            raise ValueError("Missing LLM_BASE_URL, LLM_API_KEY, or LLM_MODEL_NAME.")
        orig = os.environ.get("OLLAMA_API_KEY")
        os.environ["OLLAMA_API_KEY"] = cfg.llm_api_key
        try:
            self.llm = ChatOllama(
                model=cfg.llm_model_name, base_url=cfg.llm_base_url, temperature=0.1, num_predict=2048
            )
        finally:
            (
                os.environ.__setitem__("OLLAMA_API_KEY", orig)
                if orig is not None
                else os.environ.pop("OLLAMA_API_KEY", None)
            )
        self.structured = self.llm.with_structured_output(ExtractionResult)

        # Build allowed values lists for the prompt
        resources_list = ", ".join(f'"{r}"' for r in RESOURCES)
        topics_list = ", ".join(f'"{t}"' for t in TOPICS)

        self.prompt = PromptTemplate(
            input_variables=["pdf_text"],
            template=(
                "Extract the educational activity from this text and return JSON only.\n\n"
                "Required JSON structure:\n"
                "{{\n"
                '  "data": {{\n'
                '    "name": "activity name",\n'
                '    "description": "brief description",\n'
                '    "age_min": 6-15,\n'
                '    "age_max": 6-15,\n'
                '    "format": "unplugged|digital|hybrid",\n'
                '    "bloom_level": "remember|understand|apply|analyze|evaluate|create",\n'
                '    "duration_min_minutes": 5-300,\n'
                '    "duration_max_minutes": optional number,\n'
                f'    "resources_needed": optional array from [{resources_list}],\n'
                f'    "topics": optional array from [{topics_list}],\n'
                '    "mental_load": optional "low|medium|high",\n'
                '    "physical_energy": optional "low|medium|high",\n'
                '    "prep_time_minutes": optional number,\n'
                '    "cleanup_time_minutes": optional number,\n'
                '    "source": optional string\n'
                "  }},\n"
                '  "confidence": 0.0-1.0\n'
                "}}\n\n"
                "Notes:\n"
                "- For optional arrays, use [] if information is not clear\n"
                "- Choose closest matching value from allowed options\n"
                "- Output only the JSON object, no explanation\n\n"
                "Text:\n{{pdf_text}}"
            ),
        )
        self.chain = RunnableSequence(self.prompt, self.structured)

    def _calculate_timeout(self, text: str) -> float:
        """Calculate timeout based on text length: 1s per 1000 characters."""
        timeout = max(10.0, len(text) / 1000.0)  # Minimum 10 seconds
        return timeout

    def _invoke_with_timeout(self, text: str, timeout: float) -> ExtractionResult:
        """Invoke the LLM chain with a timeout."""
        with ThreadPoolExecutor(max_workers=1) as executor:
            future = executor.submit(self.chain.invoke, {"pdf_text": text})
            try:
                result = future.result(timeout=timeout)
                return result
            except FuturesTimeoutError as e:
                future.cancel()
                raise LLMTimeoutError(f"LLM request timed out after {timeout:.1f}s") from e

    def extract_activity_data(self, pdf_text: str, document_id: int | None = None) -> ExtractionResult:
        text = re.sub(r"\s+", " ", pdf_text).strip()
        timeout = self._calculate_timeout(text)

        # Try with timeout, retry once on timeout
        for attempt in range(2):
            try:
                result: ExtractionResult = self._invoke_with_timeout(text, timeout)
                logger.info(
                    f"Extracted: {result.data.name} (confidence={result.confidence:.2f}, "
                    f"attempt={attempt + 1}, timeout={timeout:.1f}s)"
                )
                return result
            except LLMTimeoutError as e:
                if attempt == 0:
                    logger.warning(f"LLM timeout on first attempt, retrying: {e}")
                    continue
                else:
                    logger.error(f"LLM timeout on retry, giving up: {e}")
                    raise LLMServiceError(f"LLM request timed out after {timeout:.1f}s (tried twice)") from e
            except Exception as e:
                s = str(e)
                if any(k in s for k in ("401", "unauthorized", "not authenticated")):
                    raise LLMAuthenticationError("LLM authentication failed.") from e
                if "http" in s.lower() or "status code" in s.lower():
                    raise LLMServiceError(f"LLM service error: {s}") from e
                raise LLMServiceError(f"Failed to extract activity data: {s}") from e

        # Should never reach here due to the raise in the loop, but for type safety
        raise LLMServiceError("Unexpected error in extraction retry logic")

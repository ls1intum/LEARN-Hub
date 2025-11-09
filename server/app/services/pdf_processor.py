import io
import logging

from pdfminer.high_level import extract_text

from app.services.llm_client import LLMAuthenticationError, LLMClient, LLMServiceError

logger = logging.getLogger(__name__)


class PDFProcessor:
    def __init__(self):
        self.llm_client = LLMClient()

    def parse_pdf_content(self, pdf_content: bytes, document_id: int | None = None) -> dict:
        """Parse PDF content and extract activity data with overall confidence level."""
        try:
            # Extract raw text from PDF
            text = extract_text(io.BytesIO(pdf_content))

            if not text or len(text.strip()) < 10:
                logger.warning("PDF contains insufficient text for extraction")
                return {"error": "PDF contains insufficient text for extraction", "confidence": None}

            # Extract activity data using enhanced LLM client
            extraction_result = self.llm_client.extract_activity_data(text, document_id)

            # Return both data and overall confidence
            return {
                "data": extraction_result.data.model_dump(mode="json", exclude_none=False),
                "confidence": extraction_result.confidence,
                "text_length": len(text),
                "extraction_quality": self._assess_extraction_quality(
                    extraction_result.data, extraction_result.confidence
                ),
            }

        except LLMAuthenticationError as e:
            logger.error(f"LLM authentication failed: {e}")
            return {
                "error": "LLM authentication failed. Please check your LLM_API_KEY configuration.",
                "confidence": None,
            }
        except LLMServiceError as e:
            logger.error(f"LLM service error: {e}")
            return {"error": f"LLM service error: {str(e)}", "confidence": None}
        except Exception as e:
            logger.error(f"Failed to parse PDF content: {e}")
            return {"error": f"Failed to parse PDF: {str(e)}", "confidence": None}

    def _assess_extraction_quality(self, data, confidence: float) -> str:
        """Assess the overall quality of the extraction."""
        # Count how many fields were successfully extracted
        extracted_fields = sum(
            1
            for field in [
                data.name,
                data.age_min,
                data.age_max,
                data.format,
                data.bloom_level,
                data.duration_min_minutes,
                data.topics,
            ]
            if field is not None
        )

        # Determine quality level based on field completeness and overall confidence
        if extracted_fields >= 5 and confidence >= 0.7:
            return "high"
        elif extracted_fields >= 3 and confidence >= 0.5:
            return "medium"
        else:
            return "low"

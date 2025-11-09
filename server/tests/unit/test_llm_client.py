"""
Comprehensive tests for app/services/llm_client.py - timeout and retry coverage.
"""

from concurrent.futures import TimeoutError as FuturesTimeoutError
from unittest.mock import Mock, patch

import pytest

from app.services.llm_client import (
    ExtractionResult,
    LLMAuthenticationError,
    LLMClient,
    LLMServiceError,
    LLMTimeoutError,
)


class TestExtractionResult:
    """Test ExtractionResult validation and normalization."""

    def test_extraction_result_normalizes_short_description(self):
        """Test that short descriptions are padded."""
        data = {
            "name": "Test",
            "description": "Short",  # Too short
            "age_min": 8,
            "age_max": 10,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert len(result.data.description) >= 25
        assert "This activity provides a concise summary" in result.data.description

    def test_extraction_result_truncates_long_description(self):
        """Test that long descriptions are truncated."""
        data = {
            "name": "Test",
            "description": "a" * 1500,  # Too long
            "age_min": 8,
            "age_max": 10,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert len(result.data.description) <= 1000
        assert result.data.description.endswith("...")

    def test_extraction_result_clamps_age_values(self):
        """Test that age values are clamped to valid range."""
        data = {
            "name": "Test",
            "description": "This is a test activity description with enough characters",
            "age_min": 3,  # Too low
            "age_max": 20,  # Too high
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert result.data.age_min >= 6
        assert result.data.age_max <= 15

    def test_extraction_result_normalizes_format_hybrid(self):
        """Test format normalization to hybrid."""
        data = {
            "name": "Test",
            "description": "This is a test activity description with enough characters",
            "age_min": 8,
            "age_max": 10,
            "format": "digital and paper",  # Should become hybrid
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert result.data.format == "hybrid"

    def test_extraction_result_normalizes_bloom_level(self):
        """Test bloom level normalization from variations."""
        data = {
            "name": "Test",
            "description": "This is a test activity description with enough characters",
            "age_min": 8,
            "age_max": 10,
            "format": "digital",
            "bloom_level": "analysis",  # Should become "analyze"
            "duration_min_minutes": 30,
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert result.data.bloom_level == "analyze"

    def test_extraction_result_allows_empty_topics(self):
        """Test that empty topics list is allowed."""
        data = {
            "name": "Test",
            "description": "This is a test activity description with enough characters",
            "age_min": 8,
            "age_max": 10,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "topics": [],
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert result.data.topics == []

    def test_extraction_result_allows_empty_resources(self):
        """Test that empty resources list is allowed."""
        data = {
            "name": "Test",
            "description": "This is a test activity description with enough characters",
            "age_min": 8,
            "age_max": 10,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "resources_needed": [],
        }
        result = ExtractionResult(data=data, confidence=0.8)
        assert result.data.resources_needed == []

    def test_extraction_result_valid_confidence(self):
        """Test that valid confidence values are accepted."""
        data = {
            "name": "Test",
            "description": "This is a test activity description with enough characters",
            "age_min": 8,
            "age_max": 10,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ExtractionResult(data=data, confidence=0.75)
        assert result.confidence == 0.75
        assert 0.0 <= result.confidence <= 1.0


class TestLLMClient:
    """Test LLMClient class - timeout and retry functionality."""

    @patch("app.services.llm_client.Config")
    def test_llm_client_init_missing_config(self, mock_config):
        """Test LLMClient initialization with missing configuration."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = None
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        with pytest.raises(ValueError, match="Missing LLM_BASE_URL"):
            LLMClient()

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_llm_client_init_valid_config(self, mock_config, mock_chat_ollama):
        """Test LLMClient initialization with valid configuration."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()
        assert client.llm == mock_llm
        assert client.structured == mock_structured

    def test_calculate_timeout_short_text(self):
        """Test timeout calculation for short text."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOllama"):
                client = LLMClient()
                timeout = client._calculate_timeout("Short text")
                assert timeout >= 10.0  # Minimum timeout

    def test_calculate_timeout_long_text(self):
        """Test timeout calculation for long text."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOllama"):
                client = LLMClient()
                # 8000 chars ≈ 2000 tokens = 2s timeout, plus max(10, 2) = 10s minimum
                # So we need much longer text: 80000 chars ≈ 20000 tokens = 20s
                long_text = "a" * 80000
                timeout = client._calculate_timeout(long_text)
                assert timeout >= 10.0
                assert timeout > 10.0  # Should be more than minimum for very long text

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_invoke_with_timeout_success(self, mock_config, mock_chat_ollama):
        """Test successful invocation with timeout."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        # Mock chain
        mock_result = ExtractionResult(
            data={
                "name": "Test",
                "description": "This is a test activity description with enough characters",
                "age_min": 8,
                "age_max": 10,
                "format": "digital",
                "bloom_level": "understand",
                "duration_min_minutes": 30,
            },
            confidence=0.8,
        )
        client.chain = Mock()
        client.chain.invoke.return_value = mock_result

        result = client._invoke_with_timeout("Test text", timeout=5.0)
        assert isinstance(result, ExtractionResult)

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_invoke_with_timeout_timeout_error(self, mock_config, mock_chat_ollama):
        """Test invocation that times out."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        # Mock chain that takes too long
        with patch("app.services.llm_client.ThreadPoolExecutor") as mock_executor_class:
            mock_future = Mock()
            mock_future.result.side_effect = FuturesTimeoutError("Timed out")
            mock_future.cancel.return_value = True

            mock_executor_instance = Mock()
            mock_executor_instance.submit.return_value = mock_future
            mock_executor_instance.__enter__ = Mock(return_value=mock_executor_instance)
            mock_executor_instance.__exit__ = Mock(return_value=None)

            mock_executor_class.return_value = mock_executor_instance

            with pytest.raises(LLMTimeoutError, match="LLM request timed out"):
                client._invoke_with_timeout("Test text", timeout=0.1)

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_success_first_try(self, mock_config, mock_chat_ollama):
        """Test successful extraction on first try."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        mock_result = ExtractionResult(
            data={
                "name": "Test Activity",
                "description": "This is a test activity description with enough characters",
                "age_min": 8,
                "age_max": 10,
                "format": "digital",
                "bloom_level": "understand",
                "duration_min_minutes": 30,
            },
            confidence=0.9,
        )

        with patch.object(client, "_invoke_with_timeout", return_value=mock_result):
            result = client.extract_activity_data("Test PDF text")

        assert isinstance(result, ExtractionResult)
        assert result.data.name == "Test Activity"
        assert result.confidence == 0.9

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_success_on_retry(self, mock_config, mock_chat_ollama):
        """Test successful extraction after retry."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        mock_result = ExtractionResult(
            data={
                "name": "Test Activity",
                "description": "This is a test activity description with enough characters",
                "age_min": 8,
                "age_max": 10,
                "format": "digital",
                "bloom_level": "understand",
                "duration_min_minutes": 30,
            },
            confidence=0.9,
        )

        # First call times out, second succeeds
        with patch.object(
            client,
            "_invoke_with_timeout",
            side_effect=[LLMTimeoutError("Timeout on first try"), mock_result],
        ):
            result = client.extract_activity_data("Test PDF text")

        assert isinstance(result, ExtractionResult)
        assert result.data.name == "Test Activity"

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_timeout_both_tries(self, mock_config, mock_chat_ollama):
        """Test extraction fails after both timeout attempts."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        # Both attempts time out
        with patch.object(
            client,
            "_invoke_with_timeout",
            side_effect=LLMTimeoutError("Timeout"),
        ):
            with pytest.raises(LLMServiceError, match="timed out.*tried twice"):
                client.extract_activity_data("Test PDF text")

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_authentication_error(self, mock_config, mock_chat_ollama):
        """Test authentication error handling."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        with patch.object(
            client,
            "_invoke_with_timeout",
            side_effect=Exception("401 unauthorized"),
        ):
            with pytest.raises(LLMAuthenticationError, match="authentication failed"):
                client.extract_activity_data("Test PDF text")

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_http_error(self, mock_config, mock_chat_ollama):
        """Test HTTP error handling."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        with patch.object(
            client,
            "_invoke_with_timeout",
            side_effect=Exception("HTTP error with status code 500"),
        ):
            with pytest.raises(LLMServiceError, match="LLM service error"):
                client.extract_activity_data("Test PDF text")

    @patch("app.services.llm_client.ChatOllama")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_generic_error(self, mock_config, mock_chat_ollama):
        """Test generic error handling."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured = Mock()
        mock_llm.with_structured_output.return_value = mock_structured
        mock_chat_ollama.return_value = mock_llm

        client = LLMClient()

        with patch.object(
            client,
            "_invoke_with_timeout",
            side_effect=Exception("Some random error"),
        ):
            with pytest.raises(LLMServiceError, match="Failed to extract activity data"):
                client.extract_activity_data("Test PDF text")

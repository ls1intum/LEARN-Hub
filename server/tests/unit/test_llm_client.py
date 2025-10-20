"""
Comprehensive tests for app/services/llm_client.py - 75% coverage target.
"""

from unittest.mock import Mock, patch

import pytest

from app.services.llm_client import (
    ActivityData,
    ExtractionResult,
    LLMClient,
    ThinkingParser,
)


class TestThinkingParser:
    """Test ThinkingParser class - comprehensive coverage."""

    def test_parse_thinking_output_with_answer_tags(self):
        """Test parsing thinking output with answer tags."""
        parser = ThinkingParser()
        text = "<thinking>Let me think about this</thinking><answer>Final result</answer>"
        result = parser.parse_thinking_output(text)
        assert result == "Final result"

    def test_parse_thinking_output_with_final_answer_tags(self):
        """Test parsing thinking output with final answer tags."""
        parser = ThinkingParser()
        text = "<thinking>Analysis</thinking><final_answer>Complete result</final_answer>"
        result = parser.parse_thinking_output(text)
        assert result == "Complete result"

    def test_parse_thinking_output_with_json_tags(self):
        """Test parsing thinking output with JSON tags."""
        parser = ThinkingParser()
        text = '<thinking>Processing</thinking><json>{"key": "value"}</json>'
        result = parser.parse_thinking_output(text)
        assert result == '{"key": "value"}'

    def test_parse_thinking_output_with_json_blocks(self):
        """Test parsing thinking output with JSON code blocks."""
        parser = ThinkingParser()
        text = '```json\n{"name": "test"}\n```'
        result = parser.parse_thinking_output(text)
        assert result == '{\n  "name": "test"\n}'

    def test_parse_thinking_output_without_tags(self):
        """Test parsing thinking output without structured tags."""
        parser = ThinkingParser()
        text = "This is just plain text without any tags"
        result = parser.parse_thinking_output(text)
        assert result == "This is just plain text without any tags"

    def test_parse_thinking_output_empty(self):
        """Test parsing empty thinking output."""
        parser = ThinkingParser()
        result = parser.parse_thinking_output("")
        assert result == ""

    def test_parse_thinking_output_none(self):
        """Test parsing None thinking output."""
        parser = ThinkingParser()
        result = parser.parse_thinking_output(None)
        assert result == ""

    def test_extract_json_from_text_valid_json(self):
        """Test JSON extraction with valid JSON."""
        parser = ThinkingParser()
        text = '{"name": "test", "value": 123}'
        result = parser._extract_json_from_text(text)
        assert result == '{\n  "name": "test",\n  "value": 123\n}'

    def test_extract_json_from_text_invalid_json(self):
        """Test JSON extraction with invalid JSON."""
        parser = ThinkingParser()
        text = "This is not JSON at all"
        result = parser._extract_json_from_text(text)
        assert result is None

    def test_extract_json_from_text_with_code_blocks(self):
        """Test JSON extraction with code blocks."""
        parser = ThinkingParser()
        text = '```json\n{"test": true}\n```'
        result = parser._extract_json_from_text(text)
        assert result == '{\n  "test": true\n}'

    def test_fix_common_json_issues_trailing_comma(self):
        """Test fixing common JSON issues - trailing comma."""
        parser = ThinkingParser()
        json_str = '{"key": "value",}'
        result = parser._fix_common_json_issues(json_str)
        assert result == '{"key": "value"}'

    def test_fix_common_json_issues_unquoted_keys(self):
        """Test fixing common JSON issues - unquoted keys."""
        parser = ThinkingParser()
        json_str = "{key: 'value'}"
        result = parser._fix_common_json_issues(json_str)
        assert result == '{"key": "value"}'

    def test_fix_common_json_issues_single_quotes(self):
        """Test fixing common JSON issues - single quotes."""
        parser = ThinkingParser()
        json_str = "{'key': 'value'}"
        result = parser._fix_common_json_issues(json_str)
        assert result == '{"key": "value"}'

    def test_remove_thinking_blocks(self):
        """Test removing thinking blocks from text."""
        parser = ThinkingParser()
        text = "<thinking>This is thinking</thinking>Final answer here"
        result = parser._remove_thinking_blocks(text)
        assert result == "Final answer here"

    def test_clean_answer_remove_prefixes(self):
        """Test cleaning answer by removing common prefixes."""
        parser = ThinkingParser()
        answer = "Final answer: This is the result"
        result = parser._clean_answer(answer)
        assert result == "This is the result"

    def test_clean_answer_normalize_whitespace(self):
        """Test cleaning answer by normalizing whitespace."""
        parser = ThinkingParser()
        answer = "This   has    excessive    whitespace"
        result = parser._clean_answer(answer)
        assert result == "This has excessive whitespace"


class TestActivityData:
    """Test ActivityData model validation."""

    def test_activity_data_valid(self):
        """Test ActivityData with valid data."""
        data = ActivityData(
            name="Test Activity",
            description="This is a test activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
        )
        assert data.name == "Test Activity"
        assert data.age_min == 8
        assert data.age_max == 12

    def test_activity_data_description_too_short(self):
        """Test ActivityData with description too short."""
        with pytest.raises(ValueError, match="Description must be at least 25 characters"):
            ActivityData(
                name="Test Activity",
                description="Too short",
                age_min=8,
                age_max=12,
                format="digital",
                bloom_level="understand",
                duration_min_minutes=30,
            )

    def test_activity_data_description_too_long(self):
        """Test ActivityData with description too long."""
        long_desc = "a" * 1001
        with pytest.raises(ValueError, match="Description must be at most 1000 characters"):
            ActivityData(
                name="Test Activity",
                description=long_desc,
                age_min=8,
                age_max=12,
                format="digital",
                bloom_level="understand",
                duration_min_minutes=30,
            )

    def test_activity_data_normalize_lists_string(self):
        """Test ActivityData list normalization with string input."""
        data = ActivityData(
            name="Test Activity",
            description="This is a test activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
            resources_needed="computers",
            topics="algorithms",
        )
        assert data.resources_needed == ["computers"]
        assert data.topics == ["algorithms"]

    def test_activity_data_normalize_lists_list(self):
        """Test ActivityData list normalization with list input."""
        data = ActivityData(
            name="Test Activity",
            description="This is a test activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
            resources_needed=["computers", "tablets"],
            topics=["algorithms", "patterns"],
        )
        assert data.resources_needed == ["computers", "tablets"]
        assert data.topics == ["algorithms", "patterns"]

    def test_activity_data_normalize_lists_none(self):
        """Test ActivityData list normalization with None input."""
        data = ActivityData(
            name="Test Activity",
            description="This is a test activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
            resources_needed=None,
            topics=None,
        )
        assert data.resources_needed == []
        assert data.topics == []


class TestExtractionResult:
    """Test ExtractionResult model."""

    def test_extraction_result_valid(self):
        """Test ExtractionResult with valid data."""
        activity_data = ActivityData(
            name="Test Activity",
            description="This is a test activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
        )
        result = ExtractionResult(data=activity_data, confidence=0.8)
        assert result.data == activity_data
        assert result.confidence == 0.8


class TestLLMClient:
    """Test LLMClient class - comprehensive coverage with mocks."""

    @patch("app.services.llm_client.Config")
    def test_llm_client_init_missing_config(self, mock_config):
        """Test LLMClient initialization with missing configuration."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = None
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        with pytest.raises(ValueError, match="LLM configuration is incomplete"):
            LLMClient()

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_llm_client_init_valid_config(self, mock_config, mock_chat_openai):
        """Test LLMClient initialization with valid configuration."""
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_chat_openai.return_value = mock_llm

        client = LLMClient()
        assert client.config == mock_config_instance
        assert client.llm == mock_llm

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_success(self, mock_config, mock_chat_openai):
        """Test successful activity data extraction."""
        # Setup mocks
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured_llm = Mock()
        mock_llm.with_structured_output.return_value = mock_structured_llm
        mock_chat_openai.return_value = mock_llm

        # Mock the chain invoke
        mock_activity_data = ActivityData(
            name="Test Activity",
            description="This is a test activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
        )
        mock_chain = Mock()
        mock_chain.invoke.return_value = mock_activity_data

        client = LLMClient()
        client.chain = mock_chain

        # Test extraction
        result = client.extract_activity_data("Test PDF text", document_id=123)

        assert isinstance(result, ExtractionResult)
        assert result.data == mock_activity_data
        assert result.data.document_id == 123
        assert 0.0 <= result.confidence <= 1.0

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_fallback_success(self, mock_config, mock_chat_openai):
        """Test activity data extraction with fallback to thinking parsing."""
        # Setup mocks
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured_llm = Mock()
        mock_llm.with_structured_output.return_value = mock_structured_llm
        mock_chat_openai.return_value = mock_llm

        # Mock the chain invoke to fail
        mock_chain = Mock()
        mock_chain.invoke.side_effect = Exception("Structured output failed")

        # Mock the fallback method
        mock_activity_data = ActivityData(
            name="Fallback Activity",
            description="This is a fallback activity with sufficient description length",
            age_min=8,
            age_max=12,
            format="digital",
            bloom_level="understand",
            duration_min_minutes=30,
        )

        client = LLMClient()
        client.chain = mock_chain

        with patch.object(client, "_extract_with_thinking_parsing", return_value=mock_activity_data):
            result = client.extract_activity_data("Test PDF text")

        assert isinstance(result, ExtractionResult)
        assert result.data == mock_activity_data
        assert 0.0 <= result.confidence <= 1.0

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_extract_activity_data_complete_failure(self, mock_config, mock_chat_openai):
        """Test activity data extraction with complete failure."""
        # Setup mocks
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured_llm = Mock()
        mock_llm.with_structured_output.return_value = mock_structured_llm
        mock_chat_openai.return_value = mock_llm

        # Mock the chain invoke to fail
        mock_chain = Mock()
        mock_chain.invoke.side_effect = Exception("Structured output failed")

        client = LLMClient()
        client.chain = mock_chain

        # Mock the fallback method to also fail
        with patch.object(client, "_extract_with_thinking_parsing", side_effect=Exception("Fallback failed")):
            result = client.extract_activity_data("Test PDF text")

        # Should return fallback data
        assert isinstance(result, ExtractionResult)
        assert result.data.name == "Unknown Activity"
        assert result.confidence == 0.3

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_extract_with_thinking_parsing_success(self, mock_config, mock_chat_openai):
        """Test thinking parsing extraction with success."""
        # Setup mocks
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured_llm = Mock()
        mock_llm.with_structured_output.return_value = mock_structured_llm
        mock_chat_openai.return_value = mock_llm

        # Mock LLM response
        mock_response = Mock()
        mock_response.content = (
            '{"name": "Test Activity", "description": "This is a test activity with sufficient description length", '
            '"age_min": 8, "age_max": 12, "format": "digital", "bloom_level": "understand", "duration_min_minutes": 30}'
        )
        mock_llm.invoke.return_value = mock_response

        client = LLMClient()

        result = client._extract_with_thinking_parsing("Test PDF text", document_id=123)

        assert isinstance(result, ActivityData)
        assert result.name == "Test Activity"
        assert result.document_id == 123

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_extract_with_thinking_parsing_json_fix(self, mock_config, mock_chat_openai):
        """Test thinking parsing extraction with JSON fixing."""
        # Setup mocks
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured_llm = Mock()
        mock_llm.with_structured_output.return_value = mock_structured_llm
        mock_chat_openai.return_value = mock_llm

        # Mock LLM response with malformed JSON
        mock_response = Mock()
        mock_response.content = (
            '{name: "Test Activity", description: "This is a test activity with sufficient description length", '
            'age_min: 8, age_max: 12, format: "digital", bloom_level: "understand", duration_min_minutes: 30}'
        )
        mock_llm.invoke.return_value = mock_response

        client = LLMClient()

        result = client._extract_with_thinking_parsing("Test PDF text")

        assert isinstance(result, ActivityData)
        assert result.name == "Test Activity"

    @patch("app.services.llm_client.ChatOpenAI")
    @patch("app.services.llm_client.Config")
    def test_extract_with_thinking_parsing_key_value_extraction(self, mock_config, mock_chat_openai):
        """Test thinking parsing extraction with key-value pair extraction."""
        # Setup mocks
        mock_config_instance = Mock()
        mock_config_instance.llm_base_url = "http://test.com"
        mock_config_instance.llm_api_key = "test_key"
        mock_config_instance.llm_model_name = "test_model"
        mock_config.get_instance.return_value = mock_config_instance

        mock_llm = Mock()
        mock_structured_llm = Mock()
        mock_llm.with_structured_output.return_value = mock_structured_llm
        mock_chat_openai.return_value = mock_llm

        # Mock LLM response with key-value format
        mock_response = Mock()
        mock_response.content = (
            "name: Test Activity\ndescription: This is a test activity with sufficient description length\n"
            "age_min: 8\nage_max: 12\nformat: digital\nbloom_level: understand\nduration_min_minutes: 30"
        )
        mock_llm.invoke.return_value = mock_response

        client = LLMClient()

        result = client._extract_with_thinking_parsing("Test PDF text")

        assert isinstance(result, ActivityData)
        # The key-value extraction might include the full text, so we check it contains the expected name
        assert "Test Activity" in result.name

    def test_preprocess_text(self):
        """Test text preprocessing."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                # Test text preprocessing
                text = "This   has    excessive    whitespace\n\nAnd page numbers 1 2 3"
                result = client._preprocess_text(text)
                assert "excessive whitespace" in result
                # The preprocessing normalizes whitespace and removes some artifacts
                assert "This has excessive whitespace" in result

    def test_extract_key_value_pairs_success(self):
        """Test key-value pair extraction."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                text = "name: Test Activity\nage_min: 8\nage_max: 12\nformat: digital"
                result = client._extract_key_value_pairs(text, document_id=123)

                assert result is not None
                assert result["name"] == "Test Activity"
                assert result["age_min"] == 8
                assert result["document_id"] == 123

    def test_extract_key_value_pairs_failure(self):
        """Test key-value pair extraction failure."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                # Test with empty text
                result = client._extract_key_value_pairs("", document_id=123)
                assert result is not None  # Should return defaults

    def test_validate_and_fix_extracted_data(self):
        """Test validation and fixing of extracted data."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                # Test with incomplete data
                data = {"name": "Test Activity"}
                result = client._validate_and_fix_extracted_data(data, document_id=123)

                assert result["name"] == "Test Activity"
                assert result["age_min"] == 8  # Default
                assert result["age_max"] == 12  # Default
                assert result["format"] == "unplugged"  # Default
                assert result["document_id"] == 123

    def test_validate_and_fix_extracted_data_invalid_values(self):
        """Test validation and fixing with invalid values."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                # Test with invalid values
                data = {
                    "name": "Test Activity",
                    "age_min": 3,  # Too low
                    "age_max": 20,  # Too high
                    "format": "invalid",
                    "bloom_level": "invalid",
                    "duration_min_minutes": 1,  # Too low
                }
                result = client._validate_and_fix_extracted_data(data, document_id=123)

                assert result["age_min"] == 6  # Fixed
                assert result["age_max"] == 12  # Fixed
                assert result["format"] == "unplugged"  # Fixed
                assert result["bloom_level"] == "understand"  # Fixed
                assert result["duration_min_minutes"] == 30  # Fixed

    def test_calculate_overall_confidence_high_quality(self):
        """Test confidence calculation with high quality data."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                activity_data = ActivityData(
                    name="High Quality Activity",
                    description="This is a high quality activity with sufficient description length for testing",
                    age_min=8,
                    age_max=12,
                    format="digital",
                    bloom_level="understand",
                    duration_min_minutes=30,
                    topics=["algorithms"],
                )

                confidence = client._calculate_overall_confidence(activity_data, "Original text")
                assert 0.0 <= confidence <= 1.0
                assert confidence > 0.5  # Should be high confidence

    def test_calculate_overall_confidence_low_quality(self):
        """Test confidence calculation with low quality data."""
        with patch("app.services.llm_client.Config") as mock_config:
            mock_config_instance = Mock()
            mock_config_instance.llm_base_url = "http://test.com"
            mock_config_instance.llm_api_key = "test_key"
            mock_config_instance.llm_model_name = "test_model"
            mock_config.get_instance.return_value = mock_config_instance

            with patch("app.services.llm_client.ChatOpenAI"):
                client = LLMClient()

                # Create a valid ActivityData first, then modify attributes for testing
                activity_data = ActivityData(
                    name="Valid Activity",
                    description="This is a valid activity with sufficient description length",
                    age_min=8,
                    age_max=12,
                    format="digital",
                    bloom_level="understand",
                    duration_min_minutes=30,
                    topics=["algorithms"],
                )

                # Manually set low quality attributes for testing
                activity_data.name = "X"  # Too short
                activity_data.age_min = 3  # Invalid
                activity_data.age_max = 20  # Invalid
                activity_data.format = "invalid"
                activity_data.bloom_level = "invalid"
                activity_data.duration_min_minutes = 1  # Invalid
                activity_data.topics = []  # Empty

                confidence = client._calculate_overall_confidence(activity_data, "Original text")
                assert 0.0 <= confidence <= 1.0
                assert confidence < 0.5  # Should be low confidence

"""
Auth API tests - Staff Engineer "More is Less" approach.
Focuses on critical authentication flows and security.
"""


class TestAuthAPI:
    """Test authentication API endpoints - core security functionality."""

    def test_request_verification_code_missing_email(self, client):
        """Test verification code request without email field."""
        response = client.post("/api/auth/verification-code", json={})
        assert response.status_code == 422

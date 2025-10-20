"""Simplified configuration management."""

from __future__ import annotations

from dotenv import load_dotenv
from pydantic import Field, computed_field, field_validator
from pydantic_settings import BaseSettings


class Config(BaseSettings):
    """Simplified configuration with all settings in one class."""

    model_config = {
        "env_prefix": "",
        "case_sensitive": False,
        "env_file": ".env",
        "env_file_encoding": "utf-8",
    }

    # Database settings
    postgres_db: str = Field(default="cs_activities", alias="POSTGRES_DB")
    postgres_user: str = Field(default="postgres", alias="POSTGRES_USER")
    postgres_password: str = Field(default="postgres", alias="POSTGRES_PASSWORD")
    postgres_host: str = Field(default="localhost", alias="POSTGRES_HOST")
    postgres_port: int = Field(default=5432, alias="POSTGRES_PORT")

    # Database connection pooling
    pool_size: int = Field(default=10, alias="DB_POOL_SIZE")
    max_overflow: int = Field(default=20, alias="DB_MAX_OVERFLOW")
    pool_recycle: int = Field(default=3600, alias="DB_POOL_RECYCLE")
    pool_pre_ping: bool = Field(default=True, alias="DB_POOL_PRE_PING")

    # Direct URI override (support both environment variable names)
    database_uri: str | None = Field(default=None, alias="SQLALCHEMY_DATABASE_URI")
    database_url_env: str | None = Field(default=None, alias="DATABASE_URL")

    # Security settings
    jwt_secret_key: str = Field(default="dev-secret-key", alias="JWT_SECRET_KEY")
    jwt_access_token_expires: int = Field(default=900, alias="JWT_ACCESS_TOKEN_EXPIRES")  # 15 minutes
    jwt_refresh_token_expires: int = Field(default=2592000, alias="JWT_REFRESH_TOKEN_EXPIRES")  # 30 days
    flask_secret_key: str = Field(default="dev-flask-secret", alias="FLASK_SECRET_KEY")
    dev_secret_key: str = Field(default="dev-dev-secret", alias="DEV_SECRET_KEY")

    # Email settings (STARTTLS only)
    email_address: str = Field(default="", alias="EMAIL_ADDRESS")
    email_username: str = Field(default="", alias="EMAIL_USERNAME")
    email_password: str = Field(default="", alias="EMAIL_PASSWORD")
    email_sender_name: str = Field(default="LEARN-Hub Team", alias="EMAIL_SENDER_NAME")
    smtp_server: str = Field(default="smtp.gmail.com", alias="SMTP_SERVER")
    smtp_port: int = Field(default=587, alias="SMTP_PORT")

    # External services
    google_api_key: str = Field(default="", alias="GOOGLE_API_KEY")
    frontend_url: str = Field(default="http://localhost:3000", alias="FRONTEND_URL")

    # LLM Configuration
    llm_base_url: str = Field(default="", alias="LLM_BASE_URL")
    llm_api_key: str = Field(default="", alias="LLM_API_KEY")
    llm_model_name: str = Field(default="", alias="LLM_MODEL_NAME")

    # Storage
    pdf_storage_path: str = Field(
        default="/Users/adrian/code-personal/master-thesis-code/server/data/pdfs/", alias="PDF_STORAGE_PATH"
    )

    # Environment
    environment: str = Field(default="development", alias="ENVIRONMENT")
    debug: bool = Field(default=False, alias="DEBUG")

    @field_validator("environment")
    @classmethod
    def validate_environment(cls, v: str) -> str:
        allowed = {"development", "testing", "production"}
        if v.lower() not in allowed:
            raise ValueError(f"Environment must be one of: {allowed}")
        return v.lower()

    def __init__(self, **kwargs):
        # Load .env file if it exists
        load_dotenv()
        super().__init__(**kwargs)

    @computed_field
    @property
    def SQLALCHEMY_DATABASE_URI(self) -> str:
        """Get the complete database URI."""
        # Check both environment variable names for backward compatibility
        if self.database_uri:
            return self.database_uri
        if self.database_url_env:
            return self.database_url_env
        return (
            f"postgresql://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )

    @computed_field
    @property
    def database_url(self) -> str:
        """Alias for database URI for backward compatibility."""
        return self.SQLALCHEMY_DATABASE_URI

    @computed_field
    @property
    def EMAIL_SENDER_FORMATTED(self) -> str:
        """Get formatted sender address with name."""
        if self.email_sender_name and self.email_address:
            return f"{self.email_sender_name} <{self.email_address}>"
        return self.email_address

    @classmethod
    def get_instance(cls) -> Config:
        """Get the singleton configuration instance."""
        return cls()

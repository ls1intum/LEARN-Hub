"""allow_teachers_to_have_passwords

Revision ID: c4029a75d31e
Revises: f8fb644483e9
Create Date: 2025-10-19 08:57:47.357425

"""

from collections.abc import Sequence

# revision identifiers, used by Alembic.
revision: str = "c4029a75d31e"
down_revision: str | Sequence[str] | None = "f8fb644483e9"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    pass


def downgrade() -> None:
    """Downgrade schema."""
    pass

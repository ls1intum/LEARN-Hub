"""add_description_to_activity

Revision ID: add_description_to_activity
Revises: 79c9f7e1ecc2
Create Date: 2025-01-27 12:00:00.000000

"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "add_description_to_activity"
down_revision: str | Sequence[str] | None = "79c9f7e1ecc2"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    # Add description column as nullable first
    op.add_column("activities", sa.Column("description", sa.String(length=1000), nullable=True))

    # Set 'WIP' as default value for existing activities
    op.execute("UPDATE activities SET description = 'WIP' WHERE description IS NULL")

    # Now make the column not null
    op.alter_column("activities", "description", nullable=False)


def downgrade() -> None:
    """Downgrade schema."""
    # Drop description column
    op.drop_column("activities", "description")

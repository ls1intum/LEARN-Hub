"""add_lesson_plan_snapshot_to_user_favourites

Revision ID: 7a1d2c3e4f56
Revises: 841a75f1cd58
Create Date: 2025-10-20 00:00:00.000000

"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "7a1d2c3e4f56"
down_revision: str | Sequence[str] | None = "841a75f1cd58"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema by adding lesson_plan_snapshot JSON column."""
    op.add_column(
        "user_favourites",
        sa.Column("lesson_plan_snapshot", sa.JSON(), nullable=True),
    )


def downgrade() -> None:
    """Downgrade schema by removing lesson_plan_snapshot column."""
    op.drop_column("user_favourites", "lesson_plan_snapshot")

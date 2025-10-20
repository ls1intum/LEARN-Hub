"""remove_user_selections_table

Revision ID: 6ee0728482d6
Revises: 05bfbbeeec18
Create Date: 2025-09-08 22:07:18.977482

"""

from collections.abc import Sequence

import sqlalchemy as sa
from alembic import op

# revision identifiers, used by Alembic.
revision: str = "6ee0728482d6"
down_revision: str | Sequence[str] | None = "05bfbbeeec18"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None


def upgrade() -> None:
    """Upgrade schema."""
    # Drop the user_selections table
    op.drop_table("user_selections")


def downgrade() -> None:
    """Downgrade schema."""
    # Recreate the user_selections table
    op.create_table(
        "user_selections",
        sa.Column("id", sa.Integer(), nullable=False),
        sa.Column("user_id", sa.Integer(), nullable=False),
        sa.Column("activity_ids", sa.JSON(), nullable=False),
        sa.Column("search_criteria", sa.JSON(), nullable=False),
        sa.Column("ordering_method", sa.String(length=50), nullable=False),
        sa.Column("total_duration", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(), server_default=sa.text("(CURRENT_TIMESTAMP)"), nullable=True),
        sa.ForeignKeyConstraint(
            ["user_id"],
            ["users.id"],
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_user_selections_id"), "user_selections", ["id"], unique=False)

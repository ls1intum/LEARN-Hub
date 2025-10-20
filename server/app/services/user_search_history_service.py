from __future__ import annotations

from typing import Any

from sqlalchemy.orm import Session, scoped_session

from app.db.models.user import UserSearchHistory


class UserSearchHistoryService:
    """Service for managing user search history from recommendation form."""

    def __init__(self, db: Session | scoped_session[Session] | Any):
        self.db = db

    def save_search_query(
        self,
        user_id: int,
        search_criteria: dict[str, Any],
    ) -> UserSearchHistory:
        """
        Save a user's search query from the recommendation form.

        Args:
            user_id: ID of the user making the search
            search_criteria: Search parameters used

        Returns:
            Created UserSearchHistory record
        """
        search_history = UserSearchHistory(
            user_id=user_id,
            search_criteria=search_criteria,
        )

        self.db.add(search_history)
        self.db.commit()
        self.db.refresh(search_history)
        return search_history

    def get_user_search_history(self, user_id: int, limit: int = 50, offset: int = 0) -> list[UserSearchHistory]:
        """
        Retrieve a user's search history with pagination.

        Args:
            user_id: ID of the user
            limit: Maximum number of records to return
            offset: Number of records to skip

        Returns:
            List of UserSearchHistory records ordered by creation date (newest first)
        """
        return (
            self.db.query(UserSearchHistory)
            .filter(UserSearchHistory.user_id == user_id)
            .order_by(UserSearchHistory.created_at.desc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def get_search_history_by_id(self, history_id: int, user_id: int) -> UserSearchHistory | None:
        """
        Retrieve a specific search history entry by ID (with user ownership check).

        Args:
            history_id: ID of the search history entry
            user_id: ID of the user (for ownership verification)

        Returns:
            UserSearchHistory record if found and owned by user, None otherwise
        """
        return (
            self.db.query(UserSearchHistory)
            .filter(UserSearchHistory.id == history_id, UserSearchHistory.user_id == user_id)
            .first()
        )

    def delete_search_history(self, history_id: int, user_id: int) -> bool:
        """
        Delete a search history entry (with ownership verification).

        Args:
            history_id: ID of the search history entry to delete
            user_id: ID of the user (for ownership verification)

        Returns:
            True if deletion was successful, False otherwise
        """
        history = self.get_search_history_by_id(history_id, user_id)
        if history:
            self.db.delete(history)
            self.db.commit()
            return True
        return False

    def reset_user_search_history(self, user_id: int) -> int:
        """
        Delete all search history for a specific user.

        Args:
            user_id: ID of the user whose search history should be deleted

        Returns:
            Number of search history entries that were deleted
        """
        deleted_count = self.db.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).delete()
        self.db.commit()
        return deleted_count

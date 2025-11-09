from __future__ import annotations

from typing import Any

from sqlalchemy.orm import Session, scoped_session

from app.db.models.user import UserFavourites


class UserFavouritesService:
    """Service for managing user favourites (individual activities and lesson plans)."""

    def __init__(self, db: Session | scoped_session[Session] | Any):
        self.db = db

    def save_activity_favourite(
        self,
        user_id: int,
        activity_id: int,
        name: str | None = None,
    ) -> UserFavourites:
        """
        Save a user's favourite individual activity.

        Args:
            user_id: ID of the user making the favourite
            activity_id: ID of the activity to favourite
            name: Optional custom name for the favourite

        Returns:
            Created UserFavourites record
        """
        favourite = UserFavourites(
            user_id=user_id,
            favourite_type="activity",
            activity_id=activity_id,
            name=name,
        )

        self.db.add(favourite)
        self.db.commit()
        self.db.refresh(favourite)
        return favourite

    def save_lesson_plan_favourite(
        self,
        user_id: int,
        activity_ids: list[int],
        name: str | None = None,
        lesson_plan_snapshot: dict[str, Any] | None = None,
    ) -> UserFavourites:
        """
        Save a user's favourite lesson plan from the results page.

        Args:
            user_id: ID of the user making the favourite
            activity_ids: List of activity IDs in the lesson plan
            name: Optional custom name for the favourite

        Returns:
            Created UserFavourites record
        """
        favourite = UserFavourites(
            user_id=user_id,
            favourite_type="lesson_plan",
            activity_ids=activity_ids,
            lesson_plan_snapshot=lesson_plan_snapshot,
            name=name,
        )

        self.db.add(favourite)
        self.db.commit()
        self.db.refresh(favourite)
        return favourite

    def get_user_favourites(self, user_id: int, limit: int = 50, offset: int = 0) -> list[UserFavourites]:
        """
        Retrieve a user's favourites with pagination.

        Args:
            user_id: ID of the user
            limit: Maximum number of records to return
            offset: Number of records to skip

        Returns:
            List of UserFavourites records ordered by creation date (newest first)
        """
        return (
            self.db.query(UserFavourites)
            .filter(UserFavourites.user_id == user_id)
            .order_by(UserFavourites.created_at.desc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def get_favourite_by_id(self, favourite_id: int, user_id: int) -> UserFavourites | None:
        """
        Retrieve a specific favourite by ID (with user ownership check).

        Args:
            favourite_id: ID of the favourite
            user_id: ID of the user (for ownership verification)

        Returns:
            UserFavourites record if found and owned by user, None otherwise
        """
        return (
            self.db.query(UserFavourites)
            .filter(UserFavourites.id == favourite_id, UserFavourites.user_id == user_id)
            .first()
        )

    def delete_favourite(self, favourite_id: int, user_id: int) -> bool:
        """
        Delete a favourite (with ownership verification).

        Args:
            favourite_id: ID of the favourite to delete
            user_id: ID of the user (for ownership verification)

        Returns:
            True if deletion was successful, False otherwise
        """
        favourite = self.get_favourite_by_id(favourite_id, user_id)
        if favourite:
            self.db.delete(favourite)
            self.db.commit()
            return True
        return False

    def is_activity_favourited(self, user_id: int, activity_id: int) -> bool:
        """
        Check if a specific activity is already favourited by the user.

        Args:
            user_id: ID of the user
            activity_id: ID of the activity to check

        Returns:
            True if the activity is favourited, False otherwise
        """
        favourite = (
            self.db.query(UserFavourites)
            .filter(
                UserFavourites.user_id == user_id,
                UserFavourites.favourite_type == "activity",
                UserFavourites.activity_id == activity_id,
            )
            .first()
        )
        return favourite is not None

    def remove_activity_favourite(self, user_id: int, activity_id: int) -> bool:
        """
        Remove a specific activity from user's favourites.

        Args:
            user_id: ID of the user
            activity_id: ID of the activity to remove from favourites

        Returns:
            True if removal was successful, False if not found
        """
        favourite = (
            self.db.query(UserFavourites)
            .filter(
                UserFavourites.user_id == user_id,
                UserFavourites.favourite_type == "activity",
                UserFavourites.activity_id == activity_id,
            )
            .first()
        )
        if favourite:
            self.db.delete(favourite)
            self.db.commit()
            return True
        return False

    def get_user_activity_favourites(self, user_id: int, limit: int = 50, offset: int = 0) -> list[UserFavourites]:
        """
        Retrieve a user's favourite activities with pagination.

        Args:
            user_id: ID of the user
            limit: Maximum number of records to return
            offset: Number of records to skip

        Returns:
            List of UserFavourites records for activities ordered by creation date (newest first)
        """
        return (
            self.db.query(UserFavourites)
            .filter(
                UserFavourites.user_id == user_id,
                UserFavourites.favourite_type == "activity",
            )
            .order_by(UserFavourites.created_at.desc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def get_user_lesson_plan_favourites(self, user_id: int, limit: int = 50, offset: int = 0) -> list[UserFavourites]:
        """
        Retrieve a user's favourite lesson plans with pagination.

        Args:
            user_id: ID of the user
            limit: Maximum number of records to return
            offset: Number of records to skip

        Returns:
            List of UserFavourites records for lesson plans ordered by creation date (newest first)
        """
        return (
            self.db.query(UserFavourites)
            .filter(
                UserFavourites.user_id == user_id,
                UserFavourites.favourite_type == "lesson_plan",
            )
            .order_by(UserFavourites.created_at.desc())
            .offset(offset)
            .limit(limit)
            .all()
        )

    def reset_user_favourites(self, user_id: int, auto_commit: bool = True) -> int:
        """
        Delete all favourites for a specific user.

        Args:
            user_id: ID of the user whose favourites should be deleted
            auto_commit: Whether to commit the transaction automatically (default: True)

        Returns:
            Number of favourites that were deleted
        """
        deleted_count = self.db.query(UserFavourites).filter(UserFavourites.user_id == user_id).delete()
        if auto_commit:
            self.db.commit()
        return deleted_count

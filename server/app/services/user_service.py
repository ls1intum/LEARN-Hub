from __future__ import annotations

from typing import Any

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, scoped_session

from app.db.models.user import User, UserRole
from app.services.user_favourites_service import UserFavouritesService
from app.services.user_search_history_service import UserSearchHistoryService
from app.services.verification_service import VerificationService
from app.utils.password_utils import PasswordUtils


class UserService:
    def __init__(self, db: Session | scoped_session[Session] | Any):
        self.db = db

    def create_user(self, email: str, first_name: str, last_name: str, role: UserRole = UserRole.TEACHER) -> User:
        db_user = User(email=email, first_name=first_name, last_name=last_name, role=role)
        self.db.add(db_user)
        self.db.commit()
        self.db.refresh(db_user)
        return db_user

    def get_user_by_email(self, email: str) -> User | None:
        return self.db.query(User).filter(User.email == email).first()

    def get_user_by_id(self, user_id: int) -> User | None:
        return self.db.query(User).filter(User.id == user_id).first()

    def update_user(self, user: User) -> User:
        self.db.commit()
        self.db.refresh(user)
        return user

    def delete_user(self, user_id: int) -> bool:
        """Delete a user and all associated data.

        This method handles cleanup of related records to prevent foreign key constraint
        violations:
        - Verification codes (expired and non-expired)
        - User search history
        - User favourites

        Args:
            user_id: ID of the user to delete

        Returns:
            True if deletion was successful, False if user not found

        Raises:
            IntegrityError: If deletion fails due to database constraints
        """
        user = self.get_user_by_id(user_id)
        if not user:
            return False

        try:
            # Clean up related data in order to prevent foreign key constraint violations
            verification_service = VerificationService(self.db)
            verification_service.delete_all_user_codes(user_id)

            search_history_service = UserSearchHistoryService(self.db)
            search_history_service.reset_user_search_history(user_id)

            favourites_service = UserFavouritesService(self.db)
            favourites_service.reset_user_favourites(user_id)

            # Now safe to delete the user
            self.db.delete(user)
            self.db.commit()
            return True
        except IntegrityError as e:
            # Rollback the transaction on integrity error
            self.db.rollback()
            raise e

    def get_all_users(self) -> list[User]:
        return self.db.query(User).all()

    def get_users_by_role(self, role: UserRole) -> list[User]:
        return self.db.query(User).filter(User.role == role).all()

    def get_teachers(self) -> list[User]:
        return self.get_users_by_role(UserRole.TEACHER)

    def get_admins(self) -> list[User]:
        return self.get_users_by_role(UserRole.ADMIN)

    def set_password(self, user_id: int, password: str) -> bool:
        """Set password for a user (admin or teacher)."""
        user = self.get_user_by_id(user_id)
        if not user:
            return False

        password_hash = PasswordUtils.hash_password(password)
        user.password_hash = password_hash
        self.db.commit()
        return True

    def verify_password(self, user_id: int, password: str) -> bool:
        """Verify password for a user."""
        user = self.get_user_by_id(user_id)
        if not user or not user.password_hash:
            return False

        try:
            return PasswordUtils.verify_password(password, user.password_hash)
        except Exception:
            return False

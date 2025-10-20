from __future__ import annotations

from typing import Any

from sqlalchemy.orm import Session, scoped_session

from app.db.models.user import User, UserRole
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
        user = self.get_user_by_id(user_id)
        if user:
            self.db.delete(user)
            self.db.commit()
            return True
        return False

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

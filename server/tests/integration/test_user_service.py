import pytest
from sqlalchemy.orm import Session

from app.services.user_service import UserService


@pytest.fixture()
def user_service(db_session: Session) -> UserService:
    return UserService(db_session)


def test_create_user(user_service: UserService):
    email = "test1@example.com"
    first_name = "Test"
    last_name = "User"
    user = user_service.create_user(email, first_name, last_name)
    assert user.email == email
    assert user.first_name == first_name
    assert user.last_name == last_name
    assert user.id is not None


def test_get_user_by_email(user_service: UserService):
    email = "test2@example.com"
    user_service.create_user(email, "Test", "User")
    user = user_service.get_user_by_email(email)
    assert user is not None
    assert user.email == email


def test_get_user_by_email_not_found(user_service: UserService):
    email = "nonexistent@example.com"
    user = user_service.get_user_by_email(email)
    assert user is None

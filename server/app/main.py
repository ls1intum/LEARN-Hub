import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from flask import jsonify
from flask_cors import CORS
from flask_openapi3 import OpenAPI, Server
from werkzeug.middleware.proxy_fix import ProxyFix

from app.api.activities.creation import register_activities_creation_routes
from app.api.activities.lesson_plan import register_activities_lesson_plan_routes
from app.api.activities.listing import register_activities_listing_routes
from app.api.activities.pdf import register_activities_pdf_routes
from app.api.activities.recommendations import register_activities_recommendations_routes
from app.api.auth import register_auth_routes
from app.api.documents import register_documents_routes
from app.api.history import register_history_routes
from app.api.meta import register_meta_routes
from app.services.email_service import EmailService
from app.utils.config import Config
from app.utils.database_factory import init_database

# Define API info
info = {
    "title": "LEARN-Hub API",
    "version": "1.0.0",
    "description": "API for LEARN-Hub activity recommendations and lesson planning",
}


def create_app():
    # Initialize Flask-OpenAPI3 app
    servers = [
        Server(url="http://localhost:5001", description="Development local server"),
        Server(url="https://master-api.dev.amihalcea.com", description="Development homeserver"),
        Server(url="https://learnhub-test.aet.cit.tum.de", description="Production server"),
    ]

    # Define security schemes for JWT authentication
    security_schemes = {
        "BearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "bearerFormat": "JWT",
            "description": "JWT token for authentication. Include 'Bearer ' prefix in Authorization header.",
        }
    }

    app = OpenAPI(
        __name__,
        info=info,
        servers=servers,
        doc_ui=True,
        security_schemes=security_schemes,
        doc_url="/openapi.json",
        doc_prefix="/api/openapi",
    )

    # Initialize configuration
    config = Config.get_instance()
    app.config.from_object(config)

    # Enable CORS for all routes - allow from anywhere for development and production
    CORS(app, origins="*", supports_credentials=True)

    # Trust reverse proxy headers (X-Forwarded-For/Proto/Host/Port/Prefix)
    app.wsgi_app = ProxyFix(app.wsgi_app, x_for=1, x_proto=1, x_host=1, x_port=1, x_prefix=1)

    # Configure Flask to handle reverse proxy properly
    app.config["PREFERRED_URL_SCHEME"] = "https"

    with app.app_context():
        init_database()

    app.config["SECRET_KEY"] = config.flask_secret_key
    app.config["SESSION_COOKIE_SECURE"] = True

    # Initialize email service
    email_service = EmailService()
    email_service.init_app(app)

    # Register all route modules
    register_auth_routes(app)
    register_activities_listing_routes(app)
    register_activities_recommendations_routes(app)
    register_activities_lesson_plan_routes(app)
    register_activities_pdf_routes(app)
    register_activities_creation_routes(app)
    register_history_routes(app)
    register_documents_routes(app)
    register_meta_routes(app)

    @app.route("/api/hello")
    def hello_world():
        return jsonify({"message": "Hello, world!"})

    @app.teardown_appcontext
    def shutdown_session(exception=None):
        from app.db.database import get_db_session

        try:
            session = get_db_session()
            if session is not None:
                session.remove()
        except Exception:
            # Ignore errors during teardown to prevent test failures
            pass

    return app


# Create app instance for Flask CLI and other imports
# Only create app when explicitly requested to avoid database connection during imports
app = None


def get_app():
    """Get the Flask app instance, creating it if necessary."""
    global app
    if app is None:
        app = create_app()
    return app


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "5001"))
    get_app().run(host="0.0.0.0", port=port, debug=True)

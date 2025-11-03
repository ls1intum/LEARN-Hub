import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "@/hooks/useAuth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import {
  User,
  Mail,
  UserCheck,
  AlertTriangle,
  CheckCircle,
  Heart,
  History,
} from "lucide-react";
import { logger } from "@/services/logger";
import type { UpdateProfileRequest } from "@/types/api";

export const AccountDashboardPage: React.FC = () => {
  const { user, updateProfile, deleteAccount } = useAuth();
  const navigate = useNavigate();
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState<{
    type: "success" | "error";
    text: string;
  } | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");

  // Form state for ADMIN/TEACHER users
  const [formData, setFormData] = useState<UpdateProfileRequest>({
    email: user?.email || "",
    first_name: user?.first_name || "",
    last_name: user?.last_name || "",
    password: "",
  });

  const isGuest = user?.role === "GUEST";

  const handleInputChange = (
    field: keyof UpdateProfileRequest,
    value: string,
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setMessage(null);
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setMessage(null);

    try {
      // Only include password if it's provided
      const updateData: UpdateProfileRequest = {
        email: formData.email,
        first_name: formData.first_name,
        last_name: formData.last_name,
      };

      if (formData.password && formData.password.trim()) {
        updateData.password = formData.password;
      }

      const result = await updateProfile(updateData);

      if (result.success) {
        setMessage({
          type: "success",
          text: result.message || "Profile updated successfully",
        });
        // Clear password field after successful update
        setFormData((prev) => ({ ...prev, password: "" }));
      } else {
        setMessage({
          type: "error",
          text: result.message || "Failed to update profile",
        });
      }
    } catch (error) {
      logger.error("Error updating profile", error, "AccountDashboardPage");
      setMessage({ type: "error", text: "An unexpected error occurred" });
    } finally {
      setIsLoading(false);
    }
  };

  const getErrorMessage = (error: unknown): string => {
    if (error instanceof Error) {
      const message = error.message.toLowerCase();

      // Handle specific error cases
      if (message.includes("cannot delete your own account")) {
        return "You cannot delete your own account.";
      }
      if (message.includes("not found")) {
        return "Account not found. It may have already been deleted.";
      }
      if (message.includes("foreign key") || message.includes("constraint")) {
        return "Unable to delete account due to database constraints. Please contact support if this persists.";
      }
      if (message.includes("network") || message.includes("fetch")) {
        return "Network error. Please check your connection and try again.";
      }
      if (message.includes("401") || message.includes("unauthorized")) {
        return "You don't have permission to delete this account. Please contact an administrator.";
      }
      if (message.includes("403") || message.includes("forbidden")) {
        return "You don't have permission to delete this account.";
      }
      if (message.includes("500") || message.includes("server")) {
        return "Server error occurred. Please try again later or contact support.";
      }

      // Return the original message if it's meaningful
      return error.message;
    }
    return "An unexpected error occurred. Please try again.";
  };

  const handleDeleteAccount = async () => {
    if (deleteConfirmText !== "DELETE") {
      setMessage({ type: "error", text: "Please type 'DELETE' to confirm" });
      return;
    }

    setIsLoading(true);
    setMessage(null);

    try {
      const result = await deleteAccount();

      if (result.success) {
        // User will be logged out and redirected automatically
        navigate("/login", {
          state: { message: "Account deleted successfully" },
        });
      } else {
        const errorMessage = result.message || "Failed to delete account";
        setMessage({
          type: "error",
          text: errorMessage,
        });
        setShowDeleteConfirm(false);
        setDeleteConfirmText("");
      }
    } catch (error) {
      logger.error("Error deleting account", error, "AccountDashboardPage");
      const errorMessage = getErrorMessage(error);
      setMessage({ type: "error", text: errorMessage });
      setShowDeleteConfirm(false);
      setDeleteConfirmText("");
    } finally {
      setIsLoading(false);
    }
  };

  const handleRegisterClick = () => {
    navigate("/login");
  };

  if (isGuest) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Account Dashboard
          </h1>
          <p className="text-muted-foreground">
            Manage your account settings and preferences
          </p>
        </div>

        <div className="grid gap-6">
          {/* Guest User Info */}
          <Card>
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="p-2 bg-muted rounded-lg">
                  <User className="h-5 w-5 text-muted-foreground" />
                </div>
                <div>
                  <CardTitle className="text-lg">Guest Account</CardTitle>
                  <CardDescription>
                    You're currently using a guest account
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  <Badge variant="secondary">Guest User</Badge>
                  <span className="text-sm text-muted-foreground">
                    Limited access
                  </span>
                </div>
                <p className="text-sm text-muted-foreground">
                  As a guest user, you can browse activities and get
                  recommendations, but you cannot save favorites or track your
                  search history.
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Registration Call-to-Action */}
          <Card className="border-primary/20 bg-gradient-to-r from-primary/5 to-primary/10">
            <CardHeader>
              <div className="flex items-center gap-3">
                <div className="p-2 bg-primary rounded-lg">
                  <UserCheck className="h-5 w-5 text-primary-foreground" />
                </div>
                <div>
                  <CardTitle className="text-lg text-primary">
                    Register for a Full Account
                  </CardTitle>
                  <CardDescription>
                    Unlock all features and personalize your experience
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="flex items-center gap-3 p-3 bg-background/50 rounded-lg">
                    <Heart className="h-4 w-4 text-primary" />
                    <div>
                      <p className="text-sm font-medium">Save Favorites</p>
                      <p className="text-xs text-muted-foreground">
                        Bookmark activities and lesson plans
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 p-3 bg-background/50 rounded-lg">
                    <History className="h-4 w-4 text-primary" />
                    <div>
                      <p className="text-sm font-medium">Search History</p>
                      <p className="text-xs text-muted-foreground">
                        Track your past searches
                      </p>
                    </div>
                  </div>
                </div>
                <Button onClick={handleRegisterClick} className="w-full">
                  Register Now
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">
          Account Dashboard
        </h1>
        <p className="text-muted-foreground">
          Manage your account settings and preferences
        </p>
      </div>

      <div className="grid gap-6">
        {/* Current Profile Info */}
        <Card>
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="p-2 bg-muted rounded-lg">
                <User className="h-5 w-5 text-muted-foreground" />
              </div>
              <div>
                <CardTitle className="text-lg">Profile Information</CardTitle>
                <CardDescription>Your current account details</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  Email
                </Label>
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{user?.email}</span>
                </div>
              </div>
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  Role
                </Label>
                <div className="flex items-center gap-2">
                  <Badge
                    variant={user?.role === "ADMIN" ? "default" : "secondary"}
                  >
                    {user?.role}
                  </Badge>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Edit Profile Form */}
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">Edit Profile</CardTitle>
            <CardDescription>Update your personal information</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSaveProfile} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="first_name">First Name</Label>
                  <Input
                    id="first_name"
                    value={formData.first_name}
                    onChange={(e) =>
                      handleInputChange("first_name", e.target.value)
                    }
                    placeholder="Enter your first name"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="last_name">Last Name</Label>
                  <Input
                    id="last_name"
                    value={formData.last_name}
                    onChange={(e) =>
                      handleInputChange("last_name", e.target.value)
                    }
                    placeholder="Enter your last name"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email Address</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => handleInputChange("email", e.target.value)}
                  placeholder="Enter your email address"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">New Password (Optional)</Label>
                <Input
                  id="password"
                  type="password"
                  value={formData.password}
                  onChange={(e) =>
                    handleInputChange("password", e.target.value)
                  }
                  placeholder="Enter new password (leave blank to keep current)"
                />
                <p className="text-xs text-muted-foreground">
                  Password must be at least 8 characters long
                </p>
              </div>
              <Button type="submit" disabled={isLoading} className="w-full">
                {isLoading ? "Saving..." : "Save Changes"}
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* Account Actions */}
        <Card className="border-destructive/20">
          <CardHeader>
            <div className="flex items-center gap-3">
              <div className="p-2 bg-destructive/10 rounded-lg">
                <AlertTriangle className="h-5 w-5 text-destructive" />
              </div>
              <div>
                <CardTitle className="text-lg text-destructive">
                  Danger Zone
                </CardTitle>
                <CardDescription>
                  Irreversible and destructive actions
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="p-4 bg-destructive/5 border border-destructive/20 rounded-lg">
                <h4 className="font-medium text-destructive mb-2">
                  Delete Account
                </h4>
                <p className="text-sm text-muted-foreground mb-4">
                  Permanently delete your account and all associated data. This
                  action cannot be undone.
                </p>
                {!showDeleteConfirm ? (
                  <Button
                    variant="destructive"
                    onClick={() => setShowDeleteConfirm(true)}
                    className="w-full"
                  >
                    Delete Account
                  </Button>
                ) : (
                  <div className="space-y-3">
                    <div className="p-3 bg-destructive/10 border border-destructive/30 rounded-lg">
                      <p className="text-sm font-medium text-destructive mb-2">
                        This will permanently delete:
                      </p>
                      <ul className="text-xs text-muted-foreground space-y-1 ml-4">
                        <li>• Your account and profile information</li>
                        <li>• All saved favorites and lesson plans</li>
                        <li>• Your search history</li>
                        <li>• All other personal data</li>
                      </ul>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="delete_confirm">
                        Type <strong>DELETE</strong> to confirm:
                      </Label>
                      <Input
                        id="delete_confirm"
                        value={deleteConfirmText}
                        onChange={(e) => setDeleteConfirmText(e.target.value)}
                        placeholder="Type DELETE to confirm"
                        className="border-destructive/50"
                      />
                    </div>
                    <div className="flex gap-2">
                      <Button
                        variant="destructive"
                        onClick={handleDeleteAccount}
                        disabled={isLoading || deleteConfirmText !== "DELETE"}
                        className="flex-1"
                      >
                        {isLoading ? "Deleting..." : "Confirm Delete"}
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => {
                          setShowDeleteConfirm(false);
                          setDeleteConfirmText("");
                        }}
                        className="flex-1"
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Messages */}
        {message && (
          <Alert variant={message.type === "error" ? "destructive" : "default"}>
            <div className="flex items-center gap-2">
              {message.type === "success" ? (
                <CheckCircle className="h-4 w-4" />
              ) : (
                <AlertTriangle className="h-4 w-4" />
              )}
              <AlertDescription>{message.text}</AlertDescription>
            </div>
          </Alert>
        )}
      </div>
    </div>
  );
};

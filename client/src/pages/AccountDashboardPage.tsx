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
import { useTranslation } from "react-i18next";

export const AccountDashboardPage: React.FC = () => {
  const { user, updateProfile, deleteAccount } = useAuth();
  const navigate = useNavigate();
  const { t } = useTranslation();
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
    firstName: user?.firstName || "",
    lastName: user?.lastName || "",
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
        firstName: formData.firstName,
        lastName: formData.lastName,
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
      <div className="py-6">
        <div className="mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-1.5">
            {t("account.title")}
          </h1>
          <p className="text-muted-foreground text-sm sm:text-base">
            {t("account.subtitle")}
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
                  <CardTitle className="text-lg">{t("account.guestTitle")}</CardTitle>
                  <CardDescription>
                    {t("account.guestDesc")}
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center gap-2">
                  <Badge variant="secondary">{t("userHeader.guestUser")}</Badge>
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
                      <p className="text-sm font-medium">{t("account.favourites")}</p>
                      <p className="text-xs text-muted-foreground">
                        Bookmark activities and lesson plans
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3 p-3 bg-background/50 rounded-lg">
                    <History className="h-4 w-4 text-primary" />
                    <div>
                      <p className="text-sm font-medium">{t("account.searchHistory")}</p>
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
    <div className="py-6">
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-1.5">
          {t("account.title")}
        </h1>
        <p className="text-muted-foreground text-sm sm:text-base">
          {t("account.subtitle")}
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
                <CardTitle className="text-lg">{t("account.profileInfo")}</CardTitle>
                <CardDescription>{t("account.profileDesc")}</CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  {t("account.email")}
                </Label>
                <div className="flex items-center gap-2">
                  <Mail className="h-4 w-4 text-muted-foreground" />
                  <span className="text-sm">{user?.email}</span>
                </div>
              </div>
              <div className="space-y-2">
                <Label className="text-sm font-medium text-muted-foreground">
                  {t("account.role")}
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
            <CardTitle className="text-lg">{t("account.editProfile")}</CardTitle>
            <CardDescription>{t("account.editProfileDesc")}</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSaveProfile} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName">{t("account.firstName")}</Label>
                  <Input
                    id="firstName"
                    value={formData.firstName}
                    onChange={(e) =>
                      handleInputChange("firstName", e.target.value)
                    }
                    placeholder={t("account.enterFirstName")}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName">{t("account.lastName")}</Label>
                  <Input
                    id="lastName"
                    value={formData.lastName}
                    onChange={(e) =>
                      handleInputChange("lastName", e.target.value)
                    }
                    placeholder={t("account.enterLastName")}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">{t("account.emailAddress")}</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => handleInputChange("email", e.target.value)}
                  placeholder={t("account.enterEmail")}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">{t("account.newPassword")}</Label>
                <Input
                  id="password"
                  type="password"
                  value={formData.password}
                  onChange={(e) =>
                    handleInputChange("password", e.target.value)
                  }
                  placeholder={t("account.enterPassword")}
                />
                <p className="text-xs text-muted-foreground">
                  {t("account.passwordHint")}
                </p>
              </div>
              <Button type="submit" disabled={isLoading} className="w-full">
                {isLoading ? t("account.saving") : t("account.saveChanges")}
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
                  {t("account.dangerZone")}
                </CardTitle>
                <CardDescription>
                  {t("account.dangerZoneDesc")}
                </CardDescription>
              </div>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              <div className="p-4 bg-destructive/5 border border-destructive/20 rounded-lg">
                <h4 className="font-medium text-destructive mb-2">
                  {t("account.deleteAccount")}
                </h4>
                <p className="text-sm text-muted-foreground mb-4">
                  {t("account.deleteAccountDesc")}
                </p>
                {!showDeleteConfirm ? (
                  <Button
                    variant="destructive"
                    onClick={() => setShowDeleteConfirm(true)}
                    className="w-full"
                  >
                    {t("account.deleteAccount")}
                  </Button>
                ) : (
                  <div className="space-y-3">
                    <div className="p-3 bg-destructive/10 border border-destructive/30 rounded-lg">
                      <p className="text-sm font-medium text-destructive mb-2">
                        {t("account.deleteConfirmTitle")}
                      </p>
                      <ul className="text-xs text-muted-foreground space-y-1 ml-4">
                        <li>• {t("account.deleteItem1")}</li>
                        <li>• {t("account.deleteItem2")}</li>
                        <li>• {t("account.deleteItem3")}</li>
                        <li>• {t("account.deleteItem4")}</li>
                      </ul>
                    </div>
                    <div className="space-y-2">
                      <Label htmlFor="delete_confirm">
                        {t("account.typeDelete")}
                      </Label>
                      <Input
                        id="delete_confirm"
                        value={deleteConfirmText}
                        onChange={(e) => setDeleteConfirmText(e.target.value)}
                        placeholder={t("account.typeDeletePlaceholder")}
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
                        {isLoading ? t("account.deleting") : t("account.confirmDelete")}
                      </Button>
                      <Button
                        variant="outline"
                        onClick={() => {
                          setShowDeleteConfirm(false);
                          setDeleteConfirmText("");
                        }}
                        className="flex-1"
                      >
                        {t("account.cancel")}
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

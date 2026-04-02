import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ThemeToggle } from "@/components/ThemeToggle";
import { useAuth } from "@/hooks/useAuth";
import { useEnvironment } from "@/hooks/useEnvironment";
import {
  LogIn,
  Mail,
  Lock,
  Shield,
  GraduationCap,
  AlertCircle,
  Users,
  Server,
} from "lucide-react";
import {
  getEnvironmentDisplayText,
  getEnvironmentBadgeVariant,
} from "@/utils/environment";
import { useTranslation } from "react-i18next";

type LoginMode =
  | "admin"
  | "teacher"
  | "teacher-password"
  | "verification-code"
  | "register"
  | "reset-password";

export const LoginPage: React.FC = () => {
  const [mode, setMode] = useState<LoginMode>("teacher");
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    code: "",
    firstName: "",
    lastName: "",
  });
  const [message, setMessage] = useState("");
  const [isSuccess, setIsSuccess] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [shouldRedirect, setShouldRedirect] = useState(false);
  const [redirectPath, setRedirectPath] = useState("");

  const {
    login,
    teacherLogin,
    verificationCodeLogin,
    requestVerificationCode,
    registerTeacher,
    resetPassword,
    user,
  } = useAuth();
  const navigate = useNavigate();
  const { environment } = useEnvironment();
  const { t } = useTranslation();

  // Handle redirect after user state is updated
  useEffect(() => {
    if (shouldRedirect && user && redirectPath) {
      navigate(redirectPath);
      setShouldRedirect(false);
      setRedirectPath("");
    }
  }, [user, shouldRedirect, redirectPath, navigate]);

  const setErrorMessage = (msg: string) => {
    setIsSuccess(false);
    setMessage(msg);
  };

  const setSuccessMessage = (msg: string) => {
    setIsSuccess(true);
    setMessage(msg);
  };

  const handleGuestLogin = async () => {
    setIsLoading(true);
    setSuccessMessage(t("login.welcomeGuest"));
    navigate("/recommendations");
    setIsLoading(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      let result;
      switch (mode) {
        case "admin":
          if (!formData.email || !formData.password) {
            setErrorMessage(t("login.enterBothEmailPassword"));
            setIsLoading(false);
            return;
          }
          result = await login(formData.email, formData.password);
          if (result.success) {
            setSuccessMessage(t("login.loginSuccessRedirecting"));
            setRedirectPath("/recommendations");
            setShouldRedirect(true);
          }
          break;
        case "teacher":
          if (!formData.email) {
            setErrorMessage(t("login.enterEmailAddress"));
            setIsLoading(false);
            return;
          }
          result = await requestVerificationCode(formData.email);
          if (result.success) {
            setSuccessMessage(t("login.verificationCodeSent"));
            setMode("verification-code");
          }
          break;
        case "teacher-password":
          if (!formData.email || !formData.password) {
            setErrorMessage(t("login.enterBothEmailPassword"));
            setIsLoading(false);
            return;
          }
          result = await teacherLogin(formData.email, formData.password);
          if (result.success) {
            setSuccessMessage(t("login.loginSuccessRedirecting"));
            setRedirectPath("/recommendations");
            setShouldRedirect(true);
          }
          break;
        case "verification-code":
          if (!formData.code || !formData.email) {
            setErrorMessage(t("login.enterCodeAndEmail"));
            setIsLoading(false);
            return;
          }
          result = await verificationCodeLogin(formData.code, formData.email);
          if (result.success) {
            setSuccessMessage(t("login.loginSuccessRedirecting"));
            setRedirectPath("/recommendations");
            setShouldRedirect(true);
          }
          break;
        case "register":
          if (!formData.email || !formData.firstName || !formData.lastName) {
            setErrorMessage(t("login.enterEmailFirstLastName"));
            setIsLoading(false);
            return;
          }
          result = await registerTeacher(
            formData.email,
            formData.firstName,
            formData.lastName,
          );
          if (result.success) {
            setSuccessMessage(t("login.registrationSuccess"));
            setMode("teacher-password");
          }
          break;
        case "reset-password":
          if (!formData.email) {
            setErrorMessage(t("login.enterEmailAddress"));
            setIsLoading(false);
            return;
          }
          result = await resetPassword(formData.email);
          if (result.success) {
            setSuccessMessage(t("login.resetSuccess"));
            setMode("teacher-password");
          }
          break;
      }

      if (!result?.success) {
        setErrorMessage(result?.message || t("login.operationFailed"));
      }
    } finally {
      setIsLoading(false);
    }
  };

  const getFormFields = () => {
    switch (mode) {
      case "admin":
        return (
          <div className="space-y-6">
            <div className="space-y-2">
              <Label
                htmlFor="email"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Mail className="h-4 w-4" />
                {t("login.adminEmail")}
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder={t("login.enterEmail")}
                className="h-12"
              />
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="password"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Lock className="h-4 w-4" />
                {t("login.adminPassword")}
              </Label>
              <Input
                type="password"
                id="password"
                value={formData.password}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, password: e.target.value }))
                }
                placeholder={t("login.enterPassword")}
                className="h-12"
              />
            </div>
          </div>
        );
      case "teacher":
        return (
          <div className="space-y-2">
            <Label
              htmlFor="email"
              className="text-sm font-medium flex items-center gap-2"
            >
              <GraduationCap className="h-4 w-4" />
              {t("login.teacherEmail")}
            </Label>
            <Input
              type="email"
              id="email"
              value={formData.email}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, email: e.target.value }))
              }
              required
              placeholder={t("login.enterEmail")}
              className="h-12"
            />
          </div>
        );
      case "teacher-password":
        return (
          <div className="space-y-6">
            <div className="space-y-2">
              <Label
                htmlFor="email"
                className="text-sm font-medium flex items-center gap-2"
              >
                <GraduationCap className="h-4 w-4" />
                {t("login.teacherEmail")}
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder={t("login.enterEmail")}
                className="h-12"
              />
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="password"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Lock className="h-4 w-4" />
                {t("login.teacherPassword")}
              </Label>
              <Input
                type="password"
                id="password"
                value={formData.password}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, password: e.target.value }))
                }
                placeholder={t("login.enterPassword")}
                className="h-12"
              />
            </div>
          </div>
        );
      case "register":
        return (
          <div className="space-y-6">
            <div className="space-y-2">
              <Label
                htmlFor="email"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Mail className="h-4 w-4" />
                {t("login.emailAddress")}
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder={t("login.enterEmail")}
                className="h-12"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label
                  htmlFor="firstName"
                  className="text-sm font-medium flex items-center gap-2"
                >
                  <GraduationCap className="h-4 w-4" />
                  {t("login.firstName")}
                </Label>
                <Input
                  type="text"
                  id="firstName"
                  value={formData.firstName}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      firstName: e.target.value,
                    }))
                  }
                  placeholder={t("login.enterFirstName")}
                  className="h-12"
                />
              </div>
              <div className="space-y-2">
                <Label
                  htmlFor="lastName"
                  className="text-sm font-medium flex items-center gap-2"
                >
                  <GraduationCap className="h-4 w-4" />
                  {t("login.lastName")}
                </Label>
                <Input
                  type="text"
                  id="lastName"
                  value={formData.lastName}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      lastName: e.target.value,
                    }))
                  }
                  placeholder={t("login.enterLastName")}
                  className="h-12"
                />
              </div>
            </div>
          </div>
        );
      case "reset-password":
        return (
          <div className="space-y-2">
            <Label
              htmlFor="email"
              className="text-sm font-medium flex items-center gap-2"
            >
              <Mail className="h-4 w-4" />
              {t("login.teacherEmail")}
            </Label>
            <Input
              type="email"
              id="email"
              value={formData.email}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, email: e.target.value }))
              }
              required
              placeholder={t("login.enterEmail")}
              className="h-12"
            />
          </div>
        );
      case "verification-code":
        return (
          <div className="space-y-6">
            <div className="space-y-2">
              <Label
                htmlFor="email"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Mail className="h-4 w-4" />
                {t("login.email")}
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder={t("login.enterEmail")}
                className="h-12"
              />
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="code"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Shield className="h-4 w-4" />
                {t("login.verificationCodeLabel")}
              </Label>
              <Input
                type="text"
                id="code"
                value={formData.code}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, code: e.target.value }))
                }
                placeholder={t("login.enterCode")}
                maxLength={6}
                pattern="[0-9]{6}"
                className="h-12 text-center text-lg tracking-widest"
              />
              <p className="text-xs text-muted-foreground text-center">
                {t("login.checkEmailForCode")}
              </p>
            </div>
          </div>
        );
    }
  };

  const getButtonText = () => {
    if (isLoading) {
      switch (mode) {
        case "admin":
          return t("login.loggingIn");
        case "teacher":
          return t("login.sending");
        case "teacher-password":
          return t("login.loggingIn");
        case "verification-code":
          return t("login.verifying");
        case "register":
          return t("login.registering");
        case "reset-password":
          return t("login.resetting");
        default:
          return t("login.processing");
      }
    }

    switch (mode) {
      case "admin":
        return t("login.loginAsAdmin");
      case "teacher":
        return t("login.sendVerificationCode");
      case "teacher-password":
        return t("login.loginAsTeacher");
      case "verification-code":
        return t("login.verifyCode");
      case "register":
        return t("login.registerTeacher");
      case "reset-password":
        return t("login.resetPassword");
      default:
        return t("common.submit");
    }
  };

  const handleTabChange = (value: string) => {
    if (value === "admin") {
      setMode("admin");
    } else if (value === "teacher") {
      setMode("teacher");
    }
  };

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <Card className="max-w-lg w-full shadow-lg">
        <CardHeader className="text-center pb-6">
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <div className="flex items-center justify-center gap-3 mb-4">
                <div className="w-10 h-10 rounded-lg bg-primary flex items-center justify-center">
                  <LogIn className="h-5 w-5 text-primary-foreground" />
                </div>
                <CardTitle className="text-2xl font-bold text-foreground">
                  {t("login.welcomeBack")}
                </CardTitle>
              </div>
              <p className="text-muted-foreground text-sm">
                {t("login.chooseLoginMethod")}
              </p>
            </div>
            <ThemeToggle />
          </div>
        </CardHeader>

        <CardContent className="space-y-6">
          <Tabs
            defaultValue="teacher"
            className="w-full"
            onValueChange={handleTabChange}
          >
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger value="teacher" className="flex items-center gap-2">
                <GraduationCap className="h-4 w-4" />
                {t("login.teacherLogin")}
              </TabsTrigger>
              <TabsTrigger value="admin" className="flex items-center gap-2">
                <Shield className="h-4 w-4" />
                {t("login.adminLogin")}
              </TabsTrigger>
            </TabsList>

            <TabsContent value="teacher" className="space-y-6">
              <div className="space-y-4">
                <div className="text-center">
                  <h3 className="text-lg font-semibold text-foreground mb-2">
                    {t("login.teacherLogin")}
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    {t("login.teacherSubtitle")}
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                  {getFormFields()}

                  <Button
                    type="submit"
                    disabled={isLoading}
                    className="w-full h-11"
                  >
                    {isLoading ? (
                      <div className="flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground"></div>
                        {getButtonText()}
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <LogIn className="h-4 w-4" />
                        {getButtonText()}
                      </div>
                    )}
                  </Button>
                </form>

                <div className="grid grid-cols-2 gap-2 text-sm">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setMode("register")}
                    className="text-primary hover:text-primary/80"
                  >
                    {t("login.newTeacher")}
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setMode("reset-password")}
                    className="text-primary hover:text-primary/80"
                  >
                    {t("login.forgotPassword")}
                  </Button>
                </div>

                {mode === "teacher" && (
                  <div className="text-center">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setMode("teacher-password")}
                      className="text-muted-foreground hover:text-foreground"
                    >
                      {t("login.orLoginWithPassword")}
                    </Button>
                  </div>
                )}

                {mode === "teacher-password" && (
                  <div className="text-center">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setMode("teacher")}
                      className="text-muted-foreground hover:text-foreground"
                    >
                      {t("login.orLoginWithEmailCode")}
                    </Button>
                  </div>
                )}
              </div>
            </TabsContent>

            <TabsContent value="admin" className="space-y-6">
              <div className="space-y-4">
                <div className="text-center">
                  <h3 className="text-lg font-semibold text-foreground mb-2">
                    {t("login.adminLogin")}
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    {t("login.adminSubtitle")}
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                  {getFormFields()}

                  <Button
                    type="submit"
                    disabled={isLoading}
                    className="w-full h-11"
                  >
                    {isLoading ? (
                      <div className="flex items-center gap-2">
                        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground"></div>
                        {getButtonText()}
                      </div>
                    ) : (
                      <div className="flex items-center gap-2">
                        <LogIn className="h-4 w-4" />
                        {getButtonText()}
                      </div>
                    )}
                  </Button>
                </form>
              </div>
            </TabsContent>
          </Tabs>

          <Separator />

          <div className="text-center">
            <Button
              type="button"
              onClick={handleGuestLogin}
              disabled={isLoading}
              variant="outline"
              className="w-full h-12 text-base font-semibold border-2 border-dashed border-muted-foreground/30 hover:border-primary/50 hover:bg-primary/5 transition-all duration-300"
            >
              {isLoading ? (
                <div className="flex items-center gap-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-muted-foreground"></div>
                  {t("login.continuingAsGuest")}
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4" />
                  <span className="text-muted-foreground">
                    {t("login.guestAccess")}
                  </span>
                </div>
              )}
            </Button>
            <p className="text-xs text-muted-foreground mt-2">
              {t("login.browseWithoutAccount")}
            </p>
          </div>

          {message && (
            <Alert
              className={`${
                isSuccess
                  ? "border-success/20 bg-success/5"
                  : "border-destructive/20 bg-destructive/5"
              }`}
            >
              <AlertCircle
                className={`h-4 w-4 ${
                  isSuccess ? "text-success" : "text-destructive"
                }`}
              />
              <AlertDescription
                className={isSuccess ? "text-success" : "text-destructive"}
              >
                {message}
              </AlertDescription>
            </Alert>
          )}

          <Separator />

          {/* Environment Display */}
          {environment && (
            <div className="flex items-center justify-center gap-2 py-2">
              <Server className="h-3.5 w-3.5 text-muted-foreground" />
              <span className="text-xs text-muted-foreground">
                {t("login.connectingTo")}
              </span>
              <Badge
                variant={getEnvironmentBadgeVariant(environment)}
                className="text-xs font-medium px-2.5 py-0.5"
              >
                {getEnvironmentDisplayText(environment)}
              </Badge>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

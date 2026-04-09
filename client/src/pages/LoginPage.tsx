import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { ThemeToggle } from "@/components/ThemeToggle";
import { LanguageSwitcher } from "@/components/LanguageSwitcher";
import { useAuth } from "@/hooks/useAuth";
import {
  LogIn,
  Mail,
  Lock,
  Shield,
  GraduationCap,
  AlertCircle,
  Users,
} from "lucide-react";
import { useTranslation } from "react-i18next";

type LoginMode =
  | "password"
  | "email-code"
  | "verification-code"
  | "register"
  | "reset-password";

export const LoginPage: React.FC = () => {
  const [mode, setMode] = useState<LoginMode>("email-code");
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
    verificationCodeLogin,
    requestVerificationCode,
    registerTeacher,
    resetPassword,
    user,
  } = useAuth();
  const navigate = useNavigate();
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
        case "password":
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
        case "email-code":
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
            setMode("verification-code");
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
            setMode("email-code");
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
      case "password":
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
            <div className="space-y-2">
              <Label
                htmlFor="password"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Lock className="h-4 w-4" />
                {t("login.password")}
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
      case "email-code":
        return (
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
              required
              placeholder={t("login.enterEmail")}
              className="h-12"
            />
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
              {t("login.emailAddress")}
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
        case "password":
          return t("login.loggingIn");
        case "email-code":
          return t("login.sending");
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
      case "password":
        return t("login.loginWithPassword");
      case "email-code":
        return t("login.sendVerificationCode");
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

  const activeTab =
    mode === "password" || mode === "reset-password" ? "password" : "email-code";

  const getPanelTitle = () => {
    switch (mode) {
      case "register":
        return t("login.register");
      default:
        return t("login.title");
    }
  };

  const getPanelSubtitle = () => {
    switch (mode) {
      case "register":
        return t("login.registerSubtitle");
      case "reset-password":
        return t("login.resetSubtitle");
      case "verification-code":
        return t("login.codeSentSubtitle");
      case "email-code":
        return t("login.emailCodeSubtitle");
      case "password":
      default:
        return t("login.passwordSubtitle");
    }
  };

  const handleTabChange = (value: string) => {
    setMode(value === "password" ? "password" : "email-code");
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
            <div className="flex items-center gap-1">
              <LanguageSwitcher />
              <ThemeToggle />
            </div>
          </div>
        </CardHeader>

        <CardContent className="space-y-6">
          <Tabs
            value={activeTab}
            className="w-full"
            onValueChange={handleTabChange}
          >
            <TabsList className="grid w-full grid-cols-2 mb-6">
              <TabsTrigger
                value="email-code"
                className="flex items-center gap-2"
              >
                <Mail className="h-4 w-4" />
                {t("login.emailCodeLogin")}
              </TabsTrigger>
              <TabsTrigger value="password" className="flex items-center gap-2">
                <Lock className="h-4 w-4" />
                {t("login.passwordLogin")}
              </TabsTrigger>
            </TabsList>

            <TabsContent value={activeTab} className="space-y-6">
              <div className="space-y-4">
                <div className="text-center">
                  <h3 className="text-lg font-semibold text-foreground mb-2">
                    {getPanelTitle()}
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    {getPanelSubtitle()}
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

                {activeTab === "password" && (
                  <div className="grid grid-cols-2 gap-2 text-sm">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setMode("register")}
                      className="text-primary hover:text-primary/80"
                    >
                      {t("login.createAccount")}
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() =>
                        setMode(mode === "reset-password" ? "password" : "reset-password")
                      }
                      className="text-primary hover:text-primary/80"
                    >
                      {t(
                        mode === "reset-password"
                          ? "login.backToPasswordLogin"
                          : "login.forgotPassword",
                      )}
                    </Button>
                  </div>
                )}

                {activeTab === "email-code" && mode === "email-code" && (
                  <div className="text-center">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setMode("register")}
                      className="text-primary hover:text-primary/80"
                    >
                      {t("login.newTeacher")}
                    </Button>
                  </div>
                )}

                {mode === "register" ? (
                  <div className="text-center">
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      onClick={() => setMode("email-code")}
                      className="text-primary hover:text-primary/80"
                    >
                      {t("login.haveAccount")}
                    </Button>
                  </div>
                ) : mode === "verification-code" ? (
                  <div className="text-center">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => setMode("email-code")}
                      className="text-muted-foreground hover:text-foreground"
                    >
                      {t("login.backToEmailCodeLogin")}
                    </Button>
                  </div>
                ) : null}
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
        </CardContent>
      </Card>
    </div>
  );
};

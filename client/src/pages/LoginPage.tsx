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
    guestLogin,
    user,
  } = useAuth();
  const navigate = useNavigate();

  // Handle redirect after user state is updated
  useEffect(() => {
    if (shouldRedirect && user && redirectPath) {
      navigate(redirectPath);
      setShouldRedirect(false);
      setRedirectPath("");
    }
  }, [user, shouldRedirect, redirectPath, navigate]);

  const handleGuestLogin = async () => {
    setIsLoading(true);
    try {
      const result = await guestLogin();
      if (result.success) {
        setMessage("Welcome! You're now browsing as a guest.");
        setRedirectPath("/recommendations");
        setShouldRedirect(true);
      } else {
        setMessage(result.message || "Failed to login as guest");
      }
    } catch {
      setMessage("An unexpected error occurred");
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      let result;
      switch (mode) {
        case "admin":
          if (!formData.email || !formData.password) {
            setMessage("Please enter both email and password");
            setIsLoading(false);
            return;
          }
          result = await login(formData.email, formData.password);
          if (result.success) {
            setMessage("Login successful! Redirecting...");
            setRedirectPath("/recommendations");
            setShouldRedirect(true);
          }
          break;
        case "teacher":
          if (!formData.email) {
            setMessage("Please enter your email address");
            setIsLoading(false);
            return;
          }
          result = await requestVerificationCode(formData.email);
          if (result.success) {
            setMessage(
              "If a user is associated with this email, a 6-digit verification code was sent. Check your email and enter the code below.",
            );
            setMode("verification-code");
          }
          break;
        case "teacher-password":
          if (!formData.email || !formData.password) {
            setMessage("Please enter both email and password");
            setIsLoading(false);
            return;
          }
          result = await teacherLogin(formData.email, formData.password);
          if (result.success) {
            setMessage("Login successful! Redirecting...");
            setRedirectPath("/recommendations");
            setShouldRedirect(true);
          }
          break;
        case "verification-code":
          if (!formData.code || !formData.email) {
            setMessage("Please enter the verification code and email");
            setIsLoading(false);
            return;
          }
          result = await verificationCodeLogin(formData.code, formData.email);
          if (result.success) {
            setMessage("Login successful! Redirecting...");
            setRedirectPath("/recommendations");
            setShouldRedirect(true);
          }
          break;
        case "register":
          if (!formData.email || !formData.firstName || !formData.lastName) {
            setMessage("Please enter email, first name, and last name");
            setIsLoading(false);
            return;
          }
          result = await registerTeacher(
            formData.email,
            formData.firstName,
            formData.lastName,
          );
          if (result.success) {
            setMessage(
              "Registration successful! Check your email for login credentials.",
            );
            setMode("teacher-password");
          }
          break;
        case "reset-password":
          if (!formData.email) {
            setMessage("Please enter your email address");
            setIsLoading(false);
            return;
          }
          result = await resetPassword(formData.email);
          if (result.success) {
            setMessage(
              "Password reset successful! Check your email for the new password.",
            );
            setMode("teacher-password");
          }
          break;
      }

      if (!result?.success) {
        setMessage(result?.message || "Operation failed");
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
                Admin Email
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder="admin@example.com"
                className="h-12"
              />
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="password"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Lock className="h-4 w-4" />
                Admin Password
              </Label>
              <Input
                type="password"
                id="password"
                value={formData.password}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, password: e.target.value }))
                }
                placeholder="Enter your password"
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
              Teacher Email
            </Label>
            <Input
              type="email"
              id="email"
              value={formData.email}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, email: e.target.value }))
              }
              required
              placeholder="teacher@example.com"
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
                Teacher Email
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder="teacher@example.com"
                className="h-12"
              />
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="password"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Lock className="h-4 w-4" />
                Teacher Password
              </Label>
              <Input
                type="password"
                id="password"
                value={formData.password}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, password: e.target.value }))
                }
                placeholder="Enter your password"
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
                Email Address
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder="teacher@example.com"
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
                  First Name
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
                  placeholder="John"
                  className="h-12"
                />
              </div>
              <div className="space-y-2">
                <Label
                  htmlFor="lastName"
                  className="text-sm font-medium flex items-center gap-2"
                >
                  <GraduationCap className="h-4 w-4" />
                  Last Name
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
                  placeholder="Doe"
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
              Teacher Email
            </Label>
            <Input
              type="email"
              id="email"
              value={formData.email}
              onChange={(e) =>
                setFormData((prev) => ({ ...prev, email: e.target.value }))
              }
              required
              placeholder="teacher@example.com"
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
                Email
              </Label>
              <Input
                type="email"
                id="email"
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                placeholder="teacher@example.com"
                className="h-12"
              />
            </div>
            <div className="space-y-2">
              <Label
                htmlFor="code"
                className="text-sm font-medium flex items-center gap-2"
              >
                <Shield className="h-4 w-4" />
                6-Digit Verification Code
              </Label>
              <Input
                type="text"
                id="code"
                value={formData.code}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, code: e.target.value }))
                }
                placeholder="Enter 6-digit code"
                maxLength={6}
                pattern="[0-9]{6}"
                className="h-12 text-center text-lg tracking-widest"
              />
              <p className="text-xs text-muted-foreground text-center">
                Check your email for the 6-digit verification code
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
          return "Logging in...";
        case "teacher":
          return "Sending...";
        case "teacher-password":
          return "Logging in...";
        case "verification-code":
          return "Verifying...";
        case "register":
          return "Registering...";
        case "reset-password":
          return "Resetting...";
        default:
          return "Processing...";
      }
    }

    switch (mode) {
      case "admin":
        return "Login as Admin";
      case "teacher":
        return "Send Verification Code";
      case "teacher-password":
        return "Login as Teacher";
      case "verification-code":
        return "Verify Code";
      case "register":
        return "Register Teacher";
      case "reset-password":
        return "Reset Password";
      default:
        return "Submit";
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
    <div className="min-h-screen bg-gradient-to-br from-background via-background to-muted/20 flex items-center justify-center p-4">
      <Card className="max-w-lg w-full shadow-2xl border-0 bg-card/95 backdrop-blur-xl">
        <CardHeader className="text-center pb-6">
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <div className="flex items-center justify-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-primary to-primary/80 flex items-center justify-center shadow-lg">
                  <LogIn className="h-6 w-6 text-primary-foreground" />
                </div>
                <CardTitle className="text-3xl font-bold text-foreground">
                  Welcome Back
                </CardTitle>
              </div>
              <p className="text-muted-foreground leading-relaxed">
                Choose your login method to continue
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
                Teacher
              </TabsTrigger>
              <TabsTrigger value="admin" className="flex items-center gap-2">
                <Shield className="h-4 w-4" />
                Admin
              </TabsTrigger>
            </TabsList>

            <TabsContent value="teacher" className="space-y-6">
              <div className="space-y-4">
                <div className="text-center">
                  <h3 className="text-lg font-semibold text-foreground mb-2">
                    Teacher Login
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Access your teaching resources and activity library
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                  {getFormFields()}

                  <Button
                    type="submit"
                    disabled={isLoading}
                    className="w-full h-12 text-base font-semibold bg-gradient-to-r from-primary to-primary/90 hover:from-primary/90 hover:to-primary shadow-lg hover:shadow-xl transition-all duration-300"
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
                    New Teacher?
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setMode("reset-password")}
                    className="text-primary hover:text-primary/80"
                  >
                    Forgot Password?
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
                      Or login with password
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
                      Or login with email code
                    </Button>
                  </div>
                )}
              </div>
            </TabsContent>

            <TabsContent value="admin" className="space-y-6">
              <div className="space-y-4">
                <div className="text-center">
                  <h3 className="text-lg font-semibold text-foreground mb-2">
                    Admin Login
                  </h3>
                  <p className="text-sm text-muted-foreground">
                    Access administrative functions and user management
                  </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                  {getFormFields()}

                  <Button
                    type="submit"
                    disabled={isLoading}
                    className="w-full h-12 text-base font-semibold bg-gradient-to-r from-primary to-primary/90 hover:from-primary/90 hover:to-primary shadow-lg hover:shadow-xl transition-all duration-300"
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
                  Continuing as Guest...
                </div>
              ) : (
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4" />
                  <span className="text-muted-foreground">
                    Continue as Guest
                  </span>
                </div>
              )}
            </Button>
            <p className="text-xs text-muted-foreground mt-2">
              Browse activities without creating an account
            </p>
          </div>

          {message && (
            <Alert
              className={`${
                message.includes("successful")
                  ? "border-success/20 bg-success/5"
                  : "border-destructive/20 bg-destructive/5"
              }`}
            >
              <AlertCircle
                className={`h-4 w-4 ${
                  message.includes("successful")
                    ? "text-success"
                    : "text-destructive"
                }`}
              />
              <AlertDescription
                className={
                  message.includes("successful")
                    ? "text-success"
                    : "text-destructive"
                }
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

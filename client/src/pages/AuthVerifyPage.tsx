import React, { useState, useEffect } from "react";
import { useSearchParams, Link, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/hooks/useAuth";

export const AuthVerifyPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { verificationCodeLogin } = useAuth();

  const [code, setCode] = useState("");
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    // Check if code is in URL parameters and pre-fill it
    const codeFromUrl = searchParams.get("code");
    const emailFromUrl = searchParams.get("email");
    if (codeFromUrl) {
      setCode(codeFromUrl);
    }
    if (emailFromUrl) {
      setEmail(emailFromUrl);
    }
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!code || !email) {
      setMessage("Please enter both the verification code and email");
      return;
    }

    setIsLoading(true);
    const result = await verificationCodeLogin(code, email);

    if (result.success) {
      setMessage("Login successful! Redirecting...");
      setTimeout(() => {
        // Redirect to recommendations page
        navigate("/recommendations");
      }, 1000);
    } else {
      setMessage(result.message || "Invalid or expired code");
    }
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen app-gradient text-foreground flex items-center justify-center p-4">
      <div className="max-w-md w-full panel p-6 sm:p-8">
        <h1 className="text-2xl sm:text-3xl font-semibold text-center mb-2">
          Verify Your Login
        </h1>
        <p className="text-center text-sm text-muted-foreground mb-6">
          Enter the 6-digit code from your email to complete login
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label
              htmlFor="email"
              className="block text-sm font-medium text-foreground mb-2"
            >
              Email Address
            </Label>
            <Input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="Enter your email address"
              required
              className="w-full"
            />
          </div>
          <div>
            <Label
              htmlFor="verification_code"
              className="block text-sm font-medium text-foreground mb-2"
            >
              6-Digit Verification Code
            </Label>
            <Input
              id="verification_code"
              type="text"
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="Enter 6-digit code"
              maxLength={6}
              pattern="[0-9]{6}"
              required
              className="w-full"
            />
          </div>

          <Button type="submit" disabled={isLoading} className="w-full">
            {isLoading ? "Verifying..." : "Verify & Login"}
          </Button>
        </form>

        {message && (
          <div
            className={`mt-4 text-center text-sm ${
              message.includes("successful")
                ? "text-success"
                : "text-destructive"
            }`}
          >
            {message}
          </div>
        )}

        <div className="mt-6 text-center">
          <Link
            to="/login"
            className="text-primary hover:text-primary/80 text-sm"
          >
            ← Back to Login
          </Link>
        </div>
      </div>
    </div>
  );
};

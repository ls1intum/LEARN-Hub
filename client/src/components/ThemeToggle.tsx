import React from "react";
import { Button } from "@/components/ui/button";
import { useTheme } from "@/hooks/useTheme";
import { Sun, Moon } from "lucide-react";

interface ThemeToggleProps {
  className?: string;
  size?: "sm" | "default" | "lg" | "icon";
  variant?:
    | "default"
    | "destructive"
    | "outline"
    | "secondary"
    | "ghost"
    | "link";
}

export const ThemeToggle: React.FC<ThemeToggleProps> = ({
  className,
  size = "icon",
  variant = "ghost",
}) => {
  const { theme, toggleTheme } = useTheme();

  return (
    <Button
      variant={variant}
      size={size}
      onClick={toggleTheme}
      className={className}
      aria-label={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
    >
      {theme === "light" ? (
        <Moon className="h-4 w-4" />
      ) : (
        <Sun className="h-4 w-4" />
      )}
    </Button>
  );
};

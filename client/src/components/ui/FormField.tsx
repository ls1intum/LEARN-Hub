import React from "react";
import { Label } from "@/components/ui/label";

interface FormFieldProps {
  label: string;
  required?: boolean;
  children: React.ReactNode;
  className?: string;
  htmlFor?: string;
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  required = false,
  children,
  className = "",
  htmlFor,
}) => {
  return (
    <div className={className}>
      <Label htmlFor={htmlFor}>
        {label}
        {required && <span className="text-destructive ml-1">*</span>}
      </Label>
      {children}
    </div>
  );
};

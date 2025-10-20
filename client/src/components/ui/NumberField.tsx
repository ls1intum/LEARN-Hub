import React from "react";
import { Input } from "@/components/ui/input";

interface NumberFieldProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  step?: number;
  placeholder?: string;
  className?: string;
  id?: string;
}

export const NumberField: React.FC<NumberFieldProps> = ({
  value,
  onChange,
  min,
  max,
  step = 1,
  placeholder,
  className = "",
  id,
}) => {
  return (
    <Input
      id={id}
      type="number"
      min={min}
      max={max}
      step={step}
      value={value}
      onChange={(e) => onChange(parseInt(e.target.value) || 0)}
      placeholder={placeholder}
      className={className}
    />
  );
};

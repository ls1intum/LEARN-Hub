import React from "react";
import { Slider } from "@/components/ui/slider";
import type { LucideIcon } from "lucide-react";

interface RangeFilterSliderProps {
  icon: LucideIcon;
  label: string;
  value: [number, number] | [number];
  onChange: (value: number[]) => void;
  min: number;
  max: number;
  step?: number;
  /** Format the displayed value. Defaults to showing the raw numbers. */
  formatValue?: (value: [number, number] | [number]) => string;
}

export const RangeFilterSlider: React.FC<RangeFilterSliderProps> = ({
  icon: Icon,
  label,
  value,
  onChange,
  min,
  max,
  step = 1,
  formatValue,
}) => {
  const displayValue = formatValue
    ? formatValue(value)
    : value.length === 1
      ? `${value[0]}`
      : `${value[0]}–${value[1]}`;

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-1.5">
          <Icon className="h-3.5 w-3.5 text-muted-foreground" />
          <span className="text-sm font-medium">{label}</span>
        </div>
        <span className="text-sm font-semibold text-primary tabular-nums">
          {displayValue}
        </span>
      </div>
      <Slider
        value={value}
        onValueChange={onChange}
        min={min}
        max={max || min + 1}
        step={step}
      />
    </div>
  );
};

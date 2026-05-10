import React from "react";
import { Link } from "react-router-dom";
import { ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

export interface BreadcrumbItem {
  label: string;
  href?: string;
  state?: unknown;
  onClick?: () => void;
}

interface BreadcrumbProps {
  items: BreadcrumbItem[];
  className?: string;
}

export const Breadcrumb: React.FC<BreadcrumbProps> = ({ items, className }) => {
  if (items.length === 0) return null;

  return (
    <nav
      aria-label="Breadcrumb"
      className={cn(
        "flex items-center gap-1 text-sm text-muted-foreground",
        className,
      )}
    >
      {items.map((item, index) => {
        const isLast = index === items.length - 1;
        return (
          <React.Fragment key={index}>
            {index > 0 && (
              <ChevronRight
                className="h-3.5 w-3.5 shrink-0 text-muted-foreground/50"
                aria-hidden
              />
            )}
            {isLast ? (
              <span
                className="font-medium text-foreground truncate"
                aria-current="page"
              >
                {item.label}
              </span>
            ) : item.onClick ? (
              <button
                type="button"
                onClick={item.onClick}
                className="hover:text-foreground transition-colors shrink-0"
              >
                {item.label}
              </button>
            ) : item.href ? (
              <Link
                to={item.href}
                state={item.state}
                className="hover:text-foreground transition-colors shrink-0"
              >
                {item.label}
              </Link>
            ) : (
              <span className="shrink-0">{item.label}</span>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
};

import { cva } from "class-variance-authority";

export const buttonVariants = cva(
  "inline-flex items-center justify-center whitespace-nowrap rounded-xl text-sm font-medium ring-offset-background transition-all duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 active:translate-y-px disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        default:
          "bg-primary text-primary-foreground hover:bg-primary/95 shadow-[0_14px_30px_-18px_hsl(var(--primary)/0.7)] hover:shadow-[0_18px_36px_-18px_hsl(var(--primary)/0.78)]",
        destructive:
          "bg-destructive text-destructive-foreground hover:bg-destructive/92 shadow-[0_14px_28px_-18px_hsl(var(--destructive)/0.65)]",
        outline:
          "border border-border/80 bg-background/80 hover:bg-accent/90 hover:text-accent-foreground hover:border-primary/20",
        secondary:
          "border border-white/50 bg-secondary/90 text-secondary-foreground hover:bg-secondary shadow-[0_14px_28px_-24px_rgba(15,23,42,0.35)] dark:border-white/5",
        ghost: "hover:bg-accent/85 hover:text-accent-foreground",
        link: "text-primary underline-offset-4 hover:underline",
        success:
          "bg-success text-success-foreground hover:bg-success/92 shadow-[0_14px_28px_-18px_hsl(var(--success)/0.55)]",
      },
      size: {
        default: "h-10 px-4 py-2",
        sm: "h-9 rounded-md px-3 text-xs",
        lg: "h-12 rounded-lg px-8 text-base",
        icon: "h-10 w-10",
      },
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  },
);

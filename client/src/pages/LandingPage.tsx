import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { Footer } from "@/components/layout/Footer";
import { Button } from "@/components/ui/button";
import {
  Sparkles,
  BookOpen,
  Users,
  GraduationCap,
  ChevronRight,
  ArrowRight,
  Brain,
  Search,
  Star,
} from "lucide-react";
import { cn } from "@/lib/utils";

/* ─────────────────────────────────────────────
   App Screenshot Placeholder
───────────────────────────────────────────── */
const AppScreenshotPlaceholder: React.FC = () => (
  <div className="relative w-full aspect-[16/10] rounded-xl overflow-hidden shadow-2xl border border-border bg-card">
    {/* Mock app chrome */}
    <div className="h-9 bg-muted border-b border-border flex items-center gap-2 px-3">
      <div className="flex gap-1.5">
        <span className="w-3 h-3 rounded-full bg-red-400/70" />
        <span className="w-3 h-3 rounded-full bg-yellow-400/70" />
        <span className="w-3 h-3 rounded-full bg-green-400/70" />
      </div>
      <div className="flex-1 mx-3">
        <div className="h-5 bg-background rounded-full w-1/2 mx-auto" />
      </div>
    </div>

    {/* Mock sidebar + content */}
    <div className="flex h-[calc(100%-2.25rem)]">
      {/* Sidebar */}
      <div className="w-16 sm:w-48 bg-card border-r border-border p-2 sm:p-3 flex flex-col gap-2 shrink-0">
        <div className="flex items-center gap-2 mb-2">
          <div className="h-6 w-6 rounded bg-primary shrink-0" />
          <div className="h-3 bg-muted rounded w-20 hidden sm:block" />
        </div>
        {[...Array(5)].map((_, i) => (
          <div
            key={i}
            className={cn(
              "flex items-center gap-2 rounded px-2 py-1.5",
              i === 0 && "bg-primary/10",
            )}
          >
            <div
              className={cn(
                "h-4 w-4 rounded shrink-0",
                i === 0 ? "bg-primary" : "bg-muted",
              )}
            />
            <div
              className={cn(
                "h-2.5 rounded w-16 hidden sm:block",
                i === 0 ? "bg-primary/40" : "bg-muted",
              )}
            />
          </div>
        ))}
      </div>

      {/* Main content */}
      <div className="flex-1 p-3 sm:p-4 bg-background overflow-hidden">
        <div className="h-8 sm:h-10 bg-muted rounded-lg mb-3 sm:mb-4 flex items-center gap-2 px-3">
          <div className="h-3.5 w-3.5 rounded bg-muted-foreground/30 shrink-0" />
          <div className="h-2.5 bg-muted-foreground/20 rounded w-32" />
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 sm:gap-3">
          {[
            { color: "bg-blue-500/20", accent: "bg-blue-500" },
            { color: "bg-green-500/20", accent: "bg-green-500" },
            { color: "bg-purple-500/20", accent: "bg-purple-500" },
            { color: "bg-orange-500/20", accent: "bg-orange-500" },
            { color: "bg-pink-500/20", accent: "bg-pink-500" },
            { color: "bg-teal-500/20", accent: "bg-teal-500" },
          ].map((card, i) => (
            <div
              key={i}
              className={cn("rounded-lg p-2 sm:p-3 border border-border", card.color)}
            >
              <div className={cn("h-2 sm:h-3 rounded w-3/4 mb-1.5", card.accent)} />
              <div className="space-y-1">
                <div className="h-1.5 bg-muted-foreground/30 rounded w-full" />
                <div className="h-1.5 bg-muted-foreground/20 rounded w-4/5" />
              </div>
              <div className="flex items-center gap-1 mt-2">
                <Star className="h-2.5 w-2.5 text-yellow-500 fill-yellow-500" />
                <div className="h-1.5 bg-muted-foreground/20 rounded w-6" />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>

    <div className="absolute bottom-2 right-2 text-[9px] text-muted-foreground bg-background/80 rounded px-1.5 py-0.5 backdrop-blur-sm">
      LEARN-Hub · Activity Library
    </div>
  </div>
);

/* ─────────────────────────────────────────────
   Hero Section
───────────────────────────────────────────── */
const HeroSection: React.FC = () => {
  const navigate = useNavigate();

  const scrollToFeatures = () => {
    document.getElementById("features")?.scrollIntoView({ behavior: "smooth" });
  };

  return (
    <section className="relative overflow-hidden bg-background">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-transparent"
      />
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-16 sm:py-24">
        <div className="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          <div>
            <button
              onClick={scrollToFeatures}
              className="inline-flex items-center gap-2 rounded-full border border-primary/20 bg-primary/5 px-3 py-1 text-sm text-primary font-medium mb-6 hover:bg-primary/10 transition-colors"
            >
              <Sparkles className="h-3.5 w-3.5" />
              AI-Powered CS Education
            </button>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-foreground leading-tight tracking-tight mb-6">
              Discover Activities for{" "}
              <span className="text-primary">Computer Science</span> Education
            </h1>

            <p className="text-lg sm:text-xl text-muted-foreground leading-relaxed mb-8 max-w-lg">
              LEARN-Hub helps CS teachers find, organise, and plan high-quality
              unplugged and digital learning activities — powered by AI
              recommendations tailored to your classroom.
            </p>

            <div className="flex flex-wrap gap-3">
              <Button
                size="lg"
                className="h-12 px-6 text-base"
                onClick={() => navigate("/recommendations")}
              >
                Get Recommendations
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="h-12 px-6 text-base"
                onClick={() => navigate("/library")}
              >
                <BookOpen className="mr-2 h-4 w-4" />
                Browse Library
              </Button>
            </div>

            <p className="mt-4 text-sm text-muted-foreground">
              No account required to browse.{" "}
              <Link
                to="/login"
                className="text-primary underline-offset-4 hover:underline"
              >
                Sign in
              </Link>{" "}
              for personalised features.
            </p>
          </div>

          <div className="relative">
            <div
              aria-hidden="true"
              className="absolute -inset-4 bg-gradient-to-tr from-primary/10 to-primary/5 rounded-2xl blur-2xl"
            />
            <div className="relative">
              <AppScreenshotPlaceholder />
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

/* ─────────────────────────────────────────────
   Trust Section
───────────────────────────────────────────── */
const TrustSection: React.FC = () => (
  <section className="border-y border-border bg-muted/30 py-10">
    <div className="max-w-7xl mx-auto px-4 sm:px-6 text-center">
      <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-6">
        Research-backed · Built at
      </p>
      <div className="flex flex-wrap items-center justify-center gap-8 sm:gap-12">
        <div className="flex items-center gap-2 opacity-70 hover:opacity-100 transition-opacity">
          <div className="h-8 w-8 rounded bg-primary/20 flex items-center justify-center">
            <GraduationCap className="h-5 w-5 text-primary" />
          </div>
          <div className="text-left">
            <p className="text-xs font-bold text-foreground leading-tight">TUM</p>
            <p className="text-[10px] text-muted-foreground leading-tight">
              Technical University of Munich
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 opacity-70 hover:opacity-100 transition-opacity">
          <div className="h-8 w-8 rounded bg-primary/20 flex items-center justify-center">
            <Brain className="h-5 w-5 text-primary" />
          </div>
          <div className="text-left">
            <p className="text-xs font-bold text-foreground leading-tight">AET</p>
            <p className="text-[10px] text-muted-foreground leading-tight">
              Applied Education Technologies
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2 opacity-70 hover:opacity-100 transition-opacity">
          <div className="h-8 w-8 rounded bg-primary/20 flex items-center justify-center">
            <BookOpen className="h-5 w-5 text-primary" />
          </div>
          <div className="text-left">
            <p className="text-xs font-bold text-foreground leading-tight">CS Unplugged</p>
            <p className="text-[10px] text-muted-foreground leading-tight">
              Curated activity sources
            </p>
          </div>
        </div>
      </div>
    </div>
  </section>
);

/* ─────────────────────────────────────────────
   Feature Cards
───────────────────────────────────────────── */
interface FeatureCardProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  imagePlaceholder?: React.ReactNode;
}

const FeatureCard: React.FC<FeatureCardProps> = ({
  icon,
  title,
  description,
  imagePlaceholder,
}) => (
  <div className="flex flex-col rounded-xl border border-border bg-card p-6 hover:shadow-md transition-shadow">
    <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-lg bg-primary/10 text-primary">
      {icon}
    </div>
    <h3 className="text-lg font-semibold text-foreground mb-2">{title}</h3>
    <p className="text-sm text-muted-foreground leading-relaxed flex-1">
      {description}
    </p>
    {imagePlaceholder && (
      <div className="mt-4 rounded-lg overflow-hidden border border-border bg-muted/50 aspect-[16/9] flex items-center justify-center text-muted-foreground text-xs">
        {imagePlaceholder}
      </div>
    )}
  </div>
);

const FeaturesSection: React.FC = () => (
  <section id="features" className="bg-background py-20 sm:py-28">
    <div className="max-w-7xl mx-auto px-4 sm:px-6">
      <div className="text-center mb-14">
        <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
          Everything you need to plan great CS lessons
        </h2>
        <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
          From AI-driven recommendations to a curated library of unplugged and
          digital activities — LEARN-Hub has teachers covered.
        </p>
      </div>
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <FeatureCard
          icon={<Sparkles className="h-6 w-6" />}
          title="AI-Powered Recommendations"
          description="Describe your class, topic, or learning goal and our AI matches you with the most relevant activities from a curated dataset — in seconds."
          imagePlaceholder={
            <div className="w-full h-full flex flex-col gap-1.5 p-3">
              <div className="h-2.5 bg-primary/30 rounded-full w-3/4 mx-auto" />
              {[80, 65, 50].map((w, i) => (
                <div key={i} className="h-2 bg-muted rounded-full" style={{ width: `${w}%` }} />
              ))}
              <div className="text-[9px] text-muted-foreground mt-1 text-center">
                [ AI recommendation results ]
              </div>
            </div>
          }
        />
        <FeatureCard
          icon={<BookOpen className="h-6 w-6" />}
          title="Curated Activity Library"
          description="Browse and filter hundreds of CS education activities sourced from CS Unplugged, Code.org, Barefoot, and more — all in one place."
          imagePlaceholder={
            <div className="w-full h-full flex flex-col gap-2 p-3">
              <div className="flex gap-1.5">
                {["blue", "green", "purple"].map((c) => (
                  <div key={c} className="h-6 flex-1 rounded bg-muted-foreground/20" />
                ))}
              </div>
              <div className="grid grid-cols-2 gap-1.5 flex-1">
                {[...Array(4)].map((_, i) => (
                  <div key={i} className="rounded bg-muted-foreground/10 h-8" />
                ))}
              </div>
              <div className="text-[9px] text-muted-foreground text-center">
                [ Activity library grid ]
              </div>
            </div>
          }
        />
        <FeatureCard
          icon={<Search className="h-6 w-6" />}
          title="Smart Filtering"
          description="Filter by topic, age group, duration, skill level, and teaching format. Find exactly the right activity for any classroom situation."
        />
        <FeatureCard
          icon={<Star className="h-6 w-6" />}
          title="Favourites & Lesson Plans"
          description="Save activities to your favourites and build reusable lesson plans. Your personalised teaching toolkit, always ready."
        />
        <FeatureCard
          icon={<Users className="h-6 w-6" />}
          title="Built for Teachers"
          description="Designed with secondary and university CS teachers in mind. Every feature targets the real-world challenges of planning engaging CS lessons."
        />
        <FeatureCard
          icon={<Brain className="h-6 w-6" />}
          title="Research-Backed"
          description="Developed at TUM's Applied Education Technologies chair. Activities are reviewed and tagged by education researchers and practitioners."
        />
      </div>
    </div>
  </section>
);

/* ─────────────────────────────────────────────
   Feature Image Rows (alternating layout)
───────────────────────────────────────────── */
const FeatureImageRow: React.FC<{
  title: string;
  description: string;
  cta: { label: string; path: string };
  imageContent: React.ReactNode;
  reverse?: boolean;
}> = ({ title, description, cta, imageContent, reverse }) => {
  const navigate = useNavigate();
  return (
    <div
      className={cn(
        "flex flex-col gap-10 items-center lg:gap-16",
        reverse ? "lg:flex-row-reverse" : "lg:flex-row",
      )}
    >
      <div className="flex-1">
        <h3 className="text-2xl sm:text-3xl font-bold text-foreground mb-4">
          {title}
        </h3>
        <p className="text-base text-muted-foreground leading-relaxed mb-6">
          {description}
        </p>
        <Button variant="outline" onClick={() => navigate(cta.path)} className="gap-2">
          {cta.label}
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
      <div className="flex-1 w-full">
        <div className="rounded-xl border border-border bg-card overflow-hidden shadow-lg aspect-[4/3]">
          {imageContent}
        </div>
      </div>
    </div>
  );
};

const FeatureImageSection: React.FC = () => (
  <section className="bg-muted/20 py-20 sm:py-28 border-y border-border">
    <div className="max-w-7xl mx-auto px-4 sm:px-6 space-y-20 sm:space-y-28">
      <FeatureImageRow
        title="Tell us what you need — we find the right activity"
        description="Enter your learning objectives, student age group, and available time. The AI recommendation engine analyses the full activity library and ranks the best matches for your exact teaching context."
        cta={{ label: "Try Recommendations", path: "/recommendations" }}
        imageContent={
          <div className="h-full bg-gradient-to-br from-primary/10 to-background flex flex-col gap-3 p-6">
            <div className="h-10 rounded-lg bg-primary/10 border border-primary/20 flex items-center gap-3 px-4">
              <Sparkles className="h-4 w-4 text-primary shrink-0" />
              <div className="h-2.5 bg-primary/30 rounded-full w-2/3" />
            </div>
            <div className="space-y-2.5 flex-1">
              {[90, 78, 65, 55].map((score, i) => (
                <div key={i} className="flex items-center gap-3 rounded-lg border border-border bg-card p-3">
                  <div className="h-8 w-8 rounded bg-primary/15 shrink-0" />
                  <div className="flex-1 space-y-1.5">
                    <div className="h-2.5 bg-muted rounded w-3/4" />
                    <div className="h-2 bg-muted rounded w-1/2" />
                  </div>
                  <div className="text-[11px] font-bold text-primary shrink-0">{score}%</div>
                </div>
              ))}
            </div>
            <p className="text-[10px] text-muted-foreground text-center">
              [ AI recommendation results view ]
            </p>
          </div>
        }
      />
      <FeatureImageRow
        reverse
        title="Hundreds of curated activities, one search away"
        description="Our library spans CS Unplugged, Code.org, Barefoot Computing, and more. Filter by topic, format, grade level, and duration to find the perfect fit."
        cta={{ label: "Browse Library", path: "/library" }}
        imageContent={
          <div className="h-full bg-background flex flex-col gap-3 p-4">
            <div className="flex gap-2">
              {["All", "Unplugged", "Digital", "< 30 min"].map((f, i) => (
                <div
                  key={f}
                  className={cn(
                    "rounded-full px-3 py-1 text-[10px] border font-medium",
                    i === 0
                      ? "bg-primary text-primary-foreground border-primary"
                      : "bg-card border-border text-muted-foreground",
                  )}
                >
                  {f}
                </div>
              ))}
            </div>
            <div className="grid grid-cols-2 gap-2 flex-1">
              {[
                { label: "Sorting Networks", tag: "Algorithms" },
                { label: "Binary Bracelets", tag: "Data" },
                { label: "Parity Magic", tag: "Error Correction" },
                { label: "Treasure Hunt", tag: "Networks" },
              ].map(({ label, tag }) => (
                <div key={label} className="rounded-lg border border-border bg-card p-2.5 flex flex-col gap-1.5">
                  <div className="h-1.5 w-12 rounded-full bg-primary/40" />
                  <p className="text-[10px] font-semibold text-foreground leading-tight">{label}</p>
                  <span className="text-[9px] rounded px-1 py-0.5 bg-primary/10 text-primary w-fit font-medium">
                    {tag}
                  </span>
                </div>
              ))}
            </div>
            <p className="text-[10px] text-muted-foreground text-center">
              [ Activity library view ]
            </p>
          </div>
        }
      />
    </div>
  </section>
);

/* ─────────────────────────────────────────────
   Final CTA
───────────────────────────────────────────── */
const CtaSection: React.FC = () => {
  const navigate = useNavigate();
  return (
    <section className="bg-primary text-primary-foreground py-20 sm:py-28">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 text-center">
        <h2 className="text-3xl sm:text-4xl font-bold mb-5 leading-tight">
          Ready to transform your CS lessons?
        </h2>
        <p className="text-lg opacity-80 mb-10 leading-relaxed">
          Join teachers already using LEARN-Hub to discover and plan engaging
          computer science activities. No account needed to start.
        </p>
        <div className="flex flex-wrap gap-4 justify-center">
          <Button
            size="lg"
            className="h-12 px-8 text-base bg-primary-foreground text-primary hover:bg-primary-foreground/90"
            onClick={() => navigate("/recommendations")}
          >
            Get Started Free
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
          <Button
            size="lg"
            variant="outline"
            className="h-12 px-8 text-base bg-transparent border-primary-foreground/40 text-primary-foreground hover:bg-primary-foreground/10"
            onClick={() => navigate("/library")}
          >
            <BookOpen className="mr-2 h-4 w-4" />
            Browse Activities
          </Button>
        </div>
        <p className="mt-6 text-sm opacity-60">
          Teachers can{" "}
          <Link
            to="/login"
            className="underline underline-offset-4 hover:opacity-100 transition-opacity"
          >
            create a free account
          </Link>{" "}
          for favourites, history, and lesson plans.
        </p>
      </div>
    </section>
  );
};

/* ─────────────────────────────────────────────
   Landing Page
   AppShell provides the header and footer — this component
   is content-only, filling the middle scroll area.
───────────────────────────────────────────── */
export const LandingPage: React.FC = () => (
  <div className="h-full overflow-y-auto">
    <HeroSection />
    <TrustSection />
    <FeaturesSection />
    <FeatureImageSection />
    <CtaSection />
    {/* Footer lives inside the scroll container on the landing page,
        so it appears at the end of the content rather than pinned to the viewport. */}
    <Footer />
  </div>
);

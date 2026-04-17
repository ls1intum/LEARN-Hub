import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
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
   Hero Section
───────────────────────────────────────────── */
const HeroSection: React.FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();

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
              {t("landingPage.hero.badge")}
            </button>

            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-foreground leading-tight tracking-tight mb-6">
              {t("landingPage.hero.titlePrefix")}{" "}
              <span className="text-primary">
                {t("landingPage.hero.titleHighlight")}
              </span>{" "}
              {t("landingPage.hero.titleSuffix")}
            </h1>

            <p className="text-lg sm:text-xl text-muted-foreground leading-relaxed mb-8 max-w-lg">
              {t("landingPage.hero.description")}
            </p>

            <div className="flex flex-wrap gap-3">
              <Button
                size="lg"
                className="h-12 px-6 text-base"
                onClick={() => navigate("/recommendations")}
              >
                {t("landingPage.hero.primaryCta")}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="h-12 px-6 text-base"
                onClick={() => navigate("/library")}
              >
                <BookOpen className="mr-2 h-4 w-4" />
                {t("landingPage.hero.secondaryCta")}
              </Button>
            </div>

            <p className="mt-4 text-sm text-muted-foreground">
              {t("landingPage.hero.signInPrompt")}{" "}
              <Link
                to="/login"
                className="text-primary underline-offset-4 hover:underline"
              >
                {t("landingPage.hero.signInLink")}
              </Link>{" "}
              {t("landingPage.hero.signInSuffix")}
            </p>
          </div>

          <div className="relative">
            <div
              aria-hidden="true"
              className="absolute -inset-4 bg-gradient-to-tr from-primary/10 to-primary/5 rounded-2xl blur-2xl"
            />
            <div className="relative">
              <img
                src="/hero-image.png"
                alt={t("landingPage.hero.imageAlt")}
                className="w-full rounded-xl"
              />
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
const TrustSection: React.FC = () => {
  const { t } = useTranslation();

  return (
    <section className="border-y border-border bg-muted/30 py-10">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 text-center">
        <p className="text-sm font-medium text-muted-foreground uppercase tracking-wider mb-6">
          {t("landingPage.trust.eyebrow")}
        </p>
        <div className="flex flex-wrap items-center justify-center gap-8 sm:gap-12">
          <div className="flex items-center gap-2 opacity-70 hover:opacity-100 transition-opacity">
            <div className="h-8 w-8 rounded bg-primary/20 flex items-center justify-center">
              <GraduationCap className="h-5 w-5 text-primary" />
            </div>
            <div className="text-left">
              <p className="text-xs font-bold text-foreground leading-tight">
                TUM
              </p>
              <p className="text-[10px] text-muted-foreground leading-tight">
                {t("landingPage.trust.tum")}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-2 opacity-70 hover:opacity-100 transition-opacity">
            <div className="h-8 w-8 rounded bg-primary/20 flex items-center justify-center">
              <Brain className="h-5 w-5 text-primary" />
            </div>
            <div className="text-left">
              <p className="text-xs font-bold text-foreground leading-tight">
                AET
              </p>
              <p className="text-[10px] text-muted-foreground leading-tight">
                {t("landingPage.trust.aet")}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
};

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

const FeaturesSection: React.FC = () => {
  const { t } = useTranslation();

  return (
    <section id="features" className="bg-background py-20 sm:py-28">
      <div className="max-w-7xl mx-auto px-4 sm:px-6">
        <div className="text-center mb-14">
          <h2 className="text-3xl sm:text-4xl font-bold text-foreground mb-4">
            {t("landingPage.features.title")}
          </h2>
          <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
            {t("landingPage.features.description")}
          </p>
        </div>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <FeatureCard
            icon={<Sparkles className="h-6 w-6" />}
            title={t("landingPage.features.cards.recommendations.title")}
            description={t(
              "landingPage.features.cards.recommendations.description",
            )}
          />
          <FeatureCard
            icon={<BookOpen className="h-6 w-6" />}
            title={t("landingPage.features.cards.library.title")}
            description={t("landingPage.features.cards.library.description")}
          />
          <FeatureCard
            icon={<Search className="h-6 w-6" />}
            title={t("landingPage.features.cards.filtering.title")}
            description={t("landingPage.features.cards.filtering.description")}
          />
          <FeatureCard
            icon={<Star className="h-6 w-6" />}
            title={t("landingPage.features.cards.favourites.title")}
            description={t("landingPage.features.cards.favourites.description")}
          />
          <FeatureCard
            icon={<Users className="h-6 w-6" />}
            title={t("landingPage.features.cards.teachers.title")}
            description={t("landingPage.features.cards.teachers.description")}
          />
          <FeatureCard
            icon={<Brain className="h-6 w-6" />}
            title={t("landingPage.features.cards.research.title")}
            description={t("landingPage.features.cards.research.description")}
          />
        </div>
      </div>
    </section>
  );
};

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
        <Button
          variant="outline"
          onClick={() => navigate(cta.path)}
          className="gap-2"
        >
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

const FeatureImageSection: React.FC = () => {
  const { t } = useTranslation();

  return (
    <section className="bg-muted/20 py-20 sm:py-28 border-y border-border">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 space-y-20 sm:space-y-28">
        <FeatureImageRow
          title={t("landingPage.imageRows.recommendations.title")}
          description={t("landingPage.imageRows.recommendations.description")}
          cta={{
            label: t("landingPage.imageRows.recommendations.cta"),
            path: "/recommendations",
          }}
          imageContent={
            <img
              src="/screenshot-recommendations.png"
              alt={t("landingPage.imageRows.recommendations.imageAlt")}
              className="w-full h-full object-cover object-top"
            />
          }
        />
        <FeatureImageRow
          reverse
          title={t("landingPage.imageRows.library.title")}
          description={t("landingPage.imageRows.library.description")}
          cta={{
            label: t("landingPage.imageRows.library.cta"),
            path: "/library",
          }}
          imageContent={
            <img
              src="/screenshot-library.png"
              alt={t("landingPage.imageRows.library.imageAlt")}
              className="w-full h-full object-cover object-top"
            />
          }
        />
      </div>
    </section>
  );
};

/* ─────────────────────────────────────────────
   Final CTA
───────────────────────────────────────────── */
const CtaSection: React.FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  return (
    <section className="bg-primary text-primary-foreground py-20 sm:py-28">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 text-center">
        <h2 className="text-3xl sm:text-4xl font-bold mb-5 leading-tight">
          {t("landingPage.cta.title")}
        </h2>
        <p className="text-lg opacity-80 mb-10 leading-relaxed">
          {t("landingPage.cta.description")}
        </p>
        <div className="flex flex-wrap gap-4 justify-center">
          <Button
            size="lg"
            className="h-12 px-8 text-base bg-primary-foreground text-primary hover:bg-primary-foreground/90"
            onClick={() => navigate("/recommendations")}
          >
            {t("landingPage.cta.primaryCta")}
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
          <Button
            size="lg"
            variant="outline"
            className="h-12 px-8 text-base bg-transparent border-primary-foreground/40 text-primary-foreground hover:bg-primary-foreground/10"
            onClick={() => navigate("/library")}
          >
            <BookOpen className="mr-2 h-4 w-4" />
            {t("landingPage.cta.secondaryCta")}
          </Button>
        </div>
        <p className="mt-6 text-sm opacity-60">
          {t("landingPage.cta.signUpPrompt")}{" "}
          <Link
            to="/login"
            className="underline underline-offset-4 hover:opacity-100 transition-opacity"
          >
            {t("landingPage.cta.signUpLink")}
          </Link>{" "}
          {t("landingPage.cta.signUpSuffix")}
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

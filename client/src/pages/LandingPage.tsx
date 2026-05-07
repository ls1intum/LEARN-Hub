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
  Filter,
  Clock,
  Heart,
  LayoutList,
  LayoutGrid,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  LANDING_MOCK_ACTIVITIES,
  LANDING_MOCK_RECOMMENDATIONS,
} from "@/data/landingPageMockData";

/* ─────────────────────────────────────────────
   Browser Mockup Components — exact UI replicas
   scaled via CSS transform (scale 0.5, width 200%)
───────────────────────────────────────────── */

const MOCK_SCALE = 0.5;

const BrowserChrome: React.FC<{ url: string }> = ({ url }) => (
  <div className="flex items-center gap-2 px-3 py-[7px] bg-muted/60 border-b border-border shrink-0">
    <div className="flex gap-1.5 shrink-0">
      <span className="w-3 h-3 rounded-full bg-red-400/80 block" />
      <span className="w-3 h-3 rounded-full bg-yellow-400/80 block" />
      <span className="w-3 h-3 rounded-full bg-green-400/80 block" />
    </div>
    <div className="flex-1 bg-background/80 rounded px-2 py-[3px] text-xs text-muted-foreground border border-border/50 truncate">
      {url}
    </div>
  </div>
);

const BLOOM_ORDER_MOCK = [
  "remember",
  "understand",
  "apply",
  "analyze",
  "evaluate",
  "create",
];

type MockActivity = (typeof LANDING_MOCK_ACTIVITIES)[number];
type MockRec = (typeof LANDING_MOCK_RECOMMENDATIONS)[number];

const MockActivityCard: React.FC<{ activity: MockActivity }> = ({
  activity,
}) => (
  <div className="border border-border rounded-lg overflow-hidden bg-card flex flex-col gap-2.5 h-full">
    <img
      src={activity.imageUrl}
      alt=""
      className="w-full h-36 object-cover shrink-0"
    />
    <div className="flex flex-col gap-2.5 p-3.5 flex-1">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 flex-1">
          <h3 className="text-sm font-semibold text-foreground leading-snug line-clamp-2">
            {activity.name}
          </h3>
          <p className="text-xs text-muted-foreground truncate mt-0.5">
            {activity.source}
          </p>
        </div>
        <div className="shrink-0 h-7 w-7 flex items-center justify-center rounded-md text-muted-foreground">
          <Heart className="h-3.5 w-3.5" />
        </div>
      </div>

      <p className="text-xs text-muted-foreground line-clamp-2 leading-relaxed">
        {activity.description}
      </p>

      <div className="flex flex-wrap gap-1">
        {activity.topicLabels.slice(0, 3).map((topic) => (
          <span
            key={topic}
            className="inline-flex items-center rounded-full border border-border bg-secondary px-1.5 py-0 text-[11px] font-normal text-secondary-foreground"
          >
            {topic}
          </span>
        ))}
      </div>

      <div className="flex-1" />

      <div className="flex items-center gap-1.5">
        <div className="flex items-center gap-0.5">
          {BLOOM_ORDER_MOCK.map((_, i) => (
            <div
              key={i}
              className={`w-2 h-2 rounded-full ${i <= activity.bloomIndex ? activity.bloomColor : "bg-muted"}`}
            />
          ))}
        </div>
        <span className="text-[11px] text-muted-foreground">
          {activity.bloomLabel}
        </span>
      </div>

      <div className="flex items-center gap-2 text-xs text-muted-foreground pt-1.5 border-t border-border/50">
        <span className="flex items-center gap-0.5">
          <Users className="h-3 w-3 shrink-0" />
          {activity.ageMin}–{activity.ageMax}
        </span>
        <span className="flex items-center gap-0.5">
          <Clock className="h-3 w-3 shrink-0" />
          {activity.durationMin}–{activity.durationMax}m
        </span>
        <div className="ml-auto">
          <span className="inline-flex items-center rounded-full border border-border bg-secondary px-1.5 py-0 text-[11px] font-normal">
            {activity.formatLabel}
          </span>
        </div>
      </div>
    </div>
  </div>
);

const MockRecommendationCard: React.FC<{ rec: MockRec }> = ({ rec }) => (
  <div className="border border-border rounded-lg p-3.5 bg-card flex flex-col gap-3 h-full">
    <div className="flex items-center gap-2 flex-wrap">
      <div className="flex items-center gap-1.5">
        <div className={`w-2 h-2 rounded-full shrink-0 ${rec.scoreColor}`} />
        <span className="text-sm font-semibold tabular-nums">
          {Math.round(rec.score)}%
        </span>
      </div>
      <div className="flex items-center gap-1.5 text-xs text-muted-foreground ml-auto">
        <span className="flex items-center gap-0.5">
          <Clock className="h-3 w-3 shrink-0" />
          <span className="tabular-nums">{rec.duration}m</span>
        </span>
        <span className="flex items-center gap-0.5">
          <Users className="h-3 w-3 shrink-0" />
          {rec.ageMin}–{rec.ageMax}
        </span>
      </div>
    </div>

    <div className="flex flex-col gap-1 flex-1">
      {rec.activityNames.map((name, idx) => (
        <div
          key={idx}
          className="flex items-center gap-2 rounded px-1.5 py-1"
        >
          <div className="w-4 h-4 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center shrink-0">
            <span className="text-[9px] font-semibold text-primary tabular-nums">
              {idx + 1}
            </span>
          </div>
          <span className="text-xs text-foreground truncate">{name}</span>
        </div>
      ))}
    </div>

    <div className="flex items-center gap-1.5 pt-2 border-t border-border/50">
      <span className="inline-flex items-center rounded-full border border-border bg-secondary px-1.5 py-0 text-[11px] font-normal">
        {rec.formatLabel}
      </span>
      <div className="ml-auto flex items-center gap-1.5">
        <div className="h-7 w-7 border border-border rounded-md flex items-center justify-center">
          <Heart className="h-3.5 w-3.5 text-muted-foreground" />
        </div>
        <div className="h-7 px-2 bg-primary rounded-md flex items-center gap-1 text-primary-foreground">
          <Sparkles className="h-3 w-3" />
          <span className="text-xs">Auswählen</span>
        </div>
      </div>
    </div>
  </div>
);

/* Library page in grid/card mode — used in the hero */
const LibraryCardMockup: React.FC = () => (
  <div className="rounded-xl overflow-hidden border border-border shadow-2xl bg-background select-none">
    <BrowserChrome url="learn-hub.app/library" />
    {/* paddingBottom creates the visible height: ~90% gives enough room for the grid */}
    <div className="relative overflow-hidden" style={{ paddingBottom: "90%" }}>
      <div
        className="absolute top-0 left-0 bg-background origin-top-left"
        style={{
          transform: `scale(${MOCK_SCALE})`,
          width: `${100 / MOCK_SCALE}%`,
        }}
      >
        <div className="w-full space-y-4 py-6 px-6">
          {/* Page header */}
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Aktivitätsbibliothek
            </h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Entdecke und filtere Aktivitäten für deinen Unterricht
            </p>
          </div>

          {/* Toolbar */}
          <div className="flex items-center gap-2">
            <div className="relative flex-1 min-w-0">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
              <div className="pl-8 h-8 border border-border rounded-md flex items-center text-sm text-muted-foreground bg-background">
                Aktivitäten suchen…
              </div>
            </div>
            <div className="h-8 px-3 border border-border rounded-md flex items-center gap-1.5 bg-background text-sm shrink-0">
              <Filter className="h-3.5 w-3.5 text-muted-foreground" />
              Filter
            </div>
            <div className="h-8 px-3 text-muted-foreground text-sm shrink-0 opacity-40 flex items-center">
              Filter löschen
            </div>
          </div>

          {/* Count + ViewToggle */}
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground tabular-nums">
              6 Aktivitäten
            </p>
            <div className="flex items-center rounded-md border border-border overflow-hidden shrink-0">
              <div className="h-7 w-7 rounded-none border-r border-border flex items-center justify-center text-muted-foreground">
                <LayoutList className="h-3.5 w-3.5" />
              </div>
              <div className="h-7 w-7 bg-muted text-foreground flex items-center justify-center">
                <LayoutGrid className="h-3.5 w-3.5" />
              </div>
            </div>
          </div>

          {/* Activity grid — 3 columns matching the real LibraryPage */}
          <div className="grid grid-cols-3 gap-3">
            {LANDING_MOCK_ACTIVITIES.map((act) => (
              <MockActivityCard key={act.id} activity={act} />
            ))}
          </div>
        </div>
      </div>
    </div>
  </div>
);

/* Recommendations results page — used in feature row 1 */
const RecommendationsMockup: React.FC = () => (
  <div className="flex flex-col h-full bg-background">
    <BrowserChrome url="learn-hub.app/recommendations" />
    <div className="flex-1 relative overflow-hidden">
      <div
        className="absolute top-0 left-0 bg-background origin-top-left"
        style={{
          transform: `scale(${MOCK_SCALE})`,
          width: `${100 / MOCK_SCALE}%`,
        }}
      >
        <div className="py-6 px-6">
          <div className="space-y-8">
            {/* Page header */}
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-foreground">
                Aktivitätsempfehlungen
              </h1>
              <p className="text-sm text-muted-foreground mt-1.5">
                KI-gestützte Empfehlungen passend für deine Klasse
              </p>
            </div>

            <div className="space-y-4">
              {/* ListFilterToolbar replica */}
              <div className="flex items-center gap-2">
                <div className="relative flex-1 min-w-0">
                  <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
                  <div className="pl-8 h-8 border border-border rounded-md flex items-center text-sm text-muted-foreground bg-background">
                    Empfehlungen suchen…
                  </div>
                </div>
                <div className="h-8 px-3 border border-border rounded-md flex items-center gap-1.5 bg-background text-sm shrink-0">
                  <Filter className="h-3.5 w-3.5 text-muted-foreground" />
                  Filter
                </div>
                <div className="h-8 px-3 text-muted-foreground text-sm shrink-0 opacity-40 flex items-center">
                  Filter löschen
                </div>
              </div>

              {/* Count + ViewToggle */}
              <div className="flex items-center justify-between">
                <p className="text-xs text-muted-foreground tabular-nums">
                  4 von 4 Empfehlungen
                </p>
                <div className="flex items-center rounded-md border border-border overflow-hidden shrink-0">
                  <div className="h-7 w-7 rounded-none border-r border-border flex items-center justify-center text-muted-foreground">
                    <LayoutList className="h-3.5 w-3.5" />
                  </div>
                  <div className="h-7 w-7 bg-muted text-foreground flex items-center justify-center">
                    <LayoutGrid className="h-3.5 w-3.5" />
                  </div>
                </div>
              </div>

              {/* Recommendation cards — 2 columns matching real ResultsDisplay grid */}
              <div className="grid grid-cols-2 gap-3">
                {LANDING_MOCK_RECOMMENDATIONS.map((rec, i) => (
                  <MockRecommendationCard key={i} rec={rec} />
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
);

/* Library page with filter panel open — used in feature row 2 */
const LibraryWithFiltersMockup: React.FC = () => (
  <div className="flex flex-col h-full bg-background">
    <BrowserChrome url="learn-hub.app/library" />
    <div className="flex-1 relative overflow-hidden">
      <div
        className="absolute top-0 left-0 bg-background origin-top-left"
        style={{
          transform: `scale(${MOCK_SCALE})`,
          width: `${100 / MOCK_SCALE}%`,
        }}
      >
        <div className="w-full space-y-4 py-6 px-6">
          {/* Page header */}
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">
              Aktivitätsbibliothek
            </h1>
            <p className="text-sm text-muted-foreground mt-0.5">
              Entdecke und filtere Aktivitäten für deinen Unterricht
            </p>
          </div>

          {/* Toolbar — "2 active filters" indicator */}
          <div className="flex items-center gap-2">
            <div className="relative flex-1 min-w-0">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
              <div className="pl-8 h-8 border border-border rounded-md flex items-center text-sm text-muted-foreground bg-background">
                Aktivitäten suchen…
              </div>
            </div>
            <div className="h-8 px-3 border border-border rounded-md flex items-center gap-1.5 bg-background text-sm shrink-0">
              <Filter className="h-3.5 w-3.5 text-muted-foreground" />
              Filter ausblenden
              <span className="inline-flex items-center justify-center rounded-full bg-primary text-primary-foreground text-[10px] font-semibold w-4 h-4">
                2
              </span>
            </div>
            <div className="h-8 px-3 text-muted-foreground text-sm shrink-0 flex items-center">
              Filter löschen
            </div>
          </div>

          {/* Filter panel — open */}
          <div className="border border-border rounded-lg p-4 space-y-5 bg-muted/20">
            {/* Range sliders row */}
            <div className="grid grid-cols-2 gap-5">
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <Users className="h-3.5 w-3.5 text-muted-foreground" />
                    <span className="text-sm font-medium">Altersbereich</span>
                  </div>
                  <span className="text-sm font-semibold text-primary tabular-nums">
                    8–14
                  </span>
                </div>
                <div className="relative h-2 bg-muted rounded-full">
                  <div className="absolute left-[25%] right-[30%] top-0 bottom-0 bg-primary rounded-full" />
                  <div className="absolute left-[25%] top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border-2 border-primary bg-background shadow" />
                  <div className="absolute right-[30%] top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border-2 border-primary bg-background shadow" />
                </div>
              </div>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-1.5">
                    <Clock className="h-3.5 w-3.5 text-muted-foreground" />
                    <span className="text-sm font-medium">Dauer</span>
                  </div>
                  <span className="text-sm font-semibold text-primary tabular-nums">
                    15–45m
                  </span>
                </div>
                <div className="relative h-2 bg-muted rounded-full">
                  <div className="absolute left-[10%] right-[40%] top-0 bottom-0 bg-primary rounded-full" />
                  <div className="absolute left-[10%] top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border-2 border-primary bg-background shadow" />
                  <div className="absolute right-[40%] top-1/2 -translate-y-1/2 w-4 h-4 rounded-full border-2 border-primary bg-background shadow" />
                </div>
              </div>
            </div>

            <div className="border-t border-border" />

            {/* Characteristic filters */}
            <div className="space-y-2">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
                Aktivitätsmerkmale
              </p>
              <div className="grid grid-cols-4 gap-4">
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-muted-foreground">
                    Format
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {["Draußen", "Drinnen", "Kreativ"].map((f, i) => (
                      <span
                        key={f}
                        className={cn(
                          "inline-flex items-center rounded-full px-3 py-1 text-xs font-medium cursor-pointer border",
                          i === 0
                            ? "bg-primary text-primary-foreground border-primary"
                            : "bg-transparent border-border text-foreground",
                        )}
                      >
                        {f}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-muted-foreground">
                    Bloom-Stufe
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {["Erinnern", "Verstehen", "Anwenden", "Analysieren"].map(
                      (b, i) => (
                        <span
                          key={b}
                          className={cn(
                            "inline-flex items-center rounded-full px-3 py-1 text-xs font-medium cursor-pointer border",
                            i === 2
                              ? "bg-primary text-primary-foreground border-primary"
                              : "bg-transparent border-border text-foreground",
                          )}
                        >
                          {b}
                        </span>
                      ),
                    )}
                  </div>
                </div>
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-muted-foreground">
                    Ressourcen
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {["Keine", "Material", "Digital"].map((r) => (
                      <span
                        key={r}
                        className="inline-flex items-center rounded-full px-3 py-1 text-xs font-medium cursor-pointer border border-border text-foreground"
                      >
                        {r}
                      </span>
                    ))}
                  </div>
                </div>
                <div className="space-y-1.5">
                  <span className="text-xs font-medium text-muted-foreground">
                    Themen
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {["Bewegung", "Vertrauen", "Kreativität"].map((t) => (
                      <span
                        key={t}
                        className="inline-flex items-center rounded-full px-3 py-1 text-xs font-medium cursor-pointer border border-border text-foreground"
                      >
                        {t}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Count + ViewToggle */}
          <div className="flex items-center justify-between">
            <p className="text-xs text-muted-foreground tabular-nums">
              4 Aktivitäten
            </p>
            <div className="flex items-center rounded-md border border-border overflow-hidden shrink-0">
              <div className="h-7 w-7 rounded-none border-r border-border flex items-center justify-center text-muted-foreground">
                <LayoutList className="h-3.5 w-3.5" />
              </div>
              <div className="h-7 w-7 bg-muted text-foreground flex items-center justify-center">
                <LayoutGrid className="h-3.5 w-3.5" />
              </div>
            </div>
          </div>

          {/* Filtered activity grid */}
          <div className="grid grid-cols-3 gap-3">
            {LANDING_MOCK_ACTIVITIES.slice(0, 3).map((act) => (
              <MockActivityCard key={act.id} activity={act} />
            ))}
          </div>
        </div>
      </div>
    </div>
  </div>
);

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
              <LibraryCardMockup />
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
          imageContent={<RecommendationsMockup />}
        />
        <FeatureImageRow
          reverse
          title={t("landingPage.imageRows.library.title")}
          description={t("landingPage.imageRows.library.description")}
          cta={{
            label: t("landingPage.imageRows.library.cta"),
            path: "/library",
          }}
          imageContent={<LibraryWithFiltersMockup />}
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

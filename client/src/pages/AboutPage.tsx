import React from "react";
import { useTranslation } from "react-i18next";
import { Mail } from "lucide-react";

type Person = {
  name: string;
  image: string;
  email?: string;
};

const MAINTAINERS: Person[] = [
  {
    name: "Prof. Dr. Stephan Krusche",
    image: "/team/krusche.jpg",
    email: "krusche@tum.de",
  },
  {
    name: "Ramona Beinstingel",
    image: "/team/beinstingel.jpg",
    email: "ramona.beinstingel@tum.de",
  },
];

const CONTRIBUTORS: Person[] = [
  {
    name: "Jonathan Ostertag",
    image: "/team/ostertag.jpg",
  },
];

const PersonCard: React.FC<{ person: Person }> = ({ person }) => (
  <div className="flex items-center gap-4 rounded-xl border border-border/70 bg-card/60 p-4">
    <img
      src={person.image}
      alt={person.name}
      loading="lazy"
      className="h-16 w-16 shrink-0 rounded-full border border-border/70 object-cover"
    />
    <div className="min-w-0 space-y-1">
      <p className="font-semibold text-foreground">{person.name}</p>
      {person.email && (
        <a
          href={`mailto:${person.email}`}
          className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground transition-colors"
        >
          <Mail className="h-3.5 w-3.5" />
          {person.email}
        </a>
      )}
    </div>
  </div>
);

export const AboutPage: React.FC = () => {
  const { t } = useTranslation();
  return (
    <div className="py-6 max-w-3xl">
      <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-6">
        {t("about.title")}
      </h1>

      <div className="space-y-4 text-sm leading-relaxed text-foreground">
        <p>{t("about.intro")}</p>
        <p>{t("about.mission")}</p>
      </div>

      <section className="mt-10">
        <h2 className="text-lg font-semibold tracking-tight text-foreground mb-4">
          {t("about.maintainers")}
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {MAINTAINERS.map((person) => (
            <PersonCard key={person.name} person={person} />
          ))}
        </div>
      </section>

      <section className="mt-10">
        <h2 className="text-lg font-semibold tracking-tight text-foreground mb-4">
          {t("about.contributors")}
        </h2>
        <div className="grid gap-4 sm:grid-cols-2">
          {CONTRIBUTORS.map((person) => (
            <PersonCard key={person.name} person={person} />
          ))}
        </div>
      </section>
    </div>
  );
};

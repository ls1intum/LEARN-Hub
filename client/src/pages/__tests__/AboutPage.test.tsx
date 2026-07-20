import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { AboutPage } from "../AboutPage";

afterEach(cleanup);

describe("AboutPage", () => {
  it("renders the About heading", () => {
    render(<AboutPage />);
    expect(
      screen.getByRole("heading", { name: "Über uns" }),
    ).toBeInTheDocument();
  });

  it("renders the maintainers with their emails", () => {
    render(<AboutPage />);
    expect(screen.getByText("Prof. Dr. Stephan Krusche")).toBeInTheDocument();
    expect(screen.getByText("Ramona Beinstingel")).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /krusche@tum\.de/ }),
    ).toHaveAttribute("href", "mailto:krusche@tum.de");
    expect(
      screen.getByRole("link", { name: /ramona\.beinstingel@tum\.de/ }),
    ).toHaveAttribute("href", "mailto:ramona.beinstingel@tum.de");
  });

  it("renders the contributor", () => {
    render(<AboutPage />);
    expect(screen.getByText("Jonathan Ostertag")).toBeInTheDocument();
  });
});

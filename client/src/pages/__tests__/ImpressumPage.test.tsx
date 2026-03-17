import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { ImpressumPage } from "../ImpressumPage";

afterEach(cleanup);

describe("ImpressumPage", () => {
  it("renders the Impressum heading", () => {
    render(<ImpressumPage />);
    expect(
      screen.getByRole("heading", { name: "Impressum" }),
    ).toBeInTheDocument();
  });

  it("renders TUM institution details", () => {
    render(<ImpressumPage />);
    expect(
      screen.getByText("Technische Universität München"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("TUM School of Computation, Information and Technology"),
    ).toBeInTheDocument();
    expect(screen.getByText("Department of Computer Science")).toBeInTheDocument();
    expect(screen.getByText("Prof. Dr. Stephan Krusche")).toBeInTheDocument();
  });

  it("renders address information", () => {
    render(<ImpressumPage />);
    expect(screen.getByText("Boltzmannstrasse 3")).toBeInTheDocument();
    expect(screen.getByText("D-85748 Garching b. München")).toBeInTheDocument();
  });

  it("renders regulating authority and VAT number", () => {
    render(<ImpressumPage />);
    expect(
      screen.getByText(
        /Bayerisches Staatsministerium für Wissenschaft, Forschung und Kunst/,
      ),
    ).toBeInTheDocument();
    expect(screen.getByText(/DE 811193231/)).toBeInTheDocument();
  });
});

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { HttpError } from "../../api/http";
import { alertsApi } from "../../services/alerts";
import { point } from "../../test/fixtures";
import { ThermalRulePanel } from "./ThermalRulePanel";

describe("rango térmico del punto", () => {
  it("una respuesta lenta no borra los límites que el usuario ya escribió", async () => {
    let reject!: (reason: unknown) => void;
    vi.spyOn(alertsApi, "getRule").mockReturnValue(
      new Promise((_, r) => (reject = r)),
    );
    render(<ThermalRulePanel point={point} />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Mínima (°C)"), "2");
    await user.type(screen.getByLabelText("Máxima (°C)"), "8");
    reject(new HttpError(404));
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(screen.getByLabelText("Mínima (°C)")).toHaveValue(2);
    expect(screen.getByLabelText("Máxima (°C)")).toHaveValue(8);
  });

  it("rechaza límites fuera del rango operativo sin llamar a la API", async () => {
    vi.spyOn(alertsApi, "getRule").mockRejectedValue(new HttpError(404));
    const configure = vi.spyOn(alertsApi, "configureRule");
    render(<ThermalRulePanel point={point} />);
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Mínima (°C)"), "-60");
    await user.type(screen.getByLabelText("Máxima (°C)"), "8");
    await user.click(screen.getByRole("button", { name: "Guardar rango" }));
    expect(configure).not.toHaveBeenCalled();
    expect(
      (screen.getByLabelText("Mínima (°C)") as HTMLInputElement).validity
        .rangeUnderflow,
    ).toBe(true);
  });
});

import { describe, expect, it } from "vitest";

import { toUserSessionViewState } from "./current-user";

describe("toUserSessionViewState", () => {
  it("maps an unauthenticated tracer response to the signed-out state", () => {
    expect(toUserSessionViewState({ authenticated: false })).toBe("signed-out");
  });
});

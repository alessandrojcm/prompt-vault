import { describe, expect, it } from 'vitest';

import { loginErrorMessage } from './login-screen';

describe("loginErrorMessage", () => {
  it("shows disabled account failures distinctly", () => {
    expect(
      loginErrorMessage({
        message: "Your account is disabled. Contact an administrator.",
      }),
    ).toBe("Your account is disabled. Contact an administrator.");
  });

  it("keeps invalid credential failures generic", () => {
    expect(loginErrorMessage({ message: "Invalid username or password." })).toBe(
      "Invalid username or password.",
    );
  });
});

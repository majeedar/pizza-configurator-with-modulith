// The guest order access token (agent.md §14.3) is returned exactly once, on
// the order-creation response — never retrievable again afterward — so it
// must be persisted client-side immediately to support a later status check.
// Keyed by displayNumber since a guest may place more than one order.

const STORAGE_KEY = "pizza-configurator.guestOrderTokens";

function readAll(): Record<string, string> {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? "{}");
  } catch {
    return {};
  }
}

export function saveGuestOrderToken(displayNumber: string, token: string): void {
  const all = readAll();
  all[displayNumber] = token;
  localStorage.setItem(STORAGE_KEY, JSON.stringify(all));
}

export function getGuestOrderToken(displayNumber: string): string | null {
  return readAll()[displayNumber] ?? null;
}

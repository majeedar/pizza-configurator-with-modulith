import { apiBaseUrl } from "./client";

/**
 * Agent.md §17: SSE from backend to KDS, "an optimization, not the only
 * source of truth" — events just tell the caller to refetch
 * `GET /api/v1/kitchen/orders`, they never carry state the UI trusts
 * directly. Reconnects automatically with a short backoff on any
 * disconnect, per §17's "browser reconnects automatically" requirement.
 *
 * Uses a manual fetch + ReadableStream reader rather than the browser's
 * `EventSource` API, because `EventSource` cannot send an `Authorization`
 * header and the kitchen stream is gated by the same staff bearer JWT as
 * every other `/api/v1/kitchen/**` endpoint (agent.md §14.1/§14.2).
 */
export function connectKitchenStream(token: string, onEvent: (eventName: string) => void): () => void {
  const controller = new AbortController();
  let stopped = false;

  async function run() {
    while (!stopped) {
      try {
        const response = await fetch(`${apiBaseUrl()}/api/v1/kitchen/stream`, {
          headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
          signal: controller.signal,
        });
        if (!response.ok || !response.body) {
          throw new Error(`Kitchen stream responded with ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (!stopped) {
          const { value, done } = await reader.read();
          if (done) {
            break;
          }
          buffer += decoder.decode(value, { stream: true });

          let boundary = buffer.indexOf("\n\n");
          while (boundary !== -1) {
            const rawEvent = buffer.slice(0, boundary);
            buffer = buffer.slice(boundary + 2);
            const eventName = rawEvent
              .split("\n")
              .find((line) => line.startsWith("event:"))
              ?.slice("event:".length)
              .trim();
            if (eventName) {
              onEvent(eventName);
            }
            boundary = buffer.indexOf("\n\n");
          }
        }
      } catch {
        // Aborted on unmount, or the connection dropped — either way fall
        // through to the reconnect delay below (unless we were stopped).
      }

      if (!stopped) {
        await new Promise((resolve) => setTimeout(resolve, 3000));
      }
    }
  }

  void run();

  return () => {
    stopped = true;
    controller.abort();
  };
}

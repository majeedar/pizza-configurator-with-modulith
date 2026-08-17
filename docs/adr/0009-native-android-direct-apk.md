# ADR-009: Native Android Apps Distributed as Direct APKs

## Status
Accepted

## Context
Two native Android apps (customer, kitchen) are in scope for v1 (agent.md §2/§8.5). Play Store distribution requires developer account setup, store review turnaround, and per-release review lag — friction that doesn't fit a single-team, fast-iterating v1, especially for the kitchen app, which is only ever installed on a small number of staff-owned/in-store devices, not distributed to the general public.

## Decision
Both apps distribute as directly downloadable APKs (not via Play Store) for v1, surfaced through the Admin-managed App Links feature (`admin.app_link_setting`, agent.md §7.10/§8.6): an admin sets a download URL per (platform, audience), the customer web footer and the KDS board show a QR/link generated on demand (ZXing, no stored image, so a URL change takes effect on the next scan with nothing to invalidate). `android-ci.yml` builds, lints, unit-tests, and produces the debug APK artifact for both apps on every push.

## Consequences
- No Play Store developer account, review process, or store-listing maintenance for v1 — releases are: build APK, update the app-link URL, done.
- Users (customers and kitchen staff) must enable "install from unknown sources" or accept an APK-install prompt — an accepted UX cost for v1, revisited only if/when Play Store distribution becomes worth the overhead (e.g. broader public rollout of the customer app).
- The App Links feature this decision depends on (admin-managed URL + on-demand QR) is itself now a reusable mechanism, not a one-off hack specific to Android distribution.

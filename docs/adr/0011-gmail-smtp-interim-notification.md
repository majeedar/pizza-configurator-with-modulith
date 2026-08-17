# ADR-011: Gmail SMTP as Interim Notification Provider

## Status
Accepted

## Context
Customers need email notifications for order status/ready updates (alongside push, ADR/§7.9). A dedicated transactional email provider (SendGrid, SES, Postmark) is the eventual right answer for deliverability and volume, but requires an account, domain verification (SPF/DKIM/DMARC), and a paid tier — setup cost that doesn't fit getting v1 running quickly.

## Decision
`GmailSmtpNotificationProvider` (Spring `JavaMailSender` against `smtp.gmail.com:587`) is the interim notification provider for v1, explicitly documented as interim, not a long-term choice. It's deliberately swappable: it implements the same `NotificationProvider` interface as `FcmPushNotificationProvider`, and both are stubbed automatically (no real connection attempted) when their credentials env var is blank — the same pattern used for the AI providers (ADR-008).

## Consequences
- **Gmail's sending-volume limits apply** (roughly 500 emails/day for a regular Gmail account, more for Google Workspace) — this is a real ceiling, not a theoretical one, and the project will hit it before it hits any interesting scale problem elsewhere. README documents this limitation explicitly (agent.md §31) so it isn't a surprise in production.
- `spring-boot-starter-mail`'s default health indicator was disabled (`management.health.mail.enabled: false`) — it actively opens an SMTP connection on every `/actuator/health` check, which would otherwise mark the whole app `DOWN` whenever Gmail credentials are blank (local/dev default) or Gmail is temporarily unreachable, for a dependency that per agent.md §7.9 must never be allowed to affect order state or overall app health.
- Swapping to a dedicated provider later means writing one new `NotificationProvider` implementation and changing `NOTIFICATION_PROVIDER` — no change to the `notification` module's public API or to how `orders`/`kitchen` publish events.

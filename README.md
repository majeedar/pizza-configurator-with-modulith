# Pizza Configurator

A Spring Boot Modular Monolith pizza configurator. Full architecture, domain
model, and build sequence are specified in [`data/agent.md`](data/agent.md) —
that document is the source of truth; this README covers what's actually
runnable today.

## Status

All 13 phases from `agent.md` §32 are complete (see "Production
deployment (Hetzner)" below for the one piece — the actual Hetzner VM
deployment — that's written and locally config-verified but not executed
end-to-end, since this environment has no real VM/domain). Beyond the
13-phase sequence: the **Admin Portal UI** (agent.md §8.3 — Pizzas,
Ingredients, Recipes, Sizes, Doughs, Rules, Prices, Staff, App Links,
Audit Log) has since been built into the staff web app, closing the
biggest gap noted throughout Phases 11-13 (the backend for all of it
existed; there was no screen for any of it until now); **customer-web
checkout** is wired to the real order API, closing the Phase 5 stub; and
the last two rule types (`MIN_QUANTITY`, `OPTION_ALLOWED`) now have real
evaluators, closing the only remaining `RuleType` gap.

- Spring Boot 4 application with the 13 module packages (`security`,
  `configuration`, `catalog`, `rules`, `pricing`, `basket`, `orders`,
  `kitchen`, `notification`, `admin`, `aiadapter`, `recommendation`,
  `shared`), enforced by Spring Modulith.
- PostgreSQL + Flyway (one schema per module; `catalog`, `rules`, `pricing`,
  `security`, `configuration`, `basket`, `orders`, `notification`,
  `recommendation`, and `admin` are populated so far).

- Spring Boot 4 application with the 13 module packages (`security`,
  `configuration`, `catalog`, `rules`, `pricing`, `basket`, `orders`,
  `kitchen`, `notification`, `admin`, `aiadapter`, `recommendation`,
  `shared`), enforced by Spring Modulith.
- PostgreSQL + Flyway (one schema per module; `catalog`, `rules`, `pricing`,
  `security`, `configuration`, `basket`, `orders`, `notification`,
  `recommendation`, and `admin` are populated so far).
- **Catalog / Rules / Pricing modules**: as before (see git history / earlier
  phases) — catalog browsing, deterministic rule validation, deterministic
  pricing, all with admin CRUD.
- **Security module (customer slice)**: `POST /api/v1/customers/register`
  and `/login`, BCrypt password hashing, self-issued JWT (agent.md §14.1).
  Guest checkout still works with no account at all — the token is optional
  everywhere it's accepted.
- **Configuration module**: the application-flow coordinator (agent.md
  §7.5) — `ConfigurationSession` lifecycle (`POST/PUT /api/v1/configurations`,
  `.../validate`, `.../price`, `GET .../{id}`), combining Catalog, Rules, and
  Pricing. A non-blank comment now goes through the AI Adapter (below); a
  successfully-resolved comment is merged with the structured UI selections
  and persisted onto the session before being sent through the Rule Module
  exactly like the no-comment path — the AI result is never trusted merely
  because it parsed. Any AI ambiguity/failure (per agent.md §13.3: timeout,
  error, malformed response, unknown ingredient, or unresolved language) is
  honestly parked as `PENDING_REVIEW` rather than guessed at.
- **Basket module**: `POST /api/v1/baskets`, add/remove items, refuses to
  add a configuration that isn't validated and priced yet. Basket items are
  full immutable snapshots (agent.md §7.6) — not just price, but pizza,
  size, dough, modifications, rule/price version — because the source
  `ConfigurationSession` can still be edited after being added to the
  basket, so `orders` must never re-read it at checkout.
- **Orders module** (agent.md §7.7): `POST /api/v1/orders` — creates the
  Order only from an already-priced, already-snapshotted basket (never
  re-validates or re-prices); enforces idempotency via a required
  `Idempotency-Key` header (repeat of the same key + same body replays the
  original Order, a repeat with a different body is a `409`); generates a
  human-readable display number (`P-00001`, ...) off a Postgres sequence, a
  short pickup code, and — for guest checkouts only — an opaque access
  token (returned once, raw, at creation time; only its SHA-256 hash is
  ever persisted). `GET /api/v1/orders/{displayNumber}/status` is
  access-guarded per agent.md §14.3: an authenticated customer must own the
  order, a guest must present the matching `?token=`, otherwise `403`
  (unknown display numbers are `404`). Publishes `OrderPlaced`,
  `OrderApproved`, `OrderProcessingStarted`, `OrderReady`, `OrderCompleted`;
  exposes published `OrderQuery` (incl. `findActiveOrders()`) and
  `OrderTransitions` APIs, now consumed by the Kitchen module below.
- **Staff auth** (agent.md §14.1): `POST /api/v1/staff/login` — shared,
  unauthenticated login for both `ROLE_KITCHEN` and `ROLE_ADMIN`, backed by
  a new `security.employee` table (BCrypt password hashing, self-issued
  JWT with a `role` claim). A real Spring Security filter chain now enforces
  RBAC — deny-by-default — on `/api/v1/kitchen/**`
  (`hasAnyRole("KITCHEN","ADMIN")`) and `/api/v1/admin/**`
  (`hasRole("ADMIN")`); every other endpoint (customer/guest, catalog,
  staff login itself) is untouched by this filter chain and keeps using the
  manual optional-JWT resolution from earlier phases, since
  `PUBLIC_CUSTOMER` isn't a real authentication concept (§14.2). This also
  closes the "admin endpoints are unauthenticated" gap noted in earlier
  phases. `POST /api/v1/admin/users`, `GET /api/v1/admin/users`, and
  `PUT /api/v1/admin/users/{employeeId}/enabled` (new, Phase 11) round out
  staff-account management — creating kitchen/admin logins and
  enabling/disabling them — under the same `hasRole("ADMIN")` rule; a
  disabled employee's existing JWT still validates (it's a stateless
  self-issued token) but a fresh `POST /api/v1/staff/login` correctly
  rejects them.
- **Kitchen module** (agent.md §7.8): `GET /api/v1/kitchen/orders` (the
  production board — everything not yet in a terminal state) and
  `POST /api/v1/kitchen/orders/{orderId}/approve|start|ready|complete`,
  which drive the `orders` module's state machine through a published
  `OrderTransitions` API (an out-of-order command, e.g. `ready` before
  `start`, is a `409 ORDER_STATE_CONFLICT`). `GET /api/v1/kitchen/stream`
  is a Server-Sent Events endpoint (`order.created`/`order.updated`/
  `order.ready`) — listens `AFTER_COMMIT` on the corresponding `orders`
  domain events, so the KDS is only ever told about a change once it's
  durably persisted. Per agent.md §17, SSE is "an optimization, not the
  only source of truth" — clients refetch the board on every event rather
  than trusting the event payload directly. The review-queue endpoints
  from §9.2 (`/kitchen/reviews/**`) are owned by the `recommendation`
  module below — its controller registers routes directly under
  `/api/v1/kitchen/**`, so they're covered by the same
  `hasAnyRole("KITCHEN","ADMIN")` filter-chain rule with no change needed
  to `kitchen` itself.
- **Staff web app** (`frontend/apps/staff`, new): React 19 + TypeScript +
  Vite + MUI, same stack as the customer app but a separate deployable on
  its own port (agent.md §8.4). Shared login screen, then a touch-first
  production board with one column per state
  (Confirmed/Approved/In processing/Ready) and a big action button per
  card. Live-updates via the SSE stream — using a hand-rolled fetch +
  `ReadableStream` reader rather than the browser's `EventSource` API,
  because `EventSource` cannot send an `Authorization` header and the
  stream is gated by the same staff bearer JWT as everything else under
  `/api/v1/kitchen/**`. `ROLE_ADMIN` staff land on the Admin Portal
  (below) after login, but can switch to this production board at any
  time via a nav button in the header — and back again — since admins
  "can also reach the KDS views" (§8.3).
- **Admin Portal** (`frontend/apps/staff/src/pages/admin/`, new — closes
  the biggest gap left open through Phases 11-13): a full route tree
  under `/admin/*`, gated by a new `RequireRole` component (`ROLE_ADMIN`
  only), covering all 9 screens from agent.md §8.3 — Pizzas, Ingredients,
  Sizes, Doughs, Rules, Prices, Staff, App Links, Audit Log — plus a
  10th, Recipes, reached from a pizza row's "Recipe" action rather than
  as its own top-level tab (recipes belong *to* a pizza in the domain
  model, so nesting them there reads more naturally than a flat global
  recipe list would). Two shared components do the heavy lifting across
  the ~7 near-identical CRUD screens — `AdminDataTable` (table chrome
  only, no field-level knowledge) and `AdminFormDialog` (submit/error/
  loading state, fields supplied by each page) — so each screen's own
  code is just its field list and API calls, not a from-scratch table
  and dialog every time. The Rules screen's parameter editor is
  structured per `RuleType` (mirroring each `RuleEvaluator`'s nested
  `Params` record) for all 9 rule types — the raw-JSON textarea fallback
  is kept only as a safety net for a future `RuleType` added without a
  matching structured spec, not because any current type still needs it
  (see the next bullet).
  - **Two real bugs found and fixed via testing this, not assumed away**:
    (1) `GET /api/v1/admin/pizzas/{pizzaId}/recipe` threw a
    `LazyInitializationException` — with `spring.jpa.open-in-view: false`
    (already the case since early phases), the Hibernate session closes
    when the transactional service method returns, so the controller's
    DTO mapping (which reads `ingredient.getCode()`) was touching an
    un-fetched lazy proxy after the session was already gone. This is a
    latent bug that predates this phase — it just had no caller until the
    Admin Portal's Recipe screen became the first thing to ever call that
    endpoint. Fixed with a `JOIN FETCH` query in
    `PizzaIngredientRepository` (both the list method and a new
    `findWithIngredientById` used by `updateRecipeLine`, which had the
    identical problem). (2) Picking an ingredient already on a pizza's
    recipe crashed with a raw 500 (an unhandled unique-constraint
    violation) instead of a clean conflict response — fixed at both
    layers: `CatalogExceptionHandler` now maps
    `DataIntegrityViolationException` to a 409 with a real message, *and*
    the Recipe screen's ingredient dropdown filters out ingredients
    already on the recipe so the conflict can't be reached from the UI at
    all. Both fixes are covered by new backend regression tests
    (`CatalogApiTests`) and a permanent Playwright E2E test
    (`e2e/tests/admin-portal.spec.ts`) that adds, edits, and removes a
    real recipe line against the live backend.
- **`MIN_QUANTITY`/`OPTION_ALLOWED` rule evaluators** (`rules/domain/evaluator/`,
  new — closes the last `RuleType` gap): `MinQuantityEvaluator` is
  `MaxQuantityEvaluator`'s inverse — a floor, not a ceiling, and only
  fires once an extra is actually selected (quantity > 0), since an extra
  nobody chose isn't a violation of its own minimum. `OptionAllowedEvaluator`
  is a flat per-ingredient gate (mirrors `SizeCompatibilityEvaluator`/
  `DoughCompatibilityEvaluator`'s `(code, allowed)` shape) for the one
  axis those two, `INGREDIENT_COMPATIBILITY`/`EXCLUDES`, and
  `REMOVAL_ALLOWED` didn't already cover — neither rule type's exact
  semantics were specified anywhere beyond the bare enum name, so both
  are a considered, documented inference, not a guess passed off as
  fact. 10 new unit tests (`rules/domain/evaluator/`) plus a new
  integration test cover them.
  - **A third real bug, found the same way**: the E2E test for the new
    structured rule editor created an `OPTION_ALLOWED` rule without
    touching its "Allowed" toggle — the form left `parameters.allowed`
    entirely absent rather than defaulting it, which 500'd
    (`MismatchedInputException`, `boolean` can't deserialize from a
    missing key) the moment that rule was evaluated. Because it was
    `GLOBAL`-scope and rules apply on *every* subsequent validation, this
    silently broke checkout for every pizza, not just the one being
    tested — the exact "one bad admin edit takes down the whole app"
    failure mode. Fixed at both layers: the Rules screen now seeds an
    explicit default for every parameter field the moment a rule type is
    chosen (never leaves a boolean unset), and — the more important
    fix — `RuleValidationService` now catches a per-rule evaluation
    failure and skips just that rule (logged) instead of letting one
    misconfigured rule fail validation for every pizza on the menu.
    Covered by `RuleValidationServiceIntegrationTest.aMisconfiguredRuleIsSkippedRatherThanFailingValidationForEveryPizza`.
- **Notification module** (agent.md §7.9): fans every order lifecycle event
  (`OrderPlaced`/`OrderApproved`/`OrderProcessingStarted`/`OrderReady`/
  `OrderCompleted`) out to every channel with data available for that
  order — email when the order has a registered customer (guest orders
  have no captured email address, so they only ever get push), push when
  `Order.fcmDeviceToken` is set — one `NotificationRecord` row persisted
  per channel attempted, independently. `GmailSmtpNotificationProvider`
  (Spring `JavaMailSender`, `smtp.gmail.com:587`) and
  `FcmPushNotificationProvider` (Firebase Admin SDK) both listen
  `AFTER_COMMIT`, each in its own fresh `REQUIRES_NEW` transaction — not
  an arbitrary choice: an `AFTER_COMMIT` listener runs after the
  triggering transaction's synchronization state has already started
  tearing down, so letting the notification save participate in whatever
  happens to still be lingering on the thread is unreliable (Hibernate
  hands back a generated id as if it succeeded, but the row is never
  actually durably committed) — caught by writing a real end-to-end test
  against Postgres rather than trusting a green unit test alone. Neither
  provider ever throws; a failed send is recorded as `FAILED` and never
  affects the other channel or the Order itself. Agent.md §27's "stub
  notification providers by default" local behavior is implemented by
  each provider simply never attempting a real connection when its
  credentials env var is blank (`GMAIL_SMTP_USERNAME`/
  `FIREBASE_PROJECT_ID`+`FIREBASE_SERVICE_ACCOUNT_JSON`), recording a
  synthetic `SENT` outcome instead — not a separate profile-conditional
  bean. `spring-boot-starter-mail`'s default health indicator was disabled
  (`management.health.mail.enabled: false`): it actively opens an SMTP
  connection to Gmail on every `/actuator/health` check, which would
  otherwise mark the whole app `DOWN` in local/dev by default — exactly
  the connection attempt the stub design exists to avoid.
- **AI Adapter module** (agent.md §7.4, ADR-008, new): `CommentInterpreter`
  — provider-agnostic, Deepseek primary / OpenAI fallback, both behind an
  in-process circuit breaker (3 consecutive failures opens it for 30s, then
  a half-open trial request) and a per-provider timeout. On timeout, HTTP
  error (incl. `429`), or malformed/schema-violating JSON, fails over to
  the next provider; if both fail, returns a result with non-empty
  `unresolvedText` (the Configuration module's cue for `PENDING_REVIEW`).
  Like the notification providers, `DeepseekClient`/`OpenAiClient` treat a
  blank API key as "not configured" and never attempt the HTTP call — the
  agent.md §27 local-default "stub AI by default" behavior, so any
  commented order locally goes straight to `PENDING_REVIEW` unless real
  keys are set. Requests an `{removeIngredients, extras, requestedSize,
  requestedDough, unresolvedText}` JSON object (agent.md §13.2) with a
  prompt that explicitly lists only the pizza's actual allowed codes and
  frames the customer's comment as data to interpret, never as
  instructions (prompt-injection resistance); the response is then
  normalized against those same allowed codes — anything that doesn't
  resolve (unknown ingredient, size/dough not offered, or text the model
  itself couldn't map) is moved into `unresolvedText` rather than passed
  through. Never decides validity, never prices, never touches an Order.
- **Recommendation module** (agent.md §7.11, new): owns the kitchen-side
  triage of a `ReviewRequest` and the customer's response to a kitchen
  recommendation. When the Configuration module can't resolve a request
  automatically — Rule Module `INVALID` with no deterministic suggestion,
  or an AI-unresolved comment (agent.md §4.3 steps 1-3) — it publishes
  `ConfigurationReviewRequested`; this module listens (`AFTER_COMMIT`, its
  own `REQUIRES_NEW` transaction — the exact Phase 8 lesson applied
  proactively this time) and opens a `ReviewRequest` (`OPEN`) on the
  kitchen queue (`GET /api/v1/kitchen/reviews`). Kitchen then **Accepts**
  (revalidates/reprices the original as-is), **Recommends** (proposes a
  full alternative configuration — stored as JSON, not applied yet — and
  notifies the customer), or **Rejects** (terminal). A
  `RECOMMENDED_BY_KITCHEN` request additionally waits on the customer:
  **Accept** applies the proposed configuration, then revalidates/reprices;
  **Reject** is terminal. Every path funnels through the *same*
  `ConfigurationReviewIntegration.approveOriginal`/`approveWithPatch`/
  `reject` published by `configuration` — Recommendation never revalidates
  or reprices itself, and Configuration never needs to know `ReviewRequest`
  exists (avoiding a dependency cycle, since Recommendation already depends
  on Configuration the other way). Revalidating and repricing happen
  together as part of Accept/Recommend-then-Accept (agent.md §4.3 steps
  6-7) — the customer only needs to see the final price and confirm, no
  extra `/price` call. Regardless of path, **no Order is ever created by
  this module** — checkout still requires the customer to add the
  (now-`REVIEW_APPROVED`) configuration to a basket and check out normally.
- **Admin module** (agent.md §7.10/§14.4, new): owns two things scoped
  tightly to agent.md §14.4's exact wording, "audit of admin rule/price/
  app-link changes" — deliberately *not* catalog, staff, recommendation,
  or order-status changes, which appear in a broader "at minimum" list
  elsewhere but not in §14.4 itself:
  - **App links** — `admin.app_link_setting` holds one active Android APK
    URL per (platform, audience) pair (today just `ANDROID`×`CUSTOMER`/
    `KITCHEN`). `PUT /api/v1/admin/app-links/android/{audience}`
    (`ROLE_ADMIN`) updates it; `GET /api/v1/app-links/android/{audience}`
    and `GET /api/v1/app-links/android/{audience}/qr.png` are
    unauthenticated public reads (the customer web footer and the native
    app's own update-check both need them pre-login); `GET
    /api/v1/kitchen/app-links/android/customer` lets kitchen staff pull up
    the customer app's QR to show someone in person. The QR PNG is
    generated on demand from the stored URL via ZXing — never stored as an
    image — so a URL change takes effect on the very next scan with
    nothing to invalidate.
  - **Audit log** — `admin.audit_event` (`GET /api/v1/admin/audit`,
    `ROLE_ADMIN`) records who/when/before/after for every rule create/
    update (`rules.application.RuleAdminService`), price create/update
    (`pricing.application.PriceAdminService`), and app-link update, via a
    published `AuditLog.record(AuditEntry)` API — those three services
    depend only on that interface, never on `admin`'s persistence
    directly. `shared.CurrentStaffActor` resolves "who" from the
    authenticated `StaffPrincipal` already on the request thread, so
    audited services didn't need an actor parameter threaded through
    every call site.
- **Customer web app** (`frontend/apps/customer`): the footer now renders
  a "Get the app" banner (new, Phase 11) — fetches
  `GET /api/v1/app-links/android/customer` on mount and shows the QR PNG
  plus a direct download link, or a quiet "not available right now"
  fallback on any fetch failure (never crashes the page). **Checkout is
  now real** (new, closes the Phase 5 stub): the basket page's "Confirm &
  Checkout" button posts to `POST /api/v1/orders` with a per-attempt
  `Idempotency-Key` (generated once per basket, reused across retries of
  that attempt, never regenerated on re-render) and an optional "Notes
  for the kitchen" field, then navigates to a new `/order/:displayNumber`
  page that polls `GET /api/v1/orders/{displayNumber}/status` every 5s —
  the same pull-based mechanism the native Android app uses, since the
  web app has no push channel. A guest's one-time-issued order access
  token is persisted to `localStorage`
  (`pizza-configurator.guestOrderTokens`, keyed by display number) so the
  status page still works if they revisit the bare URL later without the
  `?token=` query param it's first shown with. After a successful
  checkout, `BasketContext` discards the now-checked-out basket id and
  starts a fresh one (`startNewBasket()`) — reusing the old id would 409
  on the next add-item, since the backend marks a checked-out basket
  closed. Verified against the live stack: a guest configures, checks
  out, and lands on a status page showing the right display number/pickup
  code; a kitchen-side approval via the real API updates that same page
  within one poll cycle with no reload; and revisiting the order URL
  without the token query param still works via the localStorage
  fallback.
- **Native Android apps** (agent.md §8.5, new — `mobile/customer-android`,
  `mobile/kitchen-android`, Kotlin + Jetpack Compose): REST/SSE clients of
  the *same* backend as the web apps, ported endpoint-for-endpoint from
  the web apps' `src/api/*.ts` (customer) and the backend controllers
  directly (kitchen review queue, order creation — endpoints the web apps
  don't call yet; see below). Each app is an independent Gradle project —
  no shared module between them, mirroring `frontend/apps/customer` and
  `frontend/apps/staff` not sharing code either.
  - **Customer app**: catalog browse → configure (base ingredients,
    extras, size, dough, comment) → validate/price → basket → checkout →
    order status polling, plus a distinct kitchen-recommendation
    accept/reject screen (agent.md §8.1 step 9 — never silently folded
    into checkout). Real order creation, same as the web app now has: a
    per-checkout-attempt `Idempotency-Key`, FCM permission request +
    best-effort token fetch
    (Android 13+, `POST_NOTIFICATIONS` — denial or fetch failure degrades
    to `fcmDeviceToken: null`, never blocks the order), and a
    `FirebaseMessagingService` that renders incoming order-ready/status
    pushes. `google-services.json` ships as a structurally-valid
    placeholder (fake project, real schema) so the build succeeds without
    live Firebase credentials — same "never commit real secrets, stub
    when absent" principle as the backend's `FIREBASE_SERVICE_ACCOUNT_JSON`
    — swap in a real one from the Firebase console to exercise actual
    pushes. Money fields decode via a custom `BigDecimalSerializer` that
    reads the raw JSON number literal rather than routing through
    `Double`, avoiding float rounding on prices.
  - **Kitchen app**: staff login → production board (4-stage
    approve/start/ready/complete, same state machine as the staff web
    app's KDS) → review queue (accept / recommend-with-a-patch-editor /
    reject). Live board updates via a hand-rolled OkHttp SSE client
    (`KitchenStream`) ported 1:1 from the staff web app's
    `kitchenStream.ts` — `EventSource` can't send the required
    `Authorization` header, so both clients read raw SSE frames off a
    streaming response body instead, and both treat every event as "go
    refetch the order list" without trusting the payload (agent.md §17).
    The board's corner also shows the customer app's QR (agent.md §8.2),
    loaded straight from the public `qr.png` endpoint. No push
    notifications here per agent.md §8.5 — kitchen relies entirely on SSE.
  - **Deliberate scope decisions**: the native customer app's core screen
    omits the self-referential "Get the app" banner that agent.md §8.1
    lists for "web and native Android app" alike — advertising the app's
    own download link from inside the already-installed app has no
    purpose, unlike the identical banner in the web app. The kitchen
    app's "Recommend" patch editor is plain size/dough/removed-ingredient
    text fields rather than the full catalog-aware picker the configure
    screen has, since building a second catalog-browsing UI for an
    admin-facing edge case wasn't proportionate to this phase's scope.
  - **Verification caveat (read this before assuming "it runs")**: this
    development environment has no Android emulator with working
    hardware-accelerated virtualization, so the agent.md §32 Phase 12
    Definition of Done ("both APKs install and can complete their
    respective flows end-to-end") was **not** verified by actually
    installing and clicking through either app. What *was* verified,
    against a `mingc/android-build-box` Docker image (Android SDK 35,
    JDK 21, no local Android SDK install needed): both apps compile
    (`compileDebugKotlin`), unit tests pass (`testDebugUnitTest` — a
    `BigDecimalSerializer` round-trip test and a kitchen board
    state-machine test), lint is clean (`lintDebug`, zero warnings after
    fixes), and both produce real installable debug APKs
    (`assembleDebug` — `app/build/outputs/apk/debug/app-debug.apk`,
    ~19MB/~18MB). A genuine device/emulator click-through (like the
    Playwright-driven verification done for the web apps in earlier
    phases) is a documented gap, not a claimed pass — see "Not yet
    built" below.
- **Production hardening** (agent.md §27/§28, new):
  - **Security review**: found and fixed a real finding first — the
    committed `.env.example` had a live Gmail App Password and a live
    Deepseek API key in it instead of placeholders (both have since been
    replaced with blanks; the credentials themselves need rotating at the
    source, which only a human with account access can do). Beyond that:
    explicit Spring Security headers (HSTS with `includeSubDomains`,
    `X-Frame-Options: DENY`, `nosniff`, `Referrer-Policy`), request-size
    limits (`server.tomcat.max-http-form-post-size`,
    `server.max-http-request-header-size`), and `server.forward-headers-strategy: native`
    so HSTS/redirect logic correctly sees "this was HTTPS" once nginx is
    the one terminating TLS in production.
  - **Rate limiting**: at the nginx edge (`infrastructure/nginx/nginx.conf`),
    not in the application — a general zone (20 req/s) for catalog/config
    reads and a tight zone (5 req/min) for login/checkout endpoints
    (`/customers/login`, `/staff/login`, `/orders`), since brute-force and
    duplicate-checkout-attempt mitigation belongs at the edge agent.md
    §14.4 explicitly allows either layer for this.
  - **Observability**: `/actuator/prometheus` (Micrometer + histogram
    buckets on `http.server.requests`, so Prometheus can compute the
    agent.md §28 "<500ms p95" target with `histogram_quantile`, not just a
    mean), an optional `observability` Compose profile
    (`docker compose --profile observability up`) wiring Prometheus +
    Grafana with an auto-provisioned datasource and a real starter
    dashboard (`infrastructure/grafana/dashboards/backend-overview.json`)
    — verified locally: Prometheus actually scraping the backend, Grafana
    actually showing both the datasource and the dashboard. Structured
    JSON stdout logs (`logging.structured.format.console: ecs`, Spring
    Boot's built-in support, no extra dependency) on the new `production`
    profile only. OpenTelemetry/OTLP tracing is wired but inert by default
    (`management.tracing.sampling.probability: 0`, same "present but
    stubbed until configured" pattern as the AI/notification providers) —
    set `OTEL_EXPORTER_OTLP_ENDPOINT` and a nonzero sampling probability
    to actually export spans somewhere.
  - **Container scanning**: Trivy, run locally against every built image
    during this phase, found and fixed real HIGH-severity CVEs — stale
    transitive Netty/httpcore5/postgresql-driver versions pulled in via
    the Spring Boot 4.0.7 parent BOM (fixed with explicit
    `dependencyManagement` version overrides in `backend/pom.xml`), and 8
    unfixed Go-stdlib CVEs baked into Canonical's Pebble binary bundled in
    the Ubuntu-based `eclipse-temurin:21-jre` tag (fixed by switching the
    backend Dockerfile to `eclipse-temurin:21-jre-alpine`, which doesn't
    carry Pebble at all, plus `apk upgrade` for the base Alpine packages
    themselves). All four production images (backend, customer-web,
    staff-web — native Android APKs aren't containers) scan clean at
    HIGH/CRITICAL as of this phase; `container-build.yml` (new) re-runs
    this scan on every push and fails the build on a new HIGH/CRITICAL
    finding, uploading results to GitHub code scanning.
  - **Postgres backup/restore**: `infrastructure/scripts/backup-postgres.sh`
    (`pg_dump` from the running container, gzip, prune old local backups,
    upload off-VM via `rclone` if `RCLONE_REMOTE` is set — e.g. a Hetzner
    Storage Box) and `infrastructure/scripts/restore-postgres.sh`
    (confirmation-gated drop/recreate/restore). Both were **actually run**
    against the live local stack during this phase, not just written: a
    real backup was taken, the database was dropped and restored from it,
    the backend was restarted, and both the catalog and staff login were
    confirmed working afterward. `infrastructure/systemd/pizza-backup.{service,timer}`
    schedules this daily on the Hetzner VM.
  - **E2E suite** (`e2e/`, new): a real Playwright test project — not a
    manual verification script this time, a committed, repeatable suite —
    covering the customer configure→basket flow, staff login→production
    board, and the admin app-link→customer-banner round trip (formalizing
    the manual Phase 11 verification into an automated test, with
    before/after API calls that restore the original app-link value so
    the test doesn't leave shared demo data mutated). All 3 pass against
    the live stack. `integration.yml` (new) runs this suite in CI: start
    Compose, wait for backend health, run Playwright, collect Compose
    logs and the Playwright HTML report on failure, tear down.
  - **Load test** (`infrastructure/loadtest/load-test.js`, k6, new):
    covers exactly the five flows agent.md §28 names — catalog reads,
    configuration validation, price calculation, checkout, KDS reads.
    Actually run locally (10 VUs, ~70s, 402 checkout iterations, 0 failed
    requests): p95 latency was 8ms (catalog list), 17ms (catalog options),
    61ms (price), 76ms (validate), 190ms (KDS reads) — comfortably under
    the §28 "<500ms p95" target with real numbers, not an assumption.
  - **Architecture Decision Records** (`docs/adr/`, new): all 11 ADRs
    named in agent.md §32 were specified from Phase 1 onward but never
    actually written as files until now — they exist as real markdown
    documents as of this phase, retroactively covering every major
    decision made across all 13 phases.
- **Hetzner deployment plumbing** (new, config/script-level only — see
  "Production deployment (Hetzner)" below for the verification caveat):
  `compose.prod.yaml` (standalone production compose file — deliberately
  not a `compose.yaml` overlay, since Compose merges list fields like
  `ports` across `-f` files rather than letting an overlay remove them,
  which would make "don't publish this port publicly" awkward to express),
  `infrastructure/nginx/` (templated nginx config — `${DOMAIN}` env var,
  envsubst'd by nginx's own official-image entrypoint, no custom
  entrypoint script needed; Docker-embedded-DNS-based upstream resolution
  so a restarted backend/frontend container doesn't require an nginx
  reload to be reachable again; a `certbot`-based Let's-Encrypt bootstrap
  script following the standard community dummy-cert-then-real-cert
  pattern), `.github/workflows/deploy.yml` (new — build/scan/publish to
  GHCR with an immutable Git-SHA tag, SSH to the VM, `docker compose pull && up -d`,
  smoke test; supports redeploying a previous tag via `workflow_dispatch`
  for rollback).
- Demo data (3 pizzas + recipes, sizes, doughs, 9 ingredients, 4 demo rules,
  17 demo prices, 2 demo staff accounts — `kitchen`/`kitchen123` and
  `admin`/`admin123`) seeds automatically under the `local` profile via a
  dev-only Flyway location — never in a real deployment.
- Docker Compose (Postgres + backend + customer-web + staff-web — the
  native Android apps are intentionally not part of Compose, per
  agent.md §27, since they build/run via their own Gradle toolchain, not
  containers), CI (GitHub Actions: `mvn verify` for the backend, a matrix
  build for both frontend apps, `android-ci.yml` for both native apps,
  `container-build.yml` (new) building + Trivy-scanning every production
  image on every push, and `integration.yml` (new) running the Playwright
  E2E suite against a real Compose stack).

Not yet built: a genuine device/emulator click-through of either native
Android app (build-verified only, not run-verified — see "Native Android
apps" above), and an actual executed Hetzner VM deployment
(`deploy.yml`/nginx-certbot/backup-timer are written and locally
config-verified piece-by-piece, never run end-to-end against a real
VM/domain — see "Production deployment (Hetzner)" below).

## Prerequisites

- Docker (used for both local run and build — see note below on Java).
- For local (non-Docker) backend development: JDK 21 and Maven. (This repo
  doesn't currently ship a Maven wrapper; install Maven 3.9+ separately, or
  just use Docker.)
- Node 22+ for local (non-Docker) frontend development.

## Local startup

```bash
cp .env.example .env
docker compose up --build
```

This builds the backend image (multi-stage: Maven build stage compiles and
packages the jar, skipping tests — see "Test commands" below — then a slim
JRE runtime stage runs it as a non-root user), the customer and staff web
app images (multi-stage: Node build stage runs `vite build`, then nginx
serves the static output), and starts all four services (Postgres, backend,
customer-web, staff-web).

If port 3000 and/or 3001 are already taken on your machine, set
`CUSTOMER_WEB_PORT`/`STAFF_WEB_PORT` in `.env` before starting — and update
`CORS_ALLOWED_ORIGINS` to match, since the backend only accepts
cross-origin requests from an allowlisted origin (agent.md §14.4).

Open **http://localhost:3000** and configure a pizza — it should validate,
price, and add to your basket. Open **http://localhost:3001** and log in
as `kitchen`/`kitchen123` to see the production board, or
`admin`/`admin123` to land on the Admin Portal (with a nav button to
switch over to the production board too).

Verify the backend alone:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/catalog/pizzas
```

The second call should return Margherita, Hawaii, and Napoli.

## Test commands

Backend:

```bash
cd backend
mvn clean verify
```

Runs unit tests (including the Spring Modulith boundary-verification test,
`ModularityTests`) and Testcontainers-backed integration tests against a
real PostgreSQL container (agent.md §20.3 — no H2). Requires a local Docker
daemon (Testcontainers) and JDK 21/Maven; this is why the Dockerfile's build
stage skips tests rather than running them inside `docker build` — there's
no Docker-in-Docker access there. `mvn verify` is what CI runs
(`.github/workflows/backend-ci.yml`).

Frontend (either app):

```bash
cd frontend/apps/customer   # or frontend/apps/staff
npm install
npm run build   # tsc -b && vite build — type-checks and bundles
```

No component test suite yet (Vitest from agent.md §2 is still deferred) —
noted as a gap, not silently skipped. End-to-end coverage does exist now,
see "E2E tests" below.

E2E tests (`e2e/`, Playwright, new — requires the full Compose stack
already running, see "Local startup" above):

```bash
cd e2e
npm install
npx playwright install chromium
CUSTOMER_WEB_URL=http://localhost:3000 STAFF_WEB_URL=http://localhost:3001 \
  BACKEND_URL=http://localhost:8080 npx playwright test
```

Covers the customer configure→basket flow, staff login→production board,
and an admin app-link change reflected live on the customer web banner
(restores the app-link's original value afterward). This is what
`integration.yml` runs in CI against its own ephemeral Compose stack.

Load test (`infrastructure/loadtest/load-test.js`, k6, new — also needs
the stack running; no local k6 install required):

```bash
docker run --rm -i --network <compose-project>_default \
  -e BASE_URL=http://backend:8080 \
  grafana/k6 run - < infrastructure/loadtest/load-test.js
```

(Find `<compose-project>_default` via `docker network ls` — it's your
Compose project name plus `_default`.) Covers the five flows agent.md §28
names: catalog reads, configuration validation, price calculation,
checkout, and KDS reads, with thresholds asserting the "<500ms p95"
target from that section. **This creates real orders** in whatever
database it's pointed at — fine against local/dev data, not something to
run against a production database.

Native Android apps (either app): this repo has no Gradle wrapper jar
checked in for `mobile/*` (binary file — generated, not hand-written);
generate it once with a local Android SDK/Gradle install, or reuse the
Docker approach this project was verified with, which needs no local
Android SDK at all:

```bash
cd mobile/customer-android   # or mobile/kitchen-android
gradle wrapper --gradle-version 8.9   # one-time, inside a container/environment with Gradle+internet
docker run --rm -v "$PWD:/project" -w /project mingc/android-build-box:latest \
  bash -lc './gradlew assembleDebug testDebugUnitTest lintDebug'
```

Produces `app/build/outputs/apk/debug/app-debug.apk`. See the
"Verification caveat" note under "Native Android apps" above — this
confirms the app builds and its unit tests/lint pass, not that it runs
correctly on a device. To actually install it, sideload the APK onto a
device/emulator on the same network as the backend and pass
`-PapiBaseUrl=http://<your-machine-ip>:8080` to `assembleDebug` (the
default `http://10.0.2.2:8080` only resolves from inside the Android
emulator).

## Environment variables

See [`.env.example`](.env.example) and `agent.md` §24. Currently consumed:
`DB_*`, `SPRING_PROFILES_ACTIVE`, `SERVER_PORT`, `JWT_SIGNING_SECRET`/
`JWT_ISSUER`/`JWT_AUDIENCE` (customer and staff login — local/dev falls
back to an insecure baked-in secret if unset; production must override
it), `CORS_ALLOWED_ORIGINS`, `CUSTOMER_WEB_PORT`, `STAFF_WEB_PORT`,
`GMAIL_SMTP_USERNAME`/`GMAIL_SMTP_APP_PASSWORD` (leave blank locally —
`GmailSmtpNotificationProvider` stubs the send instead of connecting),
`FIREBASE_PROJECT_ID`/`FIREBASE_SERVICE_ACCOUNT_JSON` (leave blank locally
— same stubbing behavior for `FcmPushNotificationProvider`;
`FIREBASE_SERVICE_ACCOUNT_JSON` accepts either a filesystem path or a
base64-encoded key), `DEEPSEEK_API_KEY`/`DEEPSEEK_MODEL`/
`DEEPSEEK_TIMEOUT_MS` and `OPENAI_API_KEY`/`OPENAI_MODEL`/
`OPENAI_TIMEOUT_MS` (leave the API keys blank locally — same stubbing
behavior for `DeepseekClient`/`OpenAiClient`; `*_BASE_URL` overrides are
rarely needed outside tests, which point them at WireMock).

Phase 13 additions: `OTEL_EXPORTER_OTLP_ENDPOINT`/`OTEL_TRACES_SAMPLER_PROBABILITY`
(leave the probability at its default `0` locally — no spans are ever
created, so the endpoint is never actually contacted; matches the
stub-by-default pattern above), `GRAFANA_ADMIN_PASSWORD` (only used by
the optional `observability` Compose profile, defaults to `admin`),
`RCLONE_REMOTE` (optional, used by `infrastructure/scripts/backup-postgres.sh`
to upload off-VM — leave unset for a local-only backup). Production-only,
not in `.env.example` (set directly in the Hetzner VM's `.env` or as
`compose.prod.yaml` env vars): `DOMAIN`, `PUBLIC_BASE_URL` (real HTTPS
URL). GitHub Actions *secrets* (repo/environment settings, not app env
vars — agent.md §24): `HETZNER_HOST`/`HETZNER_SSH_USER`/`HETZNER_SSH_KEY`/
`GHCR_TOKEN`, used only by `deploy.yml`.

`frontend/apps/{customer,staff}/.env.example` documents `VITE_API_BASE_URL`
— baked in at frontend **build** time (Vite convention), not read at
runtime; the Docker build passes it as a build arg from `compose.yaml`.

## Key URLs (local)

```text
Customer web:      http://localhost:3000
Staff web (KDS):   http://localhost:3001   (kitchen/kitchen123, admin/admin123)
Backend health:    http://localhost:8080/actuator/health
Backend metrics:   http://localhost:8080/actuator/prometheus
Catalog (read):    http://localhost:8080/api/v1/catalog/pizzas
Catalog (admin):   http://localhost:8080/api/v1/admin/pizzas   (requires ROLE_ADMIN)
Rules (admin):     http://localhost:8080/api/v1/admin/rules    (requires ROLE_ADMIN)
Prices (admin):    http://localhost:8080/api/v1/admin/prices   (requires ROLE_ADMIN)
Kitchen board:     http://localhost:8080/api/v1/kitchen/orders (requires ROLE_KITCHEN/ADMIN)
Kitchen reviews:   http://localhost:8080/api/v1/kitchen/reviews (requires ROLE_KITCHEN/ADMIN)
App links (admin): http://localhost:8080/api/v1/admin/app-links (requires ROLE_ADMIN)
Audit log (admin): http://localhost:8080/api/v1/admin/audit     (requires ROLE_ADMIN)
Staff users:       http://localhost:8080/api/v1/admin/users     (requires ROLE_ADMIN)
App link (public): http://localhost:8080/api/v1/app-links/android/customer
App link QR:       http://localhost:8080/api/v1/app-links/android/customer/qr.png
Prometheus (opt.): http://localhost:9090   (docker compose --profile observability up)
Grafana (opt.):    http://localhost:3002   (admin/admin by default — anonymous Viewer access is also on)
```

The Admin Portal UI (browsing/editing catalog/rules/prices/staff/
app-links/audit through the staff web app itself, not curl) is live at
`/admin` after logging in as `admin`/`admin123` — see the "Admin Portal"
bullet above.

## Demo data

Seeded automatically for the `local` profile (the Compose default) from
`backend/src/main/resources/db/dev-migration/` — a separate Flyway location
from the always-applied `db/migration` schema migrations, per `agent.md`
§6.1 ("development/demo seed data belongs in separate dev profile
migrations"). It never runs unless `local`'s extra Flyway location is
enabled (see `application-local.yml`, which also sets
`spring.flyway.out-of-order: true` — needed because the dev-only seed
migrations are numbered above 900 and new schema migrations keep landing
below that on a database that's already run them once; local-only, never
safe for a real database).

## Notifications

Gmail SMTP (email) and Firebase Cloud Messaging (push) are both
implemented (agent.md §7.9) and stub themselves automatically when the
corresponding credentials env vars are unset — see "Environment
variables" above. To exercise a real send locally, set
`GMAIL_SMTP_USERNAME`/`GMAIL_SMTP_APP_PASSWORD` (a Gmail App Password, not
your account password) and/or `FIREBASE_PROJECT_ID`/
`FIREBASE_SERVICE_ACCOUNT_JSON` in `.env` before starting Compose.

**Gmail SMTP sending-volume limitation** (ADR-011, agent.md §31): a
regular Gmail account caps outbound SMTP at roughly 500 messages/day (more
on Google Workspace, still finite). `GmailSmtpNotificationProvider` is
explicitly an interim choice for v1, not a long-term one — it's the
project's only outbound-email volume ceiling, and it will be reached
before most other parts of the system hit an interesting scale limit.
Swap to a dedicated transactional provider (SendGrid, SES, Postmark, …)
once real order volume approaches that ceiling: write one new
`NotificationProvider` implementation and change `NOTIFICATION_PROVIDER`
— no change needed to the `notification` module's public API.

## AI

Deepseek (primary) and OpenAI (fallback) are both implemented (agent.md
§7.4) and stub themselves automatically when `DEEPSEEK_API_KEY`/
`OPENAI_API_KEY` are unset — see "Environment variables" above. With both
blank (the local default), any order comment resolves to `PENDING_REVIEW`
immediately, with no outbound HTTP call attempted. To exercise a real
provider locally, set the corresponding API key (and `_MODEL` if you want
something other than the default) in `.env` before starting Compose.

## Observability

`/actuator/health` and `/actuator/prometheus` are always on (see "Key
URLs" above). Metrics, structured logs, and tracing are otherwise opt-in:

- **Metrics dashboards** (Prometheus + Grafana, agent.md §25.2's
  `observability` Compose profile): `docker compose --profile observability up`
  starts Prometheus (scraping the backend every 15s) and Grafana with an
  auto-provisioned datasource and a starter dashboard
  (`infrastructure/grafana/dashboards/backend-overview.json` — request
  rate, p95 latency per endpoint, JVM heap, uptime). Verified locally
  during Phase 13: Prometheus's own target list shows the backend scrape
  as healthy, and Grafana's API confirms both the datasource and the
  dashboard were actually provisioned, not just configured.
- **Structured logs**: JSON to stdout (`logging.structured.format.console: ecs`,
  Spring Boot's built-in support) on the `production` profile only —
  local/dev keeps human-readable console logs. `docker compose logs` (or
  whatever collects VM container logs) gets ECS-shaped JSON lines in
  production.
- **Tracing**: Micrometer Tracing + an OTLP exporter are wired in but
  inert by default (`management.tracing.sampling.probability: 0` — no
  span is ever created, so nothing is ever sent, regardless of whether
  `OTEL_EXPORTER_OTLP_ENDPOINT` points anywhere real). Point it at a real
  OTLP collector (Jaeger, Tempo, a hosted APM) and set a nonzero sampling
  probability to actually export spans — no code change needed, only env
  vars.

## Production deployment (Hetzner)

**What this is**: a single Hetzner VM, reached via a GitHub Actions
CI/CD pipeline (agent.md §27, ADR-010) — `compose.prod.yaml` (backend +
Postgres + customer-web + staff-web + nginx + certbot, images pulled from
GHCR rather than built on the VM), `infrastructure/nginx/` (TLS
termination, security headers, rate limiting, one server block per
subdomain — `app.<domain>`, `staff.<domain>`, `api.<domain>`),
`infrastructure/scripts/{backup,restore}-postgres.sh` run daily via
`infrastructure/systemd/pizza-backup.{service,timer}`, and
`.github/workflows/deploy.yml` (build/scan/publish to GHCR with an
immutable Git-SHA tag, SSH to the VM, `docker compose pull && up -d`,
smoke test — supports redeploying a previous tag via `workflow_dispatch`
for rollback, agent.md §27).

**What was actually verified, and how** (this development environment has
no real Hetzner VM, domain, or GHCR/SSH credentials — everything below
was checked by the most real means available without one):

- `infrastructure/nginx/nginx.conf` + the templated server-block config:
  syntax-validated with a real `nginx -t` inside the official nginx
  image, using a real (self-signed, for the test) TLS certificate and the
  actual `envsubst` templating mechanism the production container uses —
  caught and fixed a real bug (upstream hostnames resolved once at
  startup instead of per-request via Docker's embedded DNS, which would
  have made nginx serve stale 502s after any backend/frontend container
  restart until nginx itself was reloaded).
- `infrastructure/scripts/backup-postgres.sh` /
  `restore-postgres.sh`: run for real against the live local Compose
  Postgres — a backup was taken, the database was dropped and restored
  from it, the backend was restarted, and the catalog + staff login were
  confirmed working against the restored data afterward.
- Container images: all four (backend, customer-web, staff-web) scan
  clean with Trivy at HIGH/CRITICAL as of this phase — see "Production
  hardening" above for what was found and fixed to get there.
- `compose.prod.yaml`: reviewed for correctness (no host ports published
  for postgres/backend/customer-web/staff-web — only nginx is
  internet-facing; resource limits set; `restart: unless-stopped`) but
  never actually run, since running it requires GHCR images that only
  exist once `deploy.yml` has published something.
- `deploy.yml`/`container-build.yml`: written per agent.md §22/§26/§27,
  reviewed, but **not executed** — GitHub Actions secrets
  (`HETZNER_HOST`/`HETZNER_SSH_USER`/`HETZNER_SSH_KEY`/`GHCR_TOKEN`) don't
  exist for this repository yet, and there is no real Hetzner VM to SSH
  into or domain to request a Let's Encrypt certificate for.

**To actually deploy this to a real Hetzner VM**, roughly:

1. Provision a Hetzner VM, install Docker, point a domain's DNS at it.
2. `git clone` this repo to `/opt/pizza-configurator` on the VM, `cp
   .env.example .env` and fill in real production values (never commit
   `.env`).
3. Add the four GitHub Actions secrets in the repo's settings.
4. Run `infrastructure/nginx/init-letsencrypt.sh` once (`DOMAIN=... CERTBOT_EMAIL=...`)
   to bootstrap the TLS certificate — see the script's own comments for
   why the dummy-cert-then-real-cert two-step is necessary.
5. Enable the backup timer: `systemctl enable --now pizza-backup.timer`
   (after copying the two unit files from `infrastructure/systemd/` into
   `/etc/systemd/system/`).
6. Push to `main` — `deploy.yml` takes it from there.

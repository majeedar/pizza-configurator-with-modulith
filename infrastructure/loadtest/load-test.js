import http from "k6/http";
import { check, sleep } from "k6";

// Covers exactly the five flows agent.md §28 calls out: catalog reads,
// configuration validation, price calculation, checkout, and KDS reads.
// Run with no local install needed:
//   docker run --rm -i --network host grafana/k6 run - < infrastructure/loadtest/load-test.js
// or, against a non-local target:
//   docker run --rm -i -e BASE_URL=https://api.example.com grafana/k6 run - < infrastructure/loadtest/load-test.js
const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  scenarios: {
    steady_load: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: 10 },
        { duration: "40s", target: 10 },
        { duration: "10s", target: 0 },
      ],
    },
  },
  thresholds: {
    // agent.md §28: "validation/pricing target: approximately <500ms p95
    // excluding slow optional AI calls" — these requests never set a
    // comment, so no AI call is ever triggered, keeping this comparison fair.
    "http_req_duration{name:catalog_list}": ["p(95)<500"],
    "http_req_duration{name:catalog_options}": ["p(95)<500"],
    "http_req_duration{name:configuration_validate}": ["p(95)<500"],
    "http_req_duration{name:configuration_price}": ["p(95)<500"],
    "http_req_duration{name:kds_orders}": ["p(95)<500"],
    http_req_failed: ["rate<0.01"],
  },
};

// One staff login per VU rather than per iteration — matches how a real KDS
// client behaves (log in once, poll repeatedly), and avoids hammering the
// login endpoint itself, which isn't one of the five listed targets.
let staffToken;

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/staff/login`,
    JSON.stringify({ username: "kitchen", password: "kitchen123" }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "staff_login" } }
  );
  check(loginRes, { "staff login ok": (r) => r.status === 200 });
  return { staffToken: loginRes.json("token") };
}

export default function (data) {
  // 1. Catalog reads
  const listRes = http.get(`${BASE_URL}/api/v1/catalog/pizzas`, { tags: { name: "catalog_list" } });
  check(listRes, { "catalog list 200": (r) => r.status === 200 });
  const pizzas = listRes.json();
  const pizza = pizzas[0];

  const optionsRes = http.get(`${BASE_URL}/api/v1/catalog/pizzas/${pizza.pizzaId}/options`, {
    tags: { name: "catalog_options" },
  });
  check(optionsRes, { "catalog options 200": (r) => r.status === 200 });
  const options = optionsRes.json();

  // 2. Configuration create + 3. validate (no comment -> never touches the
  // AI adapter, matching the §28 "excluding slow optional AI calls" caveat)
  const configBody = JSON.stringify({
    pizzaId: pizza.pizzaId,
    sizeCode: options.sizes[0].code,
    doughCode: options.doughs[0].code,
    removedIngredients: [],
    extras: [],
  });
  const createRes = http.post(`${BASE_URL}/api/v1/configurations`, configBody, {
    headers: { "Content-Type": "application/json" },
    tags: { name: "configuration_create" },
  });
  check(createRes, { "configuration create 201/200": (r) => r.status < 300 });
  const configurationId = createRes.json("configurationId");

  const validateRes = http.post(
    `${BASE_URL}/api/v1/configurations/${configurationId}/validate`,
    null,
    { tags: { name: "configuration_validate" } }
  );
  check(validateRes, { "validate 200": (r) => r.status === 200 });

  // 4. Price calculation
  const priceRes = http.post(
    `${BASE_URL}/api/v1/configurations/${configurationId}/price`,
    null,
    { tags: { name: "configuration_price" } }
  );
  check(priceRes, { "price 200": (r) => r.status === 200 });

  // 5. Checkout: basket create -> add item -> order (guest, unique idempotency key per iteration)
  const basketRes = http.post(`${BASE_URL}/api/v1/baskets`, null, { tags: { name: "basket_create" } });
  check(basketRes, { "basket create 201/200": (r) => r.status < 300 });
  const basketId = basketRes.json("basketId");

  const addItemRes = http.post(
    `${BASE_URL}/api/v1/baskets/${basketId}/items`,
    JSON.stringify({ configurationId, quantity: 1 }),
    { headers: { "Content-Type": "application/json" }, tags: { name: "basket_add_item" } }
  );
  check(addItemRes, { "basket add item 200": (r) => r.status === 200 });

  const orderRes = http.post(
    `${BASE_URL}/api/v1/orders`,
    JSON.stringify({ basketId }),
    {
      headers: {
        "Content-Type": "application/json",
        "Idempotency-Key": `k6-${__VU}-${__ITER}-${Date.now()}`,
      },
      tags: { name: "checkout" },
    }
  );
  check(orderRes, { "checkout 201": (r) => r.status === 201 });

  // 6. KDS reads
  const kdsRes = http.get(`${BASE_URL}/api/v1/kitchen/orders`, {
    headers: { Authorization: `Bearer ${data.staffToken}` },
    tags: { name: "kds_orders" },
  });
  check(kdsRes, { "kds orders 200": (r) => r.status === 200 });

  sleep(1);
}

/**
 * Published interface of the admin module (agent.md §7.10, §23). Other
 * modules — {@code rules}, {@code pricing} (audit their own changes),
 * customer/kitchen web layers (app-link lookup) — may depend on types in
 * this package only.
 */
@org.springframework.modulith.NamedInterface("api")
package com.example.pizzaconfigurator.admin.api;

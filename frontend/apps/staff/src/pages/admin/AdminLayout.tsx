import { Box, Tab, Tabs } from "@mui/material";
import { Link, Outlet, useLocation } from "react-router-dom";

const SECTIONS = [
  { path: "/admin/pizzas", label: "Pizzas" },
  { path: "/admin/ingredients", label: "Ingredients" },
  { path: "/admin/sizes", label: "Sizes" },
  { path: "/admin/doughs", label: "Doughs" },
  { path: "/admin/rules", label: "Rules" },
  { path: "/admin/prices", label: "Prices" },
  { path: "/admin/staff", label: "Staff" },
  { path: "/admin/app-links", label: "App Links" },
  { path: "/admin/audit", label: "Audit Log" },
];

/** Route shell for the whole Admin Portal (agent.md §8.3) — a tab per screen, nested content via <Outlet/>. */
export default function AdminLayout() {
  const location = useLocation();
  // Recipe editing lives under /admin/pizzas/:pizzaId/recipe — still highlight "Pizzas".
  const activeSection = SECTIONS.find((section) => location.pathname.startsWith(section.path))?.path ?? false;

  return (
    <Box>
      <Tabs
        value={activeSection}
        variant="scrollable"
        scrollButtons="auto"
        sx={{ borderBottom: 1, borderColor: "divider", mb: 3 }}
      >
        {SECTIONS.map((section) => (
          <Tab key={section.path} label={section.label} value={section.path} component={Link} to={section.path} />
        ))}
      </Tabs>
      <Outlet />
    </Box>
  );
}

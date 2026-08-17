import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import RequireAuth from "./components/RequireAuth";
import RequireRole from "./components/RequireRole";
import LoginPage from "./pages/LoginPage";
import ProductionBoardPage from "./pages/ProductionBoardPage";
import ReviewQueuePage from "./pages/ReviewQueuePage";
import AdminLayout from "./pages/admin/AdminLayout";
import PizzasAdminPage from "./pages/admin/PizzasAdminPage";
import PizzaRecipePage from "./pages/admin/PizzaRecipePage";
import IngredientsAdminPage from "./pages/admin/IngredientsAdminPage";
import SizesAdminPage from "./pages/admin/SizesAdminPage";
import DoughsAdminPage from "./pages/admin/DoughsAdminPage";
import RulesAdminPage from "./pages/admin/RulesAdminPage";
import PricesAdminPage from "./pages/admin/PricesAdminPage";
import StaffAdminPage from "./pages/admin/StaffAdminPage";
import AppLinksAdminPage from "./pages/admin/AppLinksAdminPage";
import AuditLogPage from "./pages/admin/AuditLogPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/board"
          element={
            <RequireAuth>
              <ProductionBoardPage />
            </RequireAuth>
          }
        />
        <Route
          path="/reviews"
          element={
            <RequireAuth>
              <ReviewQueuePage />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAuth>
              <RequireRole role="ADMIN">
                <AdminLayout />
              </RequireRole>
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/admin/pizzas" replace />} />
          <Route path="pizzas" element={<PizzasAdminPage />} />
          <Route path="pizzas/:pizzaId/recipe" element={<PizzaRecipePage />} />
          <Route path="ingredients" element={<IngredientsAdminPage />} />
          <Route path="sizes" element={<SizesAdminPage />} />
          <Route path="doughs" element={<DoughsAdminPage />} />
          <Route path="rules" element={<RulesAdminPage />} />
          <Route path="prices" element={<PricesAdminPage />} />
          <Route path="staff" element={<StaffAdminPage />} />
          <Route path="app-links" element={<AppLinksAdminPage />} />
          <Route path="audit" element={<AuditLogPage />} />
        </Route>
        <Route path="/" element={<Navigate to="/board" replace />} />
      </Route>
    </Routes>
  );
}

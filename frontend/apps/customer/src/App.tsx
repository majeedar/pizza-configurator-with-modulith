import { Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import PizzaListPage from "./pages/PizzaListPage";
import ConfiguratorPage from "./pages/ConfiguratorPage";
import BasketPage from "./pages/BasketPage";
import LoginPage from "./pages/LoginPage";
import OrderStatusPage from "./pages/OrderStatusPage";

export default function App() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<PizzaListPage />} />
        <Route path="/configure/:pizzaId" element={<ConfiguratorPage />} />
        <Route path="/basket" element={<BasketPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/order/:displayNumber" element={<OrderStatusPage />} />
      </Route>
    </Routes>
  );
}

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import * as basketApi from "../api/basket";
import type { BasketView } from "../api/types";
import { useAuth } from "./AuthContext";

interface BasketContextValue {
  basket: BasketView | null;
  loading: boolean;
  addItem: (configurationId: string, quantity: number) => Promise<void>;
  removeItem: (basketItemId: string) => Promise<void>;
  refresh: () => Promise<void>;
  startNewBasket: () => Promise<void>;
}

const STORAGE_KEY = "pizza-configurator.basketId";

const BasketContext = createContext<BasketContextValue | undefined>(undefined);

export function BasketProvider({ children }: { children: ReactNode }) {
  const { token } = useAuth();
  const [basket, setBasket] = useState<BasketView | null>(null);
  const [loading, setLoading] = useState(true);

  const ensureBasket = useCallback(async (): Promise<BasketView> => {
    const storedId = localStorage.getItem(STORAGE_KEY);
    if (storedId) {
      try {
        return await basketApi.getBasket(storedId);
      } catch {
        // stale/unknown basket id — fall through and create a new one
      }
    }
    const created = await basketApi.createBasket(token);
    localStorage.setItem(STORAGE_KEY, created.basketId);
    return created;
  }, [token]);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const current = await ensureBasket();
      setBasket(current);
    } finally {
      setLoading(false);
    }
  }, [ensureBasket]);

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const addItem = useCallback(
    async (configurationId: string, quantity: number) => {
      const current = basket ?? (await ensureBasket());
      const updated = await basketApi.addBasketItem(current.basketId, configurationId, quantity);
      setBasket(updated);
    },
    [basket, ensureBasket]
  );

  const removeItem = useCallback(
    async (basketItemId: string) => {
      if (!basket) return;
      const updated = await basketApi.removeBasketItem(basket.basketId, basketItemId);
      setBasket(updated);
    },
    [basket]
  );

  // After a successful checkout the backend marks this basket checked-out —
  // reusing its id would 409 on the next add-item, so start a fresh one
  // rather than calling refresh() (which would just re-fetch the same id).
  const startNewBasket = useCallback(async () => {
    localStorage.removeItem(STORAGE_KEY);
    setLoading(true);
    try {
      const created = await basketApi.createBasket(token);
      localStorage.setItem(STORAGE_KEY, created.basketId);
      setBasket(created);
    } finally {
      setLoading(false);
    }
  }, [token]);

  const value = useMemo<BasketContextValue>(
    () => ({ basket, loading, addItem, removeItem, refresh, startNewBasket }),
    [basket, loading, addItem, removeItem, refresh, startNewBasket]
  );

  return <BasketContext.Provider value={value}>{children}</BasketContext.Provider>;
}

export function useBasket(): BasketContextValue {
  const context = useContext(BasketContext);
  if (!context) {
    throw new Error("useBasket must be used within a BasketProvider");
  }
  return context;
}

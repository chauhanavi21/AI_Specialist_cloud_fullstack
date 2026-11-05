import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/client';
import { useAuth } from './AuthContext';
import { useToast } from './ToastContext';

const CartContext = createContext(null);

const EMPTY_CART = {
  items: [],
  itemCount: 0,
  subtotal: 0,
  estimatedTax: 0,
  shippingFee: 0,
  totalAmount: 0,
  freeShippingThreshold: 499,
};

export function CartProvider({ children }) {
  const { user, openAuthModal } = useAuth();
  const toast = useToast();
  const [cart, setCart] = useState(EMPTY_CART);
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchCart = async () => {
    if (!user) {
      setCart(EMPTY_CART);
      return;
    }
    try {
      setLoading(true);
      setCart((await api.getCart()) || EMPTY_CART);
    } catch (err) {
      console.error('Failed to load cart', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCart();
  }, [user]);

  const addToCart = async (bookId, quantity = 1) => {
    if (!user) {
      openAuthModal('login');
      return;
    }
    try {
      setCart(await api.addToCart(bookId, quantity));
      setIsCartOpen(true);
    } catch (err) {
      toast.error(err.message || 'Failed to add item to basket');
    }
  };

  const updateQuantity = async (itemId, quantity) => {
    try {
      setCart(await api.updateCartItem(itemId, quantity));
    } catch (err) {
      toast.error(err.message || 'Failed to update quantity');
    }
  };

  const removeItem = async (itemId) => {
    try {
      setCart(await api.removeCartItem(itemId));
    } catch (err) {
      toast.error(err.message || 'Failed to remove item');
    }
  };

  const clearCart = async () => {
    try {
      await api.clearCart();
      setCart(EMPTY_CART);
    } catch (err) {
      toast.error(err.message || 'Failed to empty the basket');
    }
  };

  return (
    <CartContext.Provider
      value={{
        cart,
        isCartOpen,
        setIsCartOpen,
        addToCart,
        updateQuantity,
        removeItem,
        clearCart,
        refreshCart: fetchCart,
        loading,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export const useCart = () => useContext(CartContext);

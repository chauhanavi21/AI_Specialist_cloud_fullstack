import React, { createContext, useContext, useState, useEffect } from 'react';
import { api } from '../api/client';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const { user, openAuthModal } = useAuth();
  const [cart, setCart] = useState({
    items: [],
    itemCount: 0,
    subtotal: 0,
    estimatedTax: 0,
    shippingFee: 0,
    totalAmount: 0,
  });
  const [isCartOpen, setIsCartOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const fetchCart = async () => {
    if (!user) {
      setCart({
        items: [],
        itemCount: 0,
        subtotal: 0,
        estimatedTax: 0,
        shippingFee: 0,
        totalAmount: 0,
      });
      return;
    }
    try {
      setLoading(true);
      const data = await api.getCart();
      setCart(data || {
        items: [],
        itemCount: 0,
        subtotal: 0,
        estimatedTax: 0,
        shippingFee: 0,
        totalAmount: 0,
      });
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
      const updatedCart = await api.addToCart(bookId, quantity);
      setCart(updatedCart);
      setIsCartOpen(true);
    } catch (err) {
      alert(err.message || 'Failed to add item to cart');
    }
  };

  const updateQuantity = async (itemId, quantity) => {
    try {
      const updatedCart = await api.updateCartItem(itemId, quantity);
      setCart(updatedCart);
    } catch (err) {
      alert(err.message || 'Failed to update quantity');
    }
  };

  const removeItem = async (itemId) => {
    try {
      const updatedCart = await api.removeCartItem(itemId);
      setCart(updatedCart);
    } catch (err) {
      alert(err.message || 'Failed to remove item');
    }
  };

  const clearCart = async () => {
    try {
      await api.clearCart();
      setCart({
        items: [],
        itemCount: 0,
        subtotal: 0,
        estimatedTax: 0,
        shippingFee: 0,
        totalAmount: 0,
      });
    } catch (err) {
      console.error('Failed to clear cart', err);
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

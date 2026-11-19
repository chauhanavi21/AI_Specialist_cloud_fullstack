package com.ibm.capstone.bookstore.service;

import com.ibm.capstone.bookstore.domain.entities.Book;
import com.ibm.capstone.bookstore.domain.entities.Order;
import com.ibm.capstone.bookstore.domain.entities.OrderItem;
import com.ibm.capstone.bookstore.domain.entities.PaymentStatus;
import com.ibm.capstone.bookstore.domain.entities.PointsTransactionType;
import com.ibm.capstone.bookstore.domain.entities.User;
import com.ibm.capstone.bookstore.repository.BookRepository;
import com.ibm.capstone.bookstore.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Applies a refund worked out by {@link RefundCalculator}: puts the books back on the shelf, returns and
 * revokes gift points, pays wallet money back and updates the order's refund totals. Cancelling, editing and
 * returning all go through here so the rules cannot drift apart.
 *
 * Card and UPI refunds go back to the original instrument through the (simulated) gateway, so they only produce
 * a reference. Wallet payments are credited straight back to the wallet.
 */
@Component
public class RefundProcessor {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final GiftPointsService giftPointsService;
    private final WalletService walletService;

    public RefundProcessor(BookRepository bookRepository, UserRepository userRepository,
                           GiftPointsService giftPointsService, WalletService walletService) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.giftPointsService = giftPointsService;
        this.walletService = walletService;
    }

    /**
     * @param units   units refunded per order item, parallel to {@code order.getItems()}
     * @param restock whether the books go back into stock (true for cancellations, edits and accepted returns)
     * @param reason  short text for the ledger entries, e.g. "cancelled Order #BK-1"
     * @return the refund reference
     */
    public String apply(Order order, RefundCalculator.Refund refund, int[] units, boolean restock, String reason) {
        for (int i = 0; i < units.length; i++) {
            if (units[i] <= 0) continue;
            OrderItem item = order.getItems().get(i);
            item.setRefundedQuantity(item.getRefundedQuantity() + units[i]);
            Book book = item.getBook();
            if (restock && book != null) {
                book.setStockQuantity(book.getStockQuantity() + units[i]);
                book.setCopiesSold(Math.max(0, book.getCopiesSold() - units[i]));
                bookRepository.save(book);
            }
        }

        User user = order.getUser();
        if (refund.pointsBack() > 0) {
            user.setGiftPointsBalance(user.getGiftPointsBalance() + refund.pointsBack());
            giftPointsService.recordTransaction(user, order.getId(), refund.pointsBack(),
                    PointsTransactionType.REFUNDED, "Refunded points from " + reason);
            order.setPointsRefunded(order.getPointsRefunded() + refund.pointsBack());
        }
        int revoked = Math.min(refund.pointsToRevoke(), user.getGiftPointsBalance());
        if (revoked > 0) {
            user.setGiftPointsBalance(user.getGiftPointsBalance() - revoked);
            giftPointsService.recordTransaction(user, order.getId(), revoked,
                    PointsTransactionType.REVOKED, "Points reversed for " + reason);
            order.setPointsRevoked(order.getPointsRevoked() + revoked);
        }
        userRepository.save(user);

        BigDecimal cash = refund.cashBack();
        if (order.getWalletAmountUsed() != null && order.getWalletAmountUsed().signum() > 0 && cash.signum() > 0) {
            walletService.credit(user, cash, order.getId(), "Refund for " + reason);
        }
        order.setRefundedAmount(order.getRefundedAmount().add(cash));
        order.setPaymentStatus(order.getRefundedAmount().compareTo(order.getTotalAmount()) >= 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);

        return "RFD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}

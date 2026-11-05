package com.ibm.capstone.bookstore.controller;

import com.ibm.capstone.bookstore.domain.dto.BookSummaryResponse;
import com.ibm.capstone.bookstore.domain.dto.StoreDto;
import com.ibm.capstone.bookstore.security.UserPrincipal;
import com.ibm.capstone.bookstore.service.EngagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Wishlist & Writers", description = "My Wishlist, My Writers and writer profiles")
public class EngagementController {

    private final EngagementService engagementService;

    public EngagementController(EngagementService engagementService) {
        this.engagementService = engagementService;
    }

    @GetMapping("/wishlist")
    @Operation(summary = "Books on the customer's wishlist")
    public ResponseEntity<List<BookSummaryResponse>> getWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(engagementService.getWishlist(principal.getId()));
    }

    @PutMapping("/wishlist/{bookId}")
    @Operation(summary = "Add a book to the wishlist (idempotent)")
    public ResponseEntity<Void> addToWishlist(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long bookId) {
        engagementService.addToWishlist(principal.getId(), bookId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/wishlist/{bookId}")
    @Operation(summary = "Remove a book from the wishlist")
    public ResponseEntity<Void> removeFromWishlist(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long bookId) {
        engagementService.removeFromWishlist(principal.getId(), bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/writers")
    @Operation(summary = "All writers; pass followed=true for the signed-in customer's writers only")
    public ResponseEntity<List<StoreDto.AuthorSummary>> getWriters(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean followed) {
        Long userId = principal != null ? principal.getId() : null;
        if (followed && userId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(engagementService.getWriters(userId, followed));
    }

    @GetMapping("/writers/{id}")
    @Operation(summary = "Writer profile with their books")
    public ResponseEntity<StoreDto.AuthorDetail> getWriter(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(engagementService.getWriter(id, principal != null ? principal.getId() : null));
    }

    @PutMapping("/writers/{id}/follow")
    @Operation(summary = "Follow a writer (idempotent)")
    public ResponseEntity<Void> follow(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        engagementService.follow(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/writers/{id}/follow")
    @Operation(summary = "Unfollow a writer")
    public ResponseEntity<Void> unfollow(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        engagementService.unfollow(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

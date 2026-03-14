package com.platform.orderservice.client;

public record InventoryItemResponse(String itemId, String productId, int availableQuantity) {}

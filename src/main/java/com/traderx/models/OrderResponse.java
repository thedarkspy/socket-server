package com.traderx.models;

import java.util.List;

public record OrderResponse(
        List<Order> rowData,
        int lastRow
) {}

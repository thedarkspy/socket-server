package com.traderx.models;

import java.util.List;
import java.util.Map;

public record OrderRequest(
        int startRow,
        int endRow,
        List<SortModel> sortModel,
        Map<String, FilterModel> filterModel
) {}
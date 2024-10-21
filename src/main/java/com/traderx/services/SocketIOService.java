package com.traderx.services;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.traderx.models.*;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
class SocketIOService {
    private final SocketIOServer server;
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public SocketIOService(SocketIOServer server) {
        this.server = server;
        setupEventListeners();
        createInitialOrders();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startServer() {
        server.start();
        System.out.println("SocketIO server started on port 8001");
    }

    @PreDestroy
    public void stopServer() {
        if (server != null) {
            server.stop();
            System.out.println("SocketIO server stopped");
        }
    }

    private void setupEventListeners() {
        server.addConnectListener(client -> {
            System.out.println("Client connected: " + client.getSessionId());
        });

        server.addDisconnectListener(client ->
                System.out.println("Client disconnected: " + client.getSessionId())
        );

        server.addEventListener("getOrders", OrderRequest.class, (client, data, ackRequest) -> {
            System.out.println("Received getOrders request from client: " + client.getSessionId());
            processOrderRequest(client, data);
        });
    }

    private void processOrderRequest(SocketIOClient client, OrderRequest data) {
        try {
            List<Order> filteredData = new ArrayList<>(orders.values());

            // Apply filters if present
            if (data.filterModel() != null && !data.filterModel().isEmpty()) {
                filteredData = applyFilters(filteredData, data.filterModel());
            }

            // Apply sorting if present
            if (data.sortModel() != null && !data.sortModel().isEmpty()) {
                filteredData = applySorting(filteredData, data.sortModel());
            }

            // Ensure endRow doesn't exceed the list size
            int endRow = Math.min(data.endRow(), filteredData.size());
            int startRow = Math.min(data.startRow(), endRow);

            // Create a new ArrayList for the paged data
            List<Order> pagedData = new ArrayList<>(
                    filteredData.subList(startRow, endRow)
            );

            OrderResponse response = new OrderResponse(pagedData, filteredData.size());
            client.sendEvent("orderUpdate", response);
            System.out.println("Sent order update to client: " + client.getSessionId());
        } catch (Exception e) {
            System.err.println("Error processing order request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 5000)
    public void generateRandomOrderUpdate() {
        try {
            Order newOrder = new Order(
                    Math.random() * 1000,
                    new Date()
            );
            orders.put(newOrder.getId(), newOrder);
            server.getBroadcastOperations().sendEvent("newOrder");
            System.out.println("Broadcasted new order update");
        } catch (Exception e) {
            System.err.println("Error generating random order: " + e.getMessage());
        }
    }

    private void createInitialOrders() {
        System.out.println("Inserting data into the cache..");
        for (int i = 0; i < 100000; i++) {
            Order order = new Order(
                    Math.random() * 1000,
                    new Date(System.currentTimeMillis() - (long) (Math.random() * 10000000000L))
            );
            orders.put(order.getId(), order);
        }
        System.out.println("Insertion complete");
    }

    private List<Order> applyFilters(List<Order> data, Map<String, FilterModel> filterModel) {
        return data.stream()
                .filter(item -> filterModel.entrySet().stream().allMatch(entry -> {
                    String field = entry.getKey();
                    FilterModel filter = entry.getValue();
                    Object itemValue = getFieldValue(item, field);

                    return switch (filter.type()) {
                        case "contains" -> itemValue.toString().toLowerCase()
                                .contains(filter.filter().toLowerCase());
                        case "equals" -> itemValue.toString().equals(filter.filter().toString());
                        case "greaterThan" -> itemValue instanceof Number &&
                                ((Number) itemValue).doubleValue() >= Double.parseDouble(filter.filter());
                        case "lessThan" -> itemValue instanceof Number &&
                                ((Number) itemValue).doubleValue() <= Double.parseDouble(filter.filter());
                        default -> true;
                    };
                }))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Order> applySorting(List<Order> data, List<SortModel> sortModel) {
        ArrayList<Order> sortedList = new ArrayList<>(data);
        sortedList.sort((a, b) -> {
            for (SortModel sort : sortModel) {
                Object valueA = getFieldValue(a, sort.colId());
                Object valueB = getFieldValue(b, sort.colId());

                if (valueA instanceof Comparable && valueB instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int comparison = ((Comparable) valueA).compareTo(valueB);
                    if (comparison != 0) {
                        return sort.sort().equals("asc") ? comparison : -comparison;
                    }
                }
            }
            return 0;
        });
        return sortedList;
    }

    private Object getFieldValue(Order order, String field) {
        return switch (field) {
            case "id" -> order.getId();
            case "price" -> order.getPrice();
            case "date" -> order.getDate();
            default -> null;
        };
    }
}

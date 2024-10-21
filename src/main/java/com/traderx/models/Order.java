package com.traderx.models;

import lombok.Data;
import java.util.Date;
import java.util.UUID;

@Data
public class Order {
    private final String id;
    private final double price;
    private final Date date;

    public Order(double price, Date date) {
        this.id = UUID.randomUUID().toString();
        this.price = Double.parseDouble(String.format("%.2f", price));
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public Date getDate() {
        return date;
    }
}
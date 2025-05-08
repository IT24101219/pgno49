package com.pgno49.salon_project.util;

import com.pgno49.salon_project.model.Appointment;

public class CustomAppointmentQueue {
    private Appointment[] queArray;
    private int maxSize;
    private int front;
    private int rear;
    private int nItems;


    public CustomAppointmentQueue(int s) {
        maxSize = s;
        queArray = new Appointment[maxSize];
        front = 0;
        rear = -1;
        nItems = 0;
    }


    public boolean insert(Appointment item) {
        if (isFull()) {
            System.out.println("Queue is full. Cannot insert appointment request for customer ID: " + item.getCustomerId());
            return false;
        }
        if (rear == maxSize - 1) {
            rear = -1;
        }
        queArray[++rear] = item;
        nItems++;
        System.out.println("Inserted appointment request for customer ID: " + item.getCustomerId() + " into queue. Items: " + nItems);
        return true;
    }


    public Appointment remove() {
        if (isEmpty()) {
            System.out.println("Queue is empty. Cannot remove.");
            return null;
        }
        Appointment temp = queArray[front++];
        if (front == maxSize) {
            front = 0;
        }
        nItems--;
        System.out.println("Removed appointment request for customer ID: " + (temp != null ? temp.getCustomerId() : "N/A") + " from queue. Items left: " + nItems);
        return temp;
    }


    public Appointment peekFront() {
        if (isEmpty()) {
            return null;
        }
        return queArray[front];
    }


    public boolean isEmpty() {
        return (nItems == 0);
    }


    public boolean isFull() {
        return (nItems == maxSize);
    }

    public int size() {
        return nItems;
    }


    public void displayQueue() {
        System.out.print("Queue (front-->rear): ");
        if (isEmpty()) {
            System.out.println("Empty");
            return;
        }
        int currentCount = 0;
        int tempFront = front;
        while (currentCount < nItems) {
            System.out.print(queArray[tempFront].getId() + "[" + queArray[tempFront].getCustomerId() + "] ");
            tempFront++;
            if (tempFront == maxSize) {
                tempFront = 0;
            }
            currentCount++;
        }
        System.out.println();
    }
}
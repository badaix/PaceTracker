package de.badaix.pacetracker.util;

public class TextItemPair<T> {
    private String text;
    private T item;

    public TextItemPair(String text, T item) {
        this.text = text;
        this.item = item;
    }

    public String getText() {
        return text;
    }

    public T getItem() {
        return item;
    }

    @Override
    public String toString() {
        return getText();
    }
}

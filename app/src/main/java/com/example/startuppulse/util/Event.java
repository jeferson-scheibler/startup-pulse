package com.example.startuppulse.util;

import androidx.lifecycle.Observer;

public class Event<T> {
    private final T content;
    private boolean hasBeenHandled = false;

    public Event(T content) {
        this.content = content;
    }

    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        } else {
            hasBeenHandled = true;
            return content;
        }
    }

    public T peekContent() {
        return content;
    }

    // --- CLASSE INTERNA ADICIONADA ---
    /**
     * Um Observer customizado para eventos LiveData.
     * Ele previne que o evento seja disparado novamente em
     * reconfigurações (como rotação de tela).
     */
    public static class EventObserver<T> implements Observer<Event<T>> {
        private final OnEventUnhandledContent<T> onEventUnhandledContent;

        public EventObserver(OnEventUnhandledContent<T> onEventUnhandledContent) {
            this.onEventUnhandledContent = onEventUnhandledContent;
        }

        @Override
        public void onChanged(Event<T> event) {
            if (event != null) {
                T content = event.getContentIfNotHandled();
                if (content != null) {
                    onEventUnhandledContent.onEventUnhandled(content);
                }
            }
        }

        // Interface para o callback
        public interface OnEventUnhandledContent<T> {
            void onEventUnhandled(T content);
        }
    }
}
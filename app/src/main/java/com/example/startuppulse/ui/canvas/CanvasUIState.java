package com.example.startuppulse.ui.canvas;

import com.example.startuppulse.data.models.Ideia;

public class CanvasUIState {
    public final Ideia ideia;
    public final boolean isReadOnly;
    public final boolean isOwner;
    public final boolean isMentorPodeAvaliar;

    public CanvasUIState(Ideia ideia, boolean isReadOnly, boolean isOwner, boolean isMentorPodeAvaliar) {
        this.ideia = ideia;
        this.isReadOnly = isReadOnly;
        this.isOwner = isOwner;
        this.isMentorPodeAvaliar = isMentorPodeAvaliar;
    }
}
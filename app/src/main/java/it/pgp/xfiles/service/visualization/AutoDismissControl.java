package it.pgp.xfiles.service.visualization;

import java.util.concurrent.atomic.AtomicBoolean;

public class AutoDismissControl {
    private final Runnable onDismiss;

    public void disableDismissTimeout() {
        currentDismissChoice.set(false);
    }
    private final AtomicBoolean currentDismissChoice = new AtomicBoolean(true);
    public void dynamicDismiss() {
        if (currentDismissChoice.get()) onDismiss.run();
    }

    public AutoDismissControl(Runnable onDismiss) {
        this.onDismiss = onDismiss;
    }
}

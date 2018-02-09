package com.example.svenu.blinktest;

/**
 * Created by svenu on 9-2-2018.
 */

public class BooleanChanger {

    private boolean value;
    private VariableChangeListener variableChangeListener;

    public BooleanChanger(boolean value) {
        this.value = value;
    }

    public void setValue(boolean value) {
        if (this.value != value) {
            this.value = value;
            signalChanged();
        }
    }

    public boolean getValue() {
        return value;
    }

    private void signalChanged() {
        if (variableChangeListener != null) {
            variableChangeListener.onVariableChanged(this);
        }
    }

    public interface VariableChangeListener {
        void onVariableChanged(BooleanChanger booleanChanger);
    }

    public void setVariableChangeListener(VariableChangeListener variableChangeListener) {
        this.variableChangeListener = variableChangeListener;
    }
}

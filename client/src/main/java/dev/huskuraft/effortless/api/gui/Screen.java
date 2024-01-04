package dev.huskuraft.effortless.api.gui;

import dev.huskuraft.effortless.api.text.Text;

public interface Screen extends ContainerWidget {

    Text getScreenTitle();

    void onAttach();

    void onDetach();

    boolean isPauseGame();

    void attach();

    void detach();

}


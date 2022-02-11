package org.ametro.ui.navigation;


import android.view.View;
import org.ametro.model.entities.MapDelay;
import org.jetbrains.annotations.NotNull;

public interface INavigationControllerListener {
    boolean onOpenMaps();

    boolean onOpenSettings();

    boolean onChangeScheme(String schemeName);

    boolean onToggleTransport(String source, boolean checked);

    boolean onDelayChanged(MapDelay delay);

    boolean onOpenAbout();

    void onDrawerSlide(@NotNull View drawerView, float slideOffset);
}

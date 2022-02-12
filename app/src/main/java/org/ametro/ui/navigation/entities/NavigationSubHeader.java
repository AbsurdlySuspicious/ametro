package org.ametro.ui.navigation.entities;

import androidx.annotation.Nullable;

public class NavigationSubHeader extends NavigationItem implements INavigationItemGroup {

    private final CharSequence text;
    private final NavigationItem[] items;

    public NavigationSubHeader(CharSequence text, NavigationItem[] items){
        this(text, items, null);
    }

    public NavigationSubHeader(CharSequence text, NavigationItem[] items, String tag){
        super(INVALID_ACTION);
        this.text = text;
        this.items = items;
        this.tag = tag;
    }

    public CharSequence getText() {
        return text;
    }

    public NavigationItem[] getItems() {
        return items;
    }
}

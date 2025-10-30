package com.shop.gramasandhai.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.appbar.AppBarLayout;

public class FixedSearchBehavior extends CoordinatorLayout.Behavior<View> {

    public FixedSearchBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            AppBarLayout appBarLayout = (AppBarLayout) dependency;
            int toolbarHeight = appBarLayout.getHeight();

            // Always position search bar below toolbar
            child.setY(toolbarHeight + 8); // 8dp margin from toolbar
            return true;
        }
        return false;
    }
}
/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.samsungxr.controls.menu;

import android.content.res.TypedArray;

import com.samsungxr.SXRContext;
import com.samsungxr.SXRNode;
import com.samsungxr.controls.focus.GamepadTouchImpl;
import com.samsungxr.controls.focus.TouchAndGestureImpl;
import com.samsungxr.controls.input.GamepadMap;

import java.util.ArrayList;
import java.util.List;

public class GridNodes extends SXRNode {

    private float horizontalSpacing;
    private int numColumns;
    private int numLines;
    private float verticalSpacing;
    private MenuControlNode buttonSelected = null;
    private ItemSelectedListener listener;
    private List<MenuControlNode> listItens = new ArrayList<MenuControlNode>();
    private TypedArray grid;
    private int gridConfig;

    public GridNodes(SXRContext sxrContext, ArrayList<MenuControlNode> listItens,
                            int gridConfig, ItemSelectedListener listener) {
        super(sxrContext);

        this.gridConfig = gridConfig;
        this.listItens = listItens;
        this.listener = listener;

        configureGrid();
        attachItems();
    }

    private void configureGrid() {

        grid = getSXRContext().getContext().getResources().obtainTypedArray(gridConfig);

        horizontalSpacing = grid.getFloat(1, -1);
        numColumns = grid.getInt(2, -1);
        numLines = grid.getInt(3, -1);
        verticalSpacing = grid.getFloat(4, -1);
    }

    private void attachItems() {

        int index = 0;
        int n = numLines;
        float vertical = 0;
        float horizontal = 0;
        float z = 0.f;

        for (int i = 0; i < numColumns; i++) {

            for (; index < n; index++) {

                attachGridItem(index, vertical, horizontal, z);

                vertical += verticalSpacing;
            }

            vertical = 0;
            horizontal += horizontalSpacing;
            n += numLines;
        }

        selectFirstItem();
    }

    private void selectFirstItem() {
        buttonSelected = ((MenuControlNode) getChildren().get(0));
        buttonSelected.select();
    }

    private void attachGridItem(int index, float vertical, float horizontal, float z) {

        final MenuControlNode item = listItens.get(index);

        item.setTouchAndGesturelistener(new TouchAndGestureImpl() {

            @Override
            public void singleTap() {
                super.singleTap();
                handleUISelection(item);
            }
        });

        item.setGamepadTouchListener(new GamepadTouchImpl() {

            @Override
            public void down(Integer code) {
                super.down(code);

                if (code == GamepadMap.KEYCODE_BUTTON_A ||
                        code == GamepadMap.KEYCODE_BUTTON_B ||
                        code == GamepadMap.KEYCODE_BUTTON_X ||
                        code == GamepadMap.KEYCODE_BUTTON_Y) {

                    handleUISelection(item);

                    item.select();
                }
            }
        });

        item.getTransform().setPosition(horizontal, vertical, z);

        this.addChildObject(item);
    }

    private void handleUISelection(MenuControlNode object) {

        if (buttonSelected != null) {

            if (buttonSelected != object) {
                buttonSelected.unselect();
            }
        }

        buttonSelected = object;

        listener.selected(buttonSelected);
    }
}
/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.localmediaprovider;

import android.database.Cursor;

public interface CursorHandler<T> {
    T handle(Cursor cu);
}

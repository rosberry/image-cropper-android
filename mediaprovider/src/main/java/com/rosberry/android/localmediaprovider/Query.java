/*
 *
 *  * Copyright (c) 2019 Rosberry. All rights reserved.
 *
 */

package com.rosberry.android.localmediaprovider;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import java.util.Arrays;

import androidx.annotation.NonNull;

class Query {

    private final Uri uri;
    private final String[] projection;
    private final String selection;
    private final String[] args;
    private final String sort;
    private final boolean ascending;
    private final int limit;

    private Query(Builder builder) {
        uri = builder.uri;
        projection = builder.projection;
        selection = builder.selection;
        args = builder.getStringArgs();
        sort = builder.sort;
        ascending = builder.ascending;
        limit = builder.limit;
    }

    Cursor getCursor(ContentResolver cr) {
        return cr.query(uri, projection, selection, args, hack());
    }

    private String hack() {
        if (sort == null && limit == Constant.NO_LIMIT) return null;

        StringBuilder builder = new StringBuilder();
        if (sort != null)
            builder.append(sort);

            // Sorting by Relative Position
            // ORDER BY 1
            // sort by the first column in the PROJECTION
            // otherwise the LIMIT should not work
        else builder.append(1);

        builder.append(" ");

        if (!ascending)
            builder.append("DESC").append(" ");

        if (limit != Constant.NO_LIMIT)
            builder.append("LIMIT").append(" ").append(limit);

        return builder.toString();
    }

    public static final class Builder {
        Uri uri = null;
        String[] projection = null;
        String selection = null;
        Object[] args = null;
        String sort = null;
        int limit = Constant.NO_LIMIT;
        boolean ascending = false;

        Builder() {
        }

        Builder uri(Uri val) {
            uri = val;
            return this;
        }

        Builder projection(String[] val) {
            projection = val;
            return this;
        }

        Builder selection(String val) {
            selection = val;
            return this;
        }

        Builder args(Object... val) {
            args = val;
            return this;
        }

        Builder sort(String val) {
            sort = val;
            return this;
        }

        Builder limit(int val) {
            limit = val;
            return this;
        }

        Builder ascending(boolean val) {
            ascending = val;
            return this;
        }

        Query build() {
            return new Query(this);
        }

        private String[] getStringArgs() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                return Arrays.stream(args).map(Object::toString).toArray(String[]::new);

            String[] list = new String[args.length];
            for (int i = 0; i < args.length; i++) list[i] = String.valueOf(args[i]);
            return list;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "Query{" +
                "\nuri=" + uri +
                "\nprojection=" + Arrays.toString(projection) +
                "\nselection='" + selection + '\'' +
                "\nargs=" + Arrays.toString(args) +
                "\nsortMode='" + sort + '\'' +
                "\nascending='" + ascending + '\'' +
                "\nlimit='" + limit + '\'' +
                '}';
    }
}
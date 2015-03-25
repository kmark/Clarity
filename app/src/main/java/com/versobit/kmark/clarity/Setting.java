/*
 * Copyright (C) 2015 Kevin Mark
 *
 * This file is part of Clarity.
 *
 * Clarity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Clarity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Clarity.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.versobit.kmark.clarity;

import java.util.Locale;

enum Setting {
    THUMBNAIL_DIM,
    DATABASE_PROCESSOR,
    SHOW_ICON,
    ABOUT_VERSION,
    DEBUG;

    static Setting fromString(String name) {
        return Setting.valueOf(name.toUpperCase(Locale.US));
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.US);
    }
}

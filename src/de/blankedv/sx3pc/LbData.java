/*
 * Copyright (C) 2018 mblank
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.blankedv.sx3pc;

import static de.blankedv.sx3pc.MainUI.*;

/**
 *
 * @author mblank
 */
public class LbData {
    private int data;  
    private int nbit;
    private String tn; 
    
    public LbData(int d, int nb, String name) {
        data = d;
        nbit = nb;
        tn = name;
    }
    
    public LbData(int d) {
        data = d;
        nbit = 1;
        tn = "A";
    }

  
    public int getData() {
        return data;
    }
    
    public int getNBit() {
        return nbit;
    }
    
    public String getTypeString() {
        StringBuilder sb = new StringBuilder();
        if (data == INVALID_INT) return ("");
        
        return tn;
    }
    
}

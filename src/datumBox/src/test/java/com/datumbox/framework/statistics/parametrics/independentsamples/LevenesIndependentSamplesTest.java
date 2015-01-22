/* 
 * Copyright (C) 2014 Vasilis Vryniotis <bbriniotis at datumbox.com>
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
package com.datumbox.framework.statistics.parametrics.independentsamples;

import com.datumbox.common.dataobjects.FlatDataCollection;
import com.datumbox.common.dataobjects.TransposeDataCollection;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bbriniotis
 */
public class LevenesIndependentSamplesTest {
    
    public LevenesIndependentSamplesTest() {
    }

    /**
     * Test of testVariances method, of class LevenesIndependentSamples.
     */
    @Test
    public void testTestVariances() {
        System.out.println("testVariances");
        TransposeDataCollection transposeDataCollection = new TransposeDataCollection(new LinkedHashMap<>());
        
        transposeDataCollection.put(0, new FlatDataCollection(Arrays.asList(new Object[]{60.8,57.0,65.0,58.6,61.7})));
        transposeDataCollection.put(1, new FlatDataCollection(Arrays.asList(new Object[]{68.7,67.7,74.0,66.3,69.8})));
        transposeDataCollection.put(2, new FlatDataCollection(Arrays.asList(new Object[]{102.6,103.1,100.2,96.5})));
        transposeDataCollection.put(3, new FlatDataCollection(Arrays.asList(new Object[]{87.9,84.2,83.1,85.7,90.3})));
        
        double aLevel = 0.05;
        boolean expResult = true;
        boolean result = LevenesIndependentSamples.testVariances(transposeDataCollection, aLevel);
        assertEquals(expResult, result);
    }
    
}

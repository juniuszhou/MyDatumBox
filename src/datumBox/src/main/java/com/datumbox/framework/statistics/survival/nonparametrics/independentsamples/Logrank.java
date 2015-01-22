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
package com.datumbox.framework.statistics.survival.nonparametrics.independentsamples;

import com.datumbox.common.dataobjects.AssociativeArray;
import com.datumbox.common.dataobjects.AssociativeArray2D;
import com.datumbox.common.dataobjects.Dataset;
import com.datumbox.common.dataobjects.FlatDataCollection;
import com.datumbox.common.dataobjects.TransposeDataCollection;
import com.datumbox.framework.statistics.descriptivestatistics.CensoredDescriptives;
import com.datumbox.framework.statistics.distributions.ContinuousDistributions;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author bbriniotis
 */
public class Logrank {
    /**
     * The internalDataCollections that are passed in this function are NOT modified after the analysis. 
     * You can safely pass directly the internalDataCollection without worrying about having them modified.
     */
    public static final boolean DATA_SAFE_CALL_BY_REFERENCE = true;
    
    /**
     * Calculates the p-value of null Hypothesis.
     * 
     * @param transposeDataCollection
     * @return
     * @throws IllegalArgumentException 
     */
    public static double getPvalue(TransposeDataCollection transposeDataCollection) throws IllegalArgumentException {
        if(transposeDataCollection.size()!=2) {
            throw new IllegalArgumentException();
        }
        
        Object[] keys = transposeDataCollection.keySet().toArray();
        
        //counter of uncencored internalData in each group
        Map<Object, Integer> n = new HashMap<>();
        n.put(keys[0], 0);
        n.put(keys[1], 0);
        
        Queue<Double> censoredData = new PriorityQueue<>();
        Queue<Double> uncensoredData = new PriorityQueue<>();
        for(Map.Entry<Object, FlatDataCollection> entry : transposeDataCollection.entrySet()) {
            Object j = entry.getKey();
            FlatDataCollection flatDataCollection = entry.getValue();
            
            for(Object value : flatDataCollection) {
                String str = value.toString();
                if(str.endsWith(CensoredDescriptives.CENSORED_NUMBER_POSTFIX)) {
                    //censored internalData encoded as 4.3+ or -4.3+
                    censoredData.add(Double.valueOf(str.substring(0,str.length()-CensoredDescriptives.CENSORED_NUMBER_POSTFIX.length()))); //remove the trailing char and convert it to double
                }
                else {
                    //uncensored internalData
                    uncensoredData.add(Dataset.toDouble(value)); //convert it to double
                }
                n.put(j, n.get(j)+1);
            }
        }

        
        Double currentCensored = null;
        Double currentUncensored = null;
        AssociativeArray2D testTable = new AssociativeArray2D(new LinkedHashMap<>());
        
        do {
            if(currentCensored==null) {
                currentCensored=censoredData.poll();
            }
            if(currentUncensored==null) {
                currentUncensored=uncensoredData.poll();
            }
            
            Double ti;
            String key;
            if(currentUncensored == null) {
                key=currentCensored.toString().concat((CensoredDescriptives.CENSORED_NUMBER_POSTFIX));
                ti = currentCensored;
                currentCensored = null;
            }
            else if(currentCensored == null) {
                key=currentUncensored.toString();
                ti = currentUncensored;
                currentUncensored = null;
            }
            else if(currentCensored<currentUncensored) { //NOT EQUAL! Uncensored internalData of the same value are always larger
                key=currentCensored.toString().concat(CensoredDescriptives.CENSORED_NUMBER_POSTFIX);
                ti = currentCensored;
                currentCensored = null;
            }
            else {
                key=currentUncensored.toString();
                ti = currentUncensored;
                currentUncensored = null;
            }
            
            Object value = testTable.get2d(key, "mi");
            if(value==null) {
                testTable.put2d(key, "mi", 1);
                testTable.put2d(key, "rti", 0);
            }
            else {
                testTable.put2d(key, "mi", ((Integer)value) +1); 
                continue; //continue in order not to count twice the r*ti below
            }
            
            for(Map.Entry<Object, FlatDataCollection> entry : transposeDataCollection.entrySet()) {
                Object j = entry.getKey();
                FlatDataCollection flatDataCollection = entry.getValue();
                
                for(Object value2 : flatDataCollection) {
                    double v;
                    String str = value2.toString();
                    if(str.endsWith(CensoredDescriptives.CENSORED_NUMBER_POSTFIX)) {
                        //censored internalData encoded as 4.3+ or -4.3+
                        v = Double.valueOf(str.substring(0,str.length()-CensoredDescriptives.CENSORED_NUMBER_POSTFIX.length())); //remove the trailing char and convert it to double
                    }
                    else {
                        //uncensored internalData
                        v = Dataset.toDouble(value2); //convert it to double
                    }
                    
                    if(v>=ti) {
                        testTable.put2d(key, "rti", (Integer)testTable.get2d(key, "rti") +1);
                    }
                }
            }
                
        } 
        while(currentCensored!=null || currentUncensored!=null || 
              !censoredData.isEmpty() || !uncensoredData.isEmpty());
        
        censoredData=null;
        uncensoredData=null;
        

        double VarS=0.0;

        Object previousUncencoredKey = null;
        for(Map.Entry<Object, AssociativeArray> entry : testTable.entrySet()) {
            Object ti = entry.getKey();
            AssociativeArray testRow = entry.getValue();

            double previousUncencoredValue=0;
            
            Object tmp = testTable.get2d(previousUncencoredKey, "eti");
            if(tmp!=null) {
                previousUncencoredValue = Dataset.toDouble(tmp);
            }
            
            if(!ti.toString().endsWith(CensoredDescriptives.CENSORED_NUMBER_POSTFIX)) { //uncensored
                double mi = testRow.getDouble("mi");
                double rti = testRow.getDouble("rti");
                double eti = previousUncencoredValue+mi/rti;
                
                testRow.put("eti", eti);
                testRow.put("wi", 1-eti);
                previousUncencoredKey=ti;
            }
            else { //censored
                testRow.put("wi", -previousUncencoredValue);
            }

            double wi = testRow.getDouble("wi");
            VarS+= testRow.getDouble("mi")*wi*wi;
        }


        double S=0.0;
        for(Object value : transposeDataCollection.get(keys[0])) { //if ti belongs to the first group
            Object key; //we must first convert the number into to double and then append the + if necessary. This is why it's converted like this.
            String str = value.toString();
            if(str.endsWith(CensoredDescriptives.CENSORED_NUMBER_POSTFIX)) {
                //censored internalData encoded as 4.3+ or -4.3+
                Double v = Double.valueOf(str.substring(0,str.length()-CensoredDescriptives.CENSORED_NUMBER_POSTFIX.length())); //remove the trailing char and convert it to double
                key = v.toString()+CensoredDescriptives.CENSORED_NUMBER_POSTFIX;
            }
            else {
                //uncensored internalData
                Double v = Dataset.toDouble(value); //convert it to double
                key = v.toString();
            }
            double wi = Dataset.toDouble(testTable.get2d(key, "wi"));
            S+= wi;
        }
        testTable = null;
        
        double n0 = n.get(keys[0]).doubleValue();
        double n1 = n.get(keys[1]).doubleValue();
        
        VarS*=n0*n1/((n0+n1)*(n0+n1-1.0));

        double Z=S/Math.sqrt(VarS);
        
        double pvalue = scoreToPvalue(Z);

        return pvalue;
    }
    
    /**
     * Tests the rejection of null Hypothesis for a particular confidence level.
     * 
     * @param transposeDataCollection
     * @param is_twoTailed
     * @param aLevel
     * @return 
     */
    public static boolean test(TransposeDataCollection transposeDataCollection, boolean is_twoTailed, double aLevel) {
        double pvalue = getPvalue(transposeDataCollection);

        boolean rejectH0=false;

        double a=aLevel;
        if(is_twoTailed) { //if to tailed test then split the statistical significance in half
            a=aLevel/2;
        }
        if(pvalue<=a || pvalue>=(1-a)) {
            rejectH0=true; 
        }

        return rejectH0;
    }

    /**
     * Returns the Pvalue for a particular score.
     * 
     * @param score
     * @return 
     */
    protected static double scoreToPvalue(double score) {
        return ContinuousDistributions.GaussCdf(score);
    }
    
}

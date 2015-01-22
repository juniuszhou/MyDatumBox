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
package com.datumbox.framework.machinelearning.classification;

import com.datumbox.common.dataobjects.AssociativeArray;
import com.datumbox.common.dataobjects.Dataset;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.persistentstorage.factories.BigDataStructureFactory;
import com.datumbox.common.persistentstorage.interfaces.BigDataStructureMarker;
import com.datumbox.configuration.GeneralConfiguration;
import com.datumbox.configuration.MemoryConfiguration;
import com.datumbox.configuration.StorageConfiguration;
import com.datumbox.framework.machinelearning.common.bases.mlmodels.BaseMLclassifier;
import com.datumbox.framework.machinelearning.common.validation.OrdinalRegressionValidation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.mongodb.morphia.annotations.Transient;

/**
 *
 * @author Vasilis Vryniotis <bbriniotis at datumbox.com>
 */
public class OrdinalRegression extends BaseMLclassifier<OrdinalRegression.ModelParameters, OrdinalRegression.TrainingParameters, OrdinalRegression.ValidationMetrics> {
    /*
    References: 
        - http://qwone.com/~jason/writing/olr.pdf
        - http://www.rbsd.de/PDF/olr_mr.pdf
        - http://www.econ.kuleuven.be/public/ndbae06/pdf-files/Robust%20estimation%20for%20ordinal%20regression.pdf
        - http://fa.bianp.net/blog/2013/logistic-ordinal-regression/ 
        - https://github.com/fabianp/minirank/blob/master/minirank/logistic.py 
        - https://github.com/gcapan/recommender/blob/master/recommender-core/src/main/java/com/discovery/recommender/gradient/RegularizedOrdinalGradient.java
        - http://www.stat.uchicago.edu/~pmcc/pubs/paper2.pdf
        - http://ttic.uchicago.edu/~nati/Publications/RennieSrebroIJCAI05.pdf
        - http://rbakker.myweb.uga.edu/pols8501/MLENotes6a.pdf
        - http://www.academicjournals.org/article/article1379683447_Tamayo.pdf
    */
    /**
     * The internalDataCollections that are passed in this function are NOT modified after the analysis. 
     * You can safely pass directly the internalDataCollection without worrying about having them modified.
     */
    public static final boolean DATA_SAFE_CALL_BY_REFERENCE = true;
    
    public static final String SHORT_METHOD_NAME = "OrReg";
    
    public static class ModelParameters extends BaseMLclassifier.ModelParameters {

        /**
         * W weights
         */
        @BigDataStructureMarker
        @Transient
        private Map<Object, Double> weights; //the W parameters of the model

        /**
         * Right-side limits of the class on the ordinal regression line. 
         */
        @BigDataStructureMarker
        @Transient
        private Map<Object, Double> thitas; 
        
        @Override
        public void bigDataStructureInitializer(BigDataStructureFactory bdsf, MemoryConfiguration memoryConfiguration) {
            super.bigDataStructureInitializer(bdsf, memoryConfiguration);
            
            BigDataStructureFactory.MapType mapType = memoryConfiguration.getMapType();
            int LRUsize = memoryConfiguration.getLRUsize();
            
            weights = bdsf.getMap("weights", mapType, LRUsize);
            thitas = bdsf.getMap("thitas", mapType, LRUsize);
        }
        
        public Map<Object, Double> getWeights() {
            return weights;
        }

        public void setWeights(Map<Object, Double> weights) {
            this.weights = weights;
        }

        public Map<Object, Double> getThitas() {
            return thitas;
        }

        public void setThitas(Map<Object, Double> thitas) {
            this.thitas = thitas;
        }
        
    } 

    
    public static class TrainingParameters extends BaseMLclassifier.TrainingParameters {         
        private int totalIterations=100; 
        private double learningRate=0.1;
        
        public int getTotalIterations() {
            return totalIterations;
        }

        public void setTotalIterations(int totalIterations) {
            this.totalIterations = totalIterations;
        }

        public double getLearningRate() {
            return learningRate;
        }

        public void setLearningRate(double learningRate) {
            this.learningRate = learningRate;
        }

    } 
    
    
    public static class ValidationMetrics extends BaseMLclassifier.ValidationMetrics {
        private double SSE = 0.0; 
        private double CountRSquare = 0.0; // http://www.ats.ucla.edu/stat/mult_pkg/faq/general/Psuedo_RSquareds.htm
        
        public double getSSE() {
            return SSE;
        }

        public void setSSE(double SSE) {
            this.SSE = SSE;
        }

        public double getCountRSquare() {
            return CountRSquare;
        }

        public void setCountRSquare(double CountRSquare) {
            this.CountRSquare = CountRSquare;
        }
        
    }
    
    public OrdinalRegression(String dbName) {
        super(dbName, OrdinalRegression.ModelParameters.class, OrdinalRegression.TrainingParameters.class, OrdinalRegression.ValidationMetrics.class, new OrdinalRegressionValidation());
    }
    
    @Override
    public final String shortMethodName() {
        return SHORT_METHOD_NAME;
    }
    
    @Override
    protected void predictDataset(Dataset newData) { 
        ModelParameters modelParameters = knowledgeBase.getModelParameters();
        
        Map<Object, Double> weights = modelParameters.getWeights();
        Map<Object, Double> thitas = modelParameters.getThitas();
        
        
        //mapping between the thita and the exact previous thita value
        Map<Object, Object> previousThitaMapping = getPreviousThitaMappings();
        
        for(Record r : newData) {
            AssociativeArray predictionProbabilities = hypothesisFunction(r.getX(), previousThitaMapping, weights, thitas);
            
            Object theClass=getSelectedClassFromClassScores(predictionProbabilities);
            
            r.setYPredicted(theClass);
            r.setYPredictedProbabilities(predictionProbabilities);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void estimateModelParameters(Dataset trainingData) {
        String tmpPrefix=StorageConfiguration.getTmpPrefix();
        
        int n = trainingData.size();
        int d = trainingData.getColumnSize();//no constant, thresholds can be seen as constants
        
        ModelParameters modelParameters = knowledgeBase.getModelParameters();
        TrainingParameters trainingParameters = knowledgeBase.getTrainingParameters();
        
        //initialization
        modelParameters.setN(n);
        modelParameters.setD(d);
        
        Map<Object, Double> weights = modelParameters.getWeights();
        Map<Object, Double> thitas = modelParameters.getThitas();
        
        //add classes in a sorted way (ordinal ascending order)
        Set<Object> sortedClasses = new TreeSet<>();
        for(Record r : trainingData) {
            Object theClass=r.getY();
            
            sortedClasses.add(theClass); 
        }
        Set<Object> classesSet = modelParameters.getClasses();
        classesSet.addAll(sortedClasses);
        
        int c = classesSet.size();
        modelParameters.setC(c);
        
        //we initialize the weights and thitas to zero
        for(Record r : trainingData) {
            for(Object feature : r.getX().keySet()) {
                weights.put(feature, 0.0);
            }
            thitas.put(r.getY(), 0.0);
        }
        
        Object finalClass = null;
        for(Object theClass : classesSet) {
            finalClass=theClass;
        }
        thitas.put(finalClass, Double.POSITIVE_INFINITY);
        
        
        //mapping between the thita and the exact previous thita value
        Map<Object, Object> previousThitaMapping = getPreviousThitaMappings();
        
        double minError = Double.POSITIVE_INFINITY;
        
        double learningRate = trainingParameters.getLearningRate();
        int totalIterations = trainingParameters.getTotalIterations();
        BigDataStructureFactory bdsf = knowledgeBase.getBdsf();
        for(int iteration=0;iteration<totalIterations;++iteration) {
            
            if(GeneralConfiguration.DEBUG) {
                System.out.println("Iteration "+iteration);
            }
            
            Map<Object, Double> newThitas = bdsf.getMap(tmpPrefix+"newThitas", knowledgeBase.getMemoryConfiguration().getMapType(), knowledgeBase.getMemoryConfiguration().getLRUsize());
            
            Map<Object, Double> newWeights = bdsf.getMap(tmpPrefix+"newWeights", knowledgeBase.getMemoryConfiguration().getMapType(), knowledgeBase.getMemoryConfiguration().getLRUsize());
            
            newThitas.putAll(thitas);
            newWeights.putAll(weights);
            batchGradientDescent(trainingData, previousThitaMapping, newWeights, newThitas, learningRate);
            
            double newError = calculateError(trainingData, previousThitaMapping, newWeights, newThitas);
            
            //bold driver
            if(newError>minError) {
                learningRate/=2.0;
            }
            else {
                learningRate*=1.05;
                minError=newError;
                
                //keep the new weights
                weights.clear();
                weights.putAll(newWeights);
                
                //keep the new thitas
                thitas.clear();
                thitas.putAll(newThitas);
            }
            
            //Drop the temporary Collections
            bdsf.dropTable(tmpPrefix+"newWeights", newWeights);
            bdsf.dropTable(tmpPrefix+"newThitas", newThitas);
        }
    }
    
    
    @Override
    protected ValidationMetrics validateModel(Dataset validationData) {
        ValidationMetrics validationMetrics = super.validateModel(validationData);
        
        
        //mapping between the thita and the exact previous thita value
        Map<Object, Object> previousThitaMapping = getPreviousThitaMappings();
        
        validationMetrics.setCountRSquare(validationMetrics.getAccuracy()); //CountRSquare is equal to Accuracy
        
        double SSE = calculateError(validationData, previousThitaMapping, knowledgeBase.getModelParameters().getWeights(), knowledgeBase.getModelParameters().getThitas());
        validationMetrics.setSSE(SSE); //TODO: rename SSE to cost
        
        return validationMetrics;
    }

    private void batchGradientDescent(Dataset trainingData, Map<Object, Object> previousThitaMapping, Map<Object, Double> newWeights, Map<Object, Double> newThitas, double learningRate) {
        //NOTE! This is not the stochastic gradient descent. It is the batch gradient descent optimized for speed (despite it looks more than the stochastic). 
        //Despite the fact that the loops are inverse, the function still changes the values of Thitas at the end of the function. We use the previous thitas 
        //to estimate the costs and only at the end we update the new thitas.
        ModelParameters modelParameters = knowledgeBase.getModelParameters();

        double multiplier = -learningRate/modelParameters.getN(); 
        Map<Object, Double> weights = modelParameters.getWeights();
        Map<Object, Double> thitas = modelParameters.getThitas();
        
        for(Record r : trainingData) {
            Object rClass = r.getY();
            Object rPreviousClass = previousThitaMapping.get(rClass);
            
            //mind the fact that we use the previous weights and thitas to estimate the new ones! this is because the thitas must be updated simultaniously
            
            //first calculate the commonly used dot product between weights and x
            double xTw = xTw(r.getX(), weights);
            
            double gOfCurrent = g(xTw-thitas.get(rClass));
            double gOfPrevious = 0.0;
            if(rPreviousClass!=null) {
                gOfPrevious = g(thitas.get(rPreviousClass)-xTw);
            }
                    
            
            
            //update weights                
            for(Map.Entry<Object, Object> entry : r.getX().entrySet()) {
                Object column = entry.getKey();
                Double xij = Dataset.toDouble(entry.getValue());
                
                newWeights.put(column, newWeights.get(column)+multiplier*xij*(gOfCurrent-gOfPrevious));
            }
            
            
            //update thitas
            newThitas.put(rClass, newThitas.get(rClass)+multiplier*(-gOfCurrent));
            if(rPreviousClass!=null) {
                newThitas.put(rPreviousClass, newThitas.get(rPreviousClass)+multiplier*gOfPrevious);
            }
        }
    }
    
    private AssociativeArray hypothesisFunction(AssociativeArray x, Map<Object, Object> previousThitaMapping, Map<Object, Double> weights, Map<Object, Double> thitas) {
        AssociativeArray probabilities = new AssociativeArray();
    
            
        //first calculate the commonly used dot product between weights and x
        double xTw = xTw(x, weights);
        
        Set<Object> classesSet = knowledgeBase.getModelParameters().getClasses();
        
        for(Object theClass : classesSet) {
            Object previousClass = previousThitaMapping.get(theClass);
            
            if(previousClass!=null) {
                probabilities.put(theClass, g(thitas.get(theClass)-xTw) - g(thitas.get(previousClass)-xTw) );
            }
            else {
                probabilities.put(theClass, g(thitas.get(theClass)-xTw) );
            }
            
            
        }
        
        return probabilities;
    }
    
    private double calculateError(Dataset trainingData, Map<Object, Object> previousThitaMapping, Map<Object, Double> weights, Map<Object, Double> thitas) {
        double error=0.0;
        
        for(Record r : trainingData) {
            double xTw = xTw(r.getX(), weights);
            
            Object theClass = r.getY();
            Object previousClass = previousThitaMapping.get(theClass);
            
            
            if(previousClass!=null) {
                error += h(thitas.get(previousClass)-xTw);
            }
            
            error += h(xTw-thitas.get(theClass));
        }
        
        return error/knowledgeBase.getModelParameters().getN();
    }
    
    private double h(double z) {
        if(z>30) {
            return z;
        }
        else if(z<-30) {
            return 0.0;
        }
        return Math.log(1.0+Math.exp(z));
    }
    
    private double g(double z) {
        if(z>30) {
            return 1.0;
        }
        else if(z<-30) {
            return 0.0;
        }
        return 1.0/(1.0+Math.exp(-z));
    }
    
    private double xTw(AssociativeArray x, Map<Object, Double> weights) {
        double xTw = 0.0;
        for(Map.Entry<Object, Object> entry : x.entrySet()) {
            Double value = Dataset.toDouble(entry.getValue());
            if(value==null || value==0.0) {
                continue;
            }
            Object column = entry.getKey();
            Double w = weights.get(column);
            if(w==null) {
                continue; //unsupported feature
            }
            xTw += value*w;
        }
        
        return xTw;
    }
    
    private Map<Object, Object> getPreviousThitaMappings() {
        Map<Object, Object> previousThitaMapping = new HashMap<>();
        Object previousThita = null; //null = the left bound thita0 which has thita equal to -inf
        for(Object thita : knowledgeBase.getModelParameters().getClasses()) {
            previousThitaMapping.put(thita, previousThita);
            previousThita = thita;
        }
        
        return previousThitaMapping;
    }
}

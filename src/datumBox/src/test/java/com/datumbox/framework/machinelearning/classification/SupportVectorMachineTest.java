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

import com.datumbox.framework.machinelearning.classification.SupportVectorMachine;
import com.datumbox.common.dataobjects.Dataset;
import com.datumbox.common.dataobjects.Record;
import com.datumbox.common.utilities.RandomValue;
import com.datumbox.configuration.MemoryConfiguration;
import com.datumbox.framework.machinelearning.datatransformation.SimpleDummyVariableExtractor;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import libsvm.svm_parameter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Vasilis Vryniotis <bbriniotis at datumbox.com>
 */
public class SupportVectorMachineTest {
    
    public SupportVectorMachineTest() {
    }


    /**
     * Test of predict method, of class SupportVectorMachine.
     */
    @Test
    public void testPredict() {
        System.out.println("predict");
        
        /*
        Example from http://www.inf.u-szeged.hu/~ormandi/ai2/06-naiveBayes-example.pdf
        FeatureList: 
            - 0: red
            - 1: yellow
            - 2: sports
            - 3: suv
            - 4: domestic
            - 5: imported
            - c1: yes
            - c2: no
        */
        /*
        Dataset trainingData = new Dataset();
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 0.0, 1.0}, 1));
        
        Dataset validationData = new Dataset();
        validationData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 1.0, 0.0}, 0));
        */
        
        Dataset trainingData = new Dataset();
        trainingData.add(Record.newDataVector(new String[] {"red", "sports", "domestic"}, "yes"));
        trainingData.add(Record.newDataVector(new String[] {"red", "sports", "domestic"}, "no"));
        trainingData.add(Record.newDataVector(new String[] {"red", "sports", "domestic"}, "yes"));
        trainingData.add(Record.newDataVector(new String[] {"yellow", "sports", "domestic"}, "no"));
        trainingData.add(Record.newDataVector(new String[] {"yellow", "sports", "imported"}, "yes"));
        trainingData.add(Record.newDataVector(new String[] {"yellow", "suv", "imported"}, "no"));
        trainingData.add(Record.newDataVector(new String[] {"yellow", "suv", "imported"}, "yes"));
        trainingData.add(Record.newDataVector(new String[] {"yellow", "suv", "domestic"}, "no"));
        trainingData.add(Record.newDataVector(new String[] {"red", "suv", "imported"}, "no"));
        trainingData.add(Record.newDataVector(new String[] {"red", "sports", "imported"}, "yes"));
        
        Dataset validationData = new Dataset();
        validationData.add(Record.newDataVector(new String[] {"red", "suv", "domestic"}, "no"));
        
        MemoryConfiguration memoryConfiguration = new MemoryConfiguration();
        
        String dbName = "JUnitClassifier";
        
        SimpleDummyVariableExtractor df = new SimpleDummyVariableExtractor(dbName);
        df.initializeTrainingConfiguration(memoryConfiguration, df.getEmptyTrainingParametersObject());
        df.transform(trainingData, true);
        df.normalize(trainingData);
        df.transform(validationData, false);
        df.normalize(validationData);
        
        SupportVectorMachine instance = new SupportVectorMachine(dbName);
        
        SupportVectorMachine.TrainingParameters param = instance.getEmptyTrainingParametersObject();
        param.getSvmParameter().kernel_type = svm_parameter.RBF;
        instance.initializeTrainingConfiguration(memoryConfiguration, param);
        instance.train(trainingData, validationData);
        
        
        instance = null;
        instance = new SupportVectorMachine(dbName);
        instance.setMemoryConfiguration(memoryConfiguration);
        instance.predict(validationData);
        
        
        df.denormalize(trainingData);
        df.denormalize(validationData);
        df.erase(true);

        
        Map<Integer, Object> expResult = new HashMap<>();
        Map<Integer, Object> result = new HashMap<>();
        for(Record r : validationData) {
            expResult.put(r.getId(), r.getY());
            result.put(r.getId(), r.getYPredicted());
        }
        assertEquals(expResult, result);
        
        instance.erase(true);
    }


    /**
     * Test of kFoldCrossValidation method, of class SupportVectorMachine.
     */
    @Test
    public void testKFoldCrossValidation() {
        System.out.println("kFoldCrossValidation");
        RandomValue.randomGenerator = new Random(42);
        int k = 5;
        
        Dataset trainingData = new Dataset();
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 1.0, 0.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 1.0, 0.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 0.0, 1.0}, 1));
        trainingData.add(Record.newDataVector(new Double[] {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, 0));
        trainingData.add(Record.newDataVector(new Double[] {1.0, 0.0, 1.0, 0.0, 0.0, 1.0}, 1));
        
        MemoryConfiguration memoryConfiguration = new MemoryConfiguration();
        
        
        String dbName = "JUnitClassifier";
        SupportVectorMachine instance = new SupportVectorMachine(dbName);
        
        SupportVectorMachine.TrainingParameters param = instance.getEmptyTrainingParametersObject();
        param.getSvmParameter().kernel_type = svm_parameter.LINEAR;
        instance.initializeTrainingConfiguration(memoryConfiguration, param);
        SupportVectorMachine.ValidationMetrics vm = instance.kFoldCrossValidation(trainingData, k);
        
        //double expResult = 0.5861704961704961;
        double expResult = 0.50;
        double result = vm.getMacroF1();
        assertTrue(result>expResult); //The SVM implementation uses Math.Rand() internally, thus the accuraccy depends on the seed and can only roughly validated
        //assertEquals(expResult, result, TestConfiguration.DOUBLE_ACCURACY_HIGH);
        instance.erase(true);
    }


    
}

package org.nd4j.autodiff.gradcheck;

import lombok.extern.slf4j.Slf4j;
import org.nd4j.autodiff.tensorgrad.TensorGrad;
import org.nd4j.autodiff.tensorgrad.TensorGradGraph;
import org.nd4j.autodiff.tensorgrad.impl.TensorGradFunction;
import org.nd4j.autodiff.tensorgrad.impl.TensorGradVariable;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.buffer.util.DataTypeUtil;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.util.ArrayUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gradient check utility
 *
 * @author Adam Gibson
 */
@Slf4j
public class GradCheckUtil {


    /**
     *
     * @param function
     * @param epsilon
     * @param maxRelError
     * @param print
     * @param inputParameters
     * @return
     */
    public static boolean checkGradients(
                                         TensorGradVariable function,
                                         TensorGradVariable wrt,
                                         double epsilon,
                                         double maxRelError,
                                         boolean print,
                                         Map<String,INDArray> inputParameters) {
        //Basic sanity checks on input:
        if (epsilon <= 0.0 || epsilon > 0.1)
            throw new IllegalArgumentException("Invalid epsilon: expect epsilon in range (0,0.1], usually 1e-4 or so");
        if (maxRelError <= 0.0 || maxRelError > 0.25)
            throw new IllegalArgumentException("Invalid maxRelativeError: " + maxRelError);

        DataBuffer.Type dataType = DataTypeUtil.getDtypeFromContext();
        if (dataType != DataBuffer.Type.DOUBLE) {
            throw new IllegalStateException("Cannot perform gradient check: Datatype is not set to double precision ("
                    + "is: " + dataType + "). Double precision must be used for gradient checks. Set "
                    + "DataTypeUtil.setDTypeForContext(DataBuffer.Type.DOUBLE); before using GradientCheckUtil");
        }

        /**
         * Need to pass in the exact gradient.
         * This is obtained from executing a subgraph
         * with just the gradient part to get the exact values.
         * You then run the comparison vs the approximation from that.
         *
         * To obtain the comparison/computing the values,  use the below routine
         */


        TensorGrad tensorGrad = function.getTensorGrad();
        //get just the subgraph for the graph
        TensorGradGraph gradGraph = new TensorGradGraph();
        tensorGrad.graph().setGraphApply(gradGraph);
        //set the graph back to normal
        tensorGrad.grad(function,wrt);
        tensorGrad.graph().setGraphApply(null);
        TensorGrad opExec = TensorGrad.create(tensorGrad,gradGraph);

        INDArray[] eval = opExec.eval(inputParameters);
        int totalNFailures = 0;
        double maxError = 0.0;

        for(Map.Entry<String,INDArray> entry : inputParameters.entrySet()) {
            int nParams = entry.getValue().length();
            INDArray params = entry.getValue().dup();
            for (int i = 0; i < nParams; i++) {
                INDArray zeros = Nd4j.create(nParams);
                zeros.putScalar(i,epsilon / 2.0);

                //(w+epsilon): Do forward pass and score
                double origValue = params.getDouble(i);
                params.putScalar(i, origValue + epsilon);
                Map<String, INDArray> evalParams = new HashMap<>();
                for (Map.Entry<String, INDArray> entry2 : inputParameters.entrySet()) {
                    if (!entry2.getKey().equals(entry.getKey())) {
                        evalParams.put(entry2.getKey(), entry2.getValue());
                    } else {
                        evalParams.put(entry.getKey(), params);
                    }
                }

                /**
                 * Need to figure out how I want to extract
                 * parameters for computing the delta..
                 *
                 */
                INDArray[] plusParams = tensorGrad.eval(evalParams);


                INDArray[] minusParams = tensorGrad.eval(evalParams);


                /**
                 * Difference between new params and old
                 */
                INDArray[] newDifferences = new INDArray[minusParams.length];
                for (int j = 0; j < newDifferences.length; j++) {
                    newDifferences[i] = plusParams[i].subi(minusParams[i]).divi(epsilon);
                }

                double diff = plusParams[plusParams.length - 1].sumNumber().doubleValue() - minusParams[minusParams.length - 1].sumNumber().doubleValue();
                double eps = diff / epsilon;
                double correctVal = eval[eval.length - 1].sumNumber().doubleValue();
                double gradDiff = Math.abs(correctVal - eps);
                if(gradDiff > maxRelError)
                    totalNFailures++;
                if (print) {
                    int nPass = nParams - totalNFailures;
                    log.info("GradientCheckUtil.checkGradients(): " + nParams + " params checked, " + nPass + " passed, "
                            + totalNFailures + " failed. Largest relative error = " + maxError);
                }
            }
        }

        return totalNFailures == 0;
    }
}
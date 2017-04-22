package org.nd4j.autodiff;

import org.junit.Test;
import org.nd4j.autodiff.functions.Constant;
import org.nd4j.autodiff.functions.DifferentialFunction;
import org.nd4j.autodiff.functions.DifferentialFunctionFactory;
import org.nd4j.autodiff.functions.Variable;
import org.nd4j.autodiff.graph.api.Edge;
import org.nd4j.autodiff.graph.Graph;
import org.nd4j.autodiff.opstate.NDArrayInformation;
import org.nd4j.autodiff.opstate.NDArrayVertex;
import org.nd4j.autodiff.opstate.OpState;
import org.nd4j.autodiff.tensorgrad.TensorGrad;
import org.nd4j.autodiff.tensorgrad.impl.TensorGradVariable;
import org.nd4j.linalg.factory.Nd4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ArrayTestAbstractFactory
        extends AbstractFactoriesTest<ArrayField> {

    private static final double EQUAL_DELTA = 1e-12;

    public ArrayTestAbstractFactory() {
        super(EQUAL_DELTA);
    }

    @Override
    protected AbstractFactory<ArrayField> getFactory() {
        return new ArrayFactory();
    }


    @Test
    public void testAutoDiff() {
        Graph<NDArrayInformation,OpState> graph = new Graph<>();
        ArrayFactory arrayFactory = new ArrayFactory(graph);
        DifferentialFunctionFactory<ArrayField> arrayFieldDifferentialFunctionFactory = new DifferentialFunctionFactory<>(graph,arrayFactory);
        NDArrayInformation xInfo = NDArrayInformation.
                builder().
                shape(new int[]{1,1}).
                id("x").
                build();
        NDArrayInformation yInfo = NDArrayInformation.
                builder().
                shape(new int[]{1,1}).
                id("y").
                build();
        NDArrayVertex xVertex = new NDArrayVertex(0,xInfo);
        NDArrayVertex arrayVertex = new NDArrayVertex(1,yInfo);

        Variable<ArrayField> x = arrayFieldDifferentialFunctionFactory.var("x",new ArrayField(xVertex, graph));
        Variable<ArrayField> y = arrayFieldDifferentialFunctionFactory.var("y", new ArrayField(arrayVertex, graph));
        DifferentialFunction<ArrayField> h = x.mul(x).mul( arrayFieldDifferentialFunctionFactory.cos(x.mul(y) ).plus(y));
        System.out.println(h.diff(x).getValue().getOps());

    }

    @Test
    public void testVariables() {
        Graph<NDArrayInformation,OpState> graph = new Graph<>();
        ArrayFactory arrayFactory = new ArrayFactory(graph);
        DifferentialFunctionFactory<ArrayField> arrayFieldDifferentialFunctionFactory = new DifferentialFunctionFactory<>(graph,arrayFactory);
        NDArrayInformation xInfo = NDArrayInformation.
                builder().
                shape(new int[]{1,1}).
                id("x").
                build();
        NDArrayVertex xVertex = new NDArrayVertex(0,xInfo);
        Variable<ArrayField> x = arrayFieldDifferentialFunctionFactory.var("x",new ArrayField(xVertex, graph));
        DifferentialFunction<ArrayField> h = x.mul(x);
        System.out.println(h.diff(x).getValue(false).getClass());

    }


    @Test
    public void testPairWiseOp() throws Exception {
        Graph<NDArrayInformation,OpState> graph = new Graph<>();
        ArrayFactory arrayFactory = new ArrayFactory(graph);

        DifferentialFunctionFactory<ArrayField> arrayFieldDifferentialFunctionFactory = new DifferentialFunctionFactory<>(graph,arrayFactory);
        NDArrayInformation xInfo = NDArrayInformation.
                builder().
                shape(new int[]{1,1}).
                id("x").
                build();
        NDArrayVertex xVertex = new NDArrayVertex(0,xInfo);

        //2 * x
        Variable<ArrayField> x = arrayFieldDifferentialFunctionFactory.var("x",new ArrayField(xVertex, graph));
        DifferentialFunction<ArrayField> h = x.mul(x);
        System.out.println(h.getFormula(new ArrayList<>()));
        //x, x as the duplicate input and result are the vertices
        assertEquals(3,graph.numVertices());
        //x * x - edges for only 1 vertex and 1 duplicate
        assertEquals(2,graph.getEdges().size());
        //2 edges
        assertEquals(1,graph.getEdges().get(0).size());
        graph.print(new File(System.getProperty("java.io.tmpdir"),"graph.png"));

    }

    @Test
    public void testConstant() {
        Graph<NDArrayInformation,OpState> graph = new Graph<>();
        ArrayFactory arrayFactory = new ArrayFactory(graph);

        DifferentialFunctionFactory<ArrayField> arrayFieldDifferentialFunctionFactory = new DifferentialFunctionFactory<>(graph,arrayFactory);
        Constant<ArrayField> constant  = arrayFieldDifferentialFunctionFactory.zero();
        Constant<ArrayField> one = arrayFieldDifferentialFunctionFactory.one();
        assertEquals(2,graph.numVertices());
        DifferentialFunction<ArrayField> mul = one.mul(constant);
        assertEquals(1,graph.getEdges().size());
        assertEquals(1,graph.getVertexInDegree(2));

        Variable<ArrayField> variable = arrayFieldDifferentialFunctionFactory.var("x",constant.getValue());
        assertEquals(3,graph.numVertices());
        System.out.println(mul.diff(variable).getFormula(new ArrayList<>()));

    }


    @Test
    public void testGrad() throws Exception {
        TensorGrad tensorGrad = TensorGrad.create();
        TensorGradVariable var = tensorGrad.var("x", Nd4j.create(1));
        TensorGradVariable var2 = tensorGrad.var("y", Nd4j.create(1));

        TensorGradVariable xTimesX = var.mul(var);
        TensorGradVariable grad = tensorGrad.grad(xTimesX,var);
        System.out.println(tensorGrad.graph());
        System.out.println(grad.getFormula());
        tensorGrad.graph().print(new File("/tmp/graph.png"));

    }

    @Test
    public void testSingleTransformOp() throws Exception {
        Graph<NDArrayInformation,OpState> graph = new Graph<>();
        ArrayFactory arrayFactory = new ArrayFactory(graph);

        DifferentialFunctionFactory<ArrayField> arrayFieldDifferentialFunctionFactory = new DifferentialFunctionFactory<>(graph,arrayFactory);
        NDArrayInformation xInfo = NDArrayInformation.
                builder().
                shape(new int[]{1,1}).
                id("x").
                build();
        NDArrayVertex xVertex = new NDArrayVertex(0,xInfo);


        //2 * x
        Variable<ArrayField> x = arrayFieldDifferentialFunctionFactory.var("x",new ArrayField(xVertex, graph));

        Field<ArrayField> h = arrayFactory.abs(x.getValue());

        //x, x as the duplicate input and result are the vertices
        assertEquals(2,graph.numVertices());
        //x * x - edges for only 1 vertex and 1 duplicate
        assertEquals(1,graph.getEdges().size());
        //2 edges
        assertEquals(1,graph.getEdges().get(0).size());
        graph.print(new File(System.getProperty("java.io.tmpdir"),"graph.png"));

    }


    @Test
    public void testAutoDiffSimple() throws Exception {
        Graph<NDArrayInformation,OpState> graph = new Graph<>();
        ArrayFactory arrayFactory = new ArrayFactory(graph);

        DifferentialFunctionFactory<ArrayField> arrayFieldDifferentialFunctionFactory = new DifferentialFunctionFactory<>(graph,arrayFactory);
        NDArrayInformation xInfo = NDArrayInformation.
                builder().
                shape(new int[]{1,1}).
                id("x").
                build();
        NDArrayVertex xVertex = new NDArrayVertex(0,xInfo);

        //2 * x
        Variable<ArrayField> x = arrayFieldDifferentialFunctionFactory.var("x",new ArrayField(xVertex, graph));
        DifferentialFunction<ArrayField> h = x.mul(x);
        System.out.println(h.getFormula(new ArrayList<>()));
        //x and result are the vertices
        assertEquals(3,graph.numVertices());
        //x * x - edges for only 1 vertex
        assertEquals(2,graph.getEdges().size());
        //2 edges
        assertEquals(1,graph.getEdges().get(0).size());
        System.out.println("Pre graph " + graph);
        //the polynomial doesn't create edges (power,one,..)
        DifferentialFunction<ArrayField> dif = h.diff(x);
        System.out.println("Formula  " + dif.getFormula(new ArrayList<>()));
        assertEquals(4,graph.getEdges().get(0).size());
        //This accumulates the edges from both x * x and 2 * (x,1) ^ 1 (the derivative)
        System.out.println(graph.toString());
        dif.getValue();
        //getValue shouldn't change graph
        assertEquals(4,graph.getEdges().get(0).size());
        dif.getFormula(new ArrayList<>());
        //getFormula shouldn't change graph
        assertEquals(4,graph.getEdges().get(0).size());
        //should have polynomial edges from 2 to 4 and 2 to 5
        assertEquals(1,graph.getEdges().get(2).size());
        graph.print(new File(System.getProperty("java.io.tmpdir"),"graph.png"));
        for(List<Edge<OpState>> edges : graph.getEdges().values()) {
        }
    }


}
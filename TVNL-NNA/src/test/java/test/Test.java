package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import de.m_marvin.basicxml.XMLException;
import de.m_marvin.basicxml.XMLInputStream;
import de.m_marvin.basicxml.XMLOutputStream;
import de.m_marvin.basicxml.marshaling.XMLMarshaler;
import de.m_marvin.basicxml.marshaling.XMLMarshalingException;
import de.m_marvin.basicxml.marshaling.XMLUnmarshaler;
import de.m_marvin.unimat.impl.MatrixNd;
import tvnlnna.NodalMatrixStampException;
import tvnlnna.nodal.NodalElement;
import tvnlnna.nodal.NodalElementState;
import tvnlnna.nodal.NodalNetwork;
import tvnlnna.nodal.NodalNetwork.StampingContext.StampingMode;

public class Test {
	
	public static void main(String[] args) throws URISyntaxException, XMLMarshalingException, IOException, XMLException, NodalMatrixStampException {
		
//		String s = "f(x,y):=x*(x+y)+y*y;\n"
//				 + "g(A):=f(2.0,A);";
//		
//		System.out.println(s);
//		
////		MathParsingContext ctx = MathParsingContext.standard();
////		MathFunction f = MathFunction.parseInfix(s, ctx);
////		
////		System.out.println("solve for f(x,y) = f(2, 3)");
////		
////		ValueAndDerivative res1 = f.evaluateAndDerive("x", 2.0, 3.0);
////		ValueAndDerivative res2 = f.evaluateAndDerive("y", 2.0, 3.0);
////		
////		System.out.println("f(x) = " + res1.value());
////		System.out.println("∂f/∂x = " + res1.derivative());
////		System.out.println("∂f/∂y = " + res2.derivative());
////		
////		String s2 = "g(A):=f(2.0,A)";
////
////		System.out.println(s2);
////		
////		MathFunction f2 = MathFunction.parseInfix(s2, ctx);
////
////		System.out.println("solve for g(A) = g(3)");
////		
////		ValueAndDerivative res3 = f2.evaluateAndDerive("A", 3.0);
////		System.out.println("g(A) = " + res3.value());
////		System.out.println("∂f/∂A = " + res3.derivative());
//		
//		MathFunction f2 = MathFunction.parseInfixList(s, MathParsingContext.standard());
//
//		System.out.println("solve for g(A) = g(3)");
//		
//		ValueAndDerivative res3 = f2.evaluateAndDerive("A", 3.0);
//		System.out.println("g(A) = " + res3.value());
//		System.out.println("∂f/∂A = " + res3.derivative());
		
		
		File modelPath = new File(Test.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "../run/stdmodels");
		
		NodalElement voltageSource = loadModel(modelPath, "voltage_independent");
		NodalElement resistor = loadModel(modelPath, "resistor");
		NodalElement capacitor = loadModel(modelPath, "capacitor");
		
		NodalNetwork network = new NodalNetwork();
		
		{
			NodalElementState element = voltageSource.newInstance("source");
			element.setParameter("U", 5);
			element.setNode("p_A", "VDD");
			element.setNode("p_B", "GND");
			network.addElement(element);
		}
		
		{
			NodalElementState element = resistor.newInstance("load");
			element.setParameter("R", 4000);
			element.setNode("p_A", "VDD");
			element.setNode("p_B", "OUT");
			network.addElement(element);
		}

		{
			NodalElementState element = capacitor.newInstance("load");
			element.setParameter("C", 0.004700);
			element.setNode("p_A", "OUT");
			element.setNode("p_B", "GND");
			network.addElement(element);
		}
		
		network.setZeroNode("GND");
		
		System.out.println(	network.toString());	
		
		network.stampMatrices(StampingMode.FULL_MATRICES);
		
		System.out.println("A");
		System.out.println(network.getSystemMatrix_A());
		System.out.println(network.getSystemMatrix_A().determinant());
		System.out.println("E");
		System.out.println(network.getSystemMatrix_E());
		System.out.println(network.getSystemMatrix_E().determinant());
		System.out.println("x");
		System.out.println(network.getSystemMatrix_x());
		System.out.println("z");
		System.out.println(network.getSystemMatrix_z());
		
		
	}
	
	public static final XMLUnmarshaler LOADER = new XMLUnmarshaler(true, NodalElement.class);
	
	public static NodalElement loadModel(File modelPath, String name) throws FileNotFoundException, IOException, XMLException, XMLMarshalingException {
		return LOADER.unmarshall(new XMLInputStream(new FileInputStream(new File(modelPath, name + ".xml"))), NodalElement.class);
	}
	
}

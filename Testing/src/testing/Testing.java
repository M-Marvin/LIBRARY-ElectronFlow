package testing;

public class Testing {
	
	public static void main(String[] args) {
		
		float A_11 = 2; // Reziproke Leerlauf-Spannungsübersetzung
		float A_12 = 0; // Kurzschluss-Kernimpedanz vorwärts (reziproke Steilheit)
		float A_21 = 0; // Leerlauf Kernadmittanz vorwärts (Übertragungsleitwert)
		float A_22 = 0.5f; // Reziproke Kurzschluss-Stromübersetzung

		float U_2 = 100;
		float I_2 = 3;
		
		float U_1 = 0;
		float I_1 = 0;
		
		MatrixDf A = new MatrixDf(2, 2);
		A.setField(0, 0, A_11);
		A.setField(0, 1, A_21);
		A.setField(1, 0, A_12);
		A.setField(1, 1, A_22);
		
		MatrixDf P2 = new MatrixDf(1, 2);
		P2.setField(0, 0, U_2);
		P2.setField(0, 1, I_2);
		
		MatrixDf P1 = A.mul(P2);
		
		U_1 = P1.getField(0, 0);
		I_1 = P1.getField(0, 1);
		
		
		MatrixDf Z = new MatrixDf(2, 2);
		Z.setField(0, 0, A_11 / A_21);
		Z.setField(0, 1, 1 / A_21);
		Z.setField(1, 0, (A_11 * A_22 - A_12 * A_21) / A_21);
		Z.setField(1, 1, A_22 / A_21);
		

		System.out.println("test");
		System.out.println(String.format("U1: %f\nI1: %f\nU2: %f\nI2: %f", U_1, I_1, U_2, I_2));
		
		
		I_1 = 2;
		I_2 = 4;
		
		P2.setField(0, 0, I_1);
		P2.setField(0, 1, I_2);
		
		P1 = Z.mul(P2);

		U_1 = P1.getField(0, 0);
		U_2 = P1.getField(0, 1);
		
		System.out.println("test");
		System.out.println(String.format("U1: %f\nI1: %f\nU2: %f\nI2: %f", U_1, I_1, U_2, I_2));
		
		
	}
	
}

package prMarkovChains;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DetectorAnomalias {
	
	// Mismos valores que en la clase Matriz:
	private static int rangoVentana = 4;
	private static int numIntervalo = 10;
	
	// Lee la matriz de probabilidades
	public static double[][] leerMatrizP() throws java.io.FileNotFoundException{
	    Scanner sc = new Scanner(new File("matrizP.txt"));
        double[][] mp = new double[numIntervalo][numIntervalo];

        int fil = 0;
        while(sc.hasNextLine()) {
	        String[] numsFila = sc.nextLine().split("\t");
	        int col = 0;
	        for(int i = 0; i < numIntervalo; i++) {
	        	mp[fil][col] = Double.parseDouble(numsFila[i]);
	        	col++;
	        }
	        fil++;
        }
        sc.close();	
        return mp;
	}
	
	// Lee el punto de corte
	public static double leerPuntoCorte() throws java.io.FileNotFoundException{
	    Scanner sc = new Scanner(new File("puntoCorte.txt"));
	    double pc = Double.parseDouble(sc.nextLine());
        sc.close();
        return pc;
	}

	// Lee el mayor estado obtenido durante el entrenamiento
	private static int leerMayor() throws java.io.FileNotFoundException{
	    Scanner sc = new Scanner(new File("mayor.txt"));
	    int mayor = Integer.parseInt(sc.nextLine());
        sc.close();
        return mayor;
	}

	// Lee el menor estado obtenido durante el entrenamiento
	private static int leerMenor() throws java.io.FileNotFoundException{
	    Scanner sc = new Scanner(new File("menor.txt"));
	    int menor = Integer.parseInt(sc.nextLine());
        sc.close();
        return menor;
	}

	public synchronized static String detectarAnomaliaPr2(List<Integer> nums)
												throws FileNotFoundException {
		double pc = leerPuntoCorte();
		int menor = leerMenor();
		int mayor = leerMayor();
		double[][] matrizProb = leerMatrizP();
		List<Integer> estados = new ArrayList<>();
		boolean anomalia = false;
		String s = "";
		for(int i = 0; i < nums.size(); i++) {
			estados.add(calcEstado(menor, mayor, nums.get(i)));
			if(estados.size() == rangoVentana) {
				double p3t = 1.0;
				for(int j = 1; j < estados.size(); j++) {
					p3t *= matrizProb[estados.get(j-1)][estados.get(j)];
				}			 // tolerancia
				if(p3t <= pc + 0.0000001) {
					anomalia = true;
					s += "[" + nums.get(i-3) + " - " + nums.get(i-2) + " - "
							+ nums.get(i-1) + " - " + nums.get(i) + "]";
				}
				estados.remove(0);
			}
		}
		if(anomalia) {
			return "Se ha producido una anomalia: " + s;
		}else{
			return "No se han detectado anomalias";
		}
	}
	
	static synchronized int calcEstado(int menor, int mayor, int n) {
		float div = (float)(n-menor)/(float)(mayor-menor);
		int aux = Math.round((numIntervalo-1)*div);
		if(aux < 0) {	// Por si llegan valores menores que el menor
			aux = 0;
		}else if(aux > (numIntervalo-1)) {	// Por si llegan valores mayores que el mayor
			aux = numIntervalo-1;
		}
		return aux;
	}
	
}



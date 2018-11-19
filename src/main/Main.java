package main;

import java.nio.file.Paths;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import ui.graph.SimpleGraph;

public class Main {
	
	private Main() {	}

	public static void main(String[] args) throws Exception {
		
		int instanceId = getRequestedInstanceId(args);
		
		Path filePath = Paths.get("data/example"+instanceId+".txt");
		if(!filePath.toFile().exists()) {
			System.out.println("The requested instance does not exist!");
			System.exit(0);
		}
		
		List<String> entries = Files.readAllLines(filePath);
		
		
		double[][] cities = new double[entries.size()][2];
		try {
			for(int i=0; i<cities.length;i++) {
				String[] data = entries.get(i).split(",");
				double x = Double.parseDouble(data[0]);
				double y = Double.parseDouble(data[1]);
				cities[i][0] = x;
				cities[i][1] = y;
			}
		}
		catch(Exception ex) {
			System.out.println("Could not parse city locations. \n"
					+ "Example files with N cities should have N rows each containing the x and y coordinate of a city separated by a comma.");
			System.exit(0);
		}

		//Start visualization
		SimpleGraph graph = new SimpleGraph(20, 20);
		graph.setPointSize(8);
		for(double[] city:cities) {
			graph.addPoint(city[0],city[1],Color.GREEN);
		}
		graph.centralize();
		graph.display();
				
		//Utility
		Random random = new Random();
		
		//Initialize neurons
		double[][] neurons = new double[cities.length*5][2];
		
		//Run algorithm
		final int maxEpochCount = 2000;
		double learningRate = 0.75;
		int neighbourhoodSize = 25;
		
		visualizeKohonenChain(graph, neurons);
		
		double[] help = new double[2];
		for(int epoch=0; epoch<maxEpochCount;epoch++) {
			System.out.println("Epoch: " + epoch);
			learningRate = Math.max(learningRate*0.99,0.05);
			if(epoch%50==0)
				neighbourhoodSize = Math.max(0, neighbourhoodSize-1);
			
			for(int iteration = 0; iteration<cities.length; iteration++) {
				double[] city = cities[random.nextInt(cities.length)];
				help[0] = city[0]+random.nextGaussian()/(epoch+1);
				help[1] = city[1]+random.nextGaussian()/(epoch+1);
				int closestNeuronId = determineIdOfClosest(neurons, help);

				//Update closest neuron
				double[] neuron = neurons[closestNeuronId];
				double xShift = learningRate*(city[0]-neuron[0]);
				double yShift = learningRate*(city[1]-neuron[1]);
				neuron[0]+=xShift;
				neuron[1]+=yShift;

				//Update neighborhood
				for(int i=1; i<=neighbourhoodSize;i++) {
					//Update right neighbor
					double[] neighborR = neurons[(closestNeuronId+i)%neurons.length];
					double factorR1 = adjustedSigmoidal(distanceBetween(neighborR, neuron));
					double factorR2 = 1-adjustedSigmoidal(distanceBetween(neighborR, city));
					double factorR = (factorR1*0.75+factorR2*0.25);
					double xRShift = factorR*learningRate*(city[0]-neighborR[0]);
					double yRShift = factorR*learningRate*(city[1]-neighborR[1]);
					neighborR[0]+=xRShift;
					neighborR[1]+=yRShift;
					//Update left neighbor
					double[] neighborL = neurons[(closestNeuronId-i+neurons.length)%neurons.length];
					double factorL1 = adjustedSigmoidal(distanceBetween(neighborL, neuron));
					double factorL2 = 1-adjustedSigmoidal(distanceBetween(neighborL, city));
					double factorL = (factorL1*0.75+factorL2*0.25);
					double xLShift = factorL*learningRate*(city[0]-neighborL[0]);
					double yLShift = factorL*learningRate*(city[1]-neighborL[1]);
					neighborL[0]+=xLShift;
					neighborL[1]+=yLShift;
				}
			}

			if(epoch%25==0) {
				//Visualize solution
				visualizeKohonenChain(graph, neurons);
				
				//Slow down the algorithm so we can see what is happening
				Thread.sleep(100);
			}		
		}
		visualizeKohonenChain(graph, neurons);		
		
		//Decode path
		try {
			System.out.println("Decoding path...");
			int[] path = decodePath(cities, neurons);
			double pathDistance = calculatePathDistance(path,cities);
			System.out.println("Path: " + Arrays.toString(path));
			System.out.println("Path distance: " + pathDistance);
		}
		catch(Exception ex) {
			System.out.println("Error occured while decoding the achieved path. ");
		}
	}

	//====================================================================================================
	//Network training functions

	private static double adjustedSigmoidal(double x) {
		double s = Math.exp(x)/(Math.exp(x)+1);
		return (s-0.5)*2;
	}

	private static int determineIdOfClosest(double[][] positions, double[] goal) {
		Random random = new Random();
		int i = -1;
		double minDistance = Double.POSITIVE_INFINITY;
		for(int n=0; n<positions.length;n++) {
			double[] neuron = positions[n];
						
			double distance = distanceBetween(goal, neuron);
			if(minDistance>distance || (Math.abs(minDistance-distance)<1e-6 && random.nextBoolean())) {
				minDistance = distance;
				i = n;
			}
		}
		return i;
	}

	private static double distanceBetween(double[] city, double[] neuron) {
		return Math.sqrt(Math.pow(city[0]-neuron[0], 2)+Math.pow(city[1]-neuron[1], 2));
	}
	//====================================================================================================
	//Path processing functions
	
	private static double calculatePathDistance(int[] path, double[][] cities) {
		double totalDistance = 0;
		
		for(int i=0; i<path.length; i++) {
			int start = i;
			int end = (i+1)%path.length;
			totalDistance+=distanceBetween(cities[start], cities[end]);
		}
		
		return totalDistance;
	}

	private static int[] decodePath(double[][] cities, double[][] neurons) {
		int[] path = new int[cities.length];
		int p = 0;
		double d = 2.5e-4;
		
		do {
			p = 0;
			for(double[] neuron:neurons) {
				if(p==path.length)
					break;
				int cityId = determineIdOfClosest(cities, neuron);
				double distance = distanceBetween(cities[cityId], neuron);
				if(distance<d) {
					if(p==0 || path[p-1]!=cityId) {
						path[p++] = cityId;
					}
				}
			}
			d+=1e-5;
		}while(p<path.length);
		return path;
	}

	//====================================================================================================
	//Visualization utility function

	private static void visualizeKohonenChain(SimpleGraph graph, double[][] neurons) {
		graph.removeAllShapes();
		for(int i=0; i<neurons.length; i++) {
			double[] n1 = neurons[i];
			double[] n2 = neurons[(i+1)%neurons.length];
			graph.addShape(new SimpleGraph.Line(n1, n2, Color.RED));
		}
		graph.repaint();
	}

	
	//====================================================================================================
	//Input arguments processing
	
	private static int getRequestedInstanceId(String[] args) {
		if(args.length==0) {
			System.out.println("Please provide the instance identifier. \n" + 
					"Valid inputs are positive integer numbers. ");
			System.exit(0);
		}
		
		int instanceId = -1;
		try {
			instanceId = Integer.parseInt(args[0]);
		}
		catch(Exception ex) {
			System.out.println("Please provide a valid instance identifier. \n"+
					"Valid inputs are positive integer numbers. \n"+
					args[0]+" is not a valid input!");
			System.exit(0);
		}
		
		if(instanceId<=0) {
			System.out.println("The instance identifier needs to be a positive integer number.");
			System.exit(0);
		}
		return instanceId;
	}

}

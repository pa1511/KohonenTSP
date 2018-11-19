# KohonenTSP
Solving the TSP problem using a Kohonen NN


This project is visual demonstration of using a Kohonen network to solve traveling salesman problems. 

## Running examples
In order to use the visual demo there is a KohonenTSP.jar file in the compiled folder. 
Place that .jar file next to the data folder. 
You can then run examples with: 
```
java -jar KohonenTSP.jar <NUMBER_OF_EXAMPLE>
```
for example: 
```
java -jar KohonenTSP.jar 1 
```
will run the first example. 

When you run an example a graph will appear on the screan and show a chain Kohonen network solving the TSP problem. 
There are 5 examples in the data folder. The next graphs show one solution per instance.

Example 1

![Example1](/images/instance1.gif)

Example 2

![Example2](/images/instance2.gif)

Example 3

![Example3](/images/instance3.gif)

Example 4

![Example4](/images/instance4.gif)

Example 5

![Example5](/images/instance5.gif)

## Adding your own examples
If you wish to add your own examples for the network to solve add them to the data folder. 
The file name should be: 
```
example<NUMBER_OF_EXAMPLE>.txt
```
The example file should consist of *N* rows if there are *N* cities in the example. 
Each row should contain the *X* and *Y* coordinate of the city separated by a comma.
Example of a row in the file:
```
1.2,3.75 
```


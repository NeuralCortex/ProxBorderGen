# ProxBorderGen 1.1.0

![Application Screenshot](https://github.com/NeuralCortex/ProxBorderGen/blob/main/images/app.png)

## Overview

ProxBorderGen is a Java-based application that generates random geographic coordinates in WGS84 format within the boundaries of a specified state or country. It utilizes multithreading to calculate bearings toward the boundary line. The generated data can be exported as a CSV file.

## Requirements

- An active internet connection is required.
- The application uses a corrected version of the JXMapViewer2 library, available at [NeuralCortex/JXMapViewer2](https://github.com/NeuralCortex/JXMapViewer2), as the original library is no longer functional.
- Java Runtime Environment (JRE) or Java Development Kit (JDK) version 24 is required.

## Workflow

1. Right-click on the map to select a country of your choice.
2. Choose a state or country from the available options.
3. Specify the bearing with a percentage deviation.
4. Generate test data based on the defined width and quantity.
5. Export the data as a semicolon-separated CSV file.

## Technologies Used

- **IDE**: [Apache NetBeans 27](https://netbeans.apache.org/)
- **Java SDK**: [Java 24](https://www.oracle.com/java/technologies/downloads/#jdk24-windows)
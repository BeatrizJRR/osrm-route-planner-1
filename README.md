# OSRM Route Planner

A simple desktop route planner that:

- Calculates routes using OSRM
- Geocodes user input using Nominatim
- Finds POIs along the route using Overpass

The UI is a JavaFX application using the `mapjfx` library to display maps and markers.

## Prerequisites

- Java 17 (JDK) installed
- Apache Maven

Verify with:

```powershell
java -version
mvn -v
```

## Build & Run

From the project root run:

```powershell
mvn clean javafx:run
```

Or build without running:

```powershell
mvn clean package
```

## Quick usage

- Start the app, set an origin (search or click on the map), add waypoints and click "Calcular Rota".
- Use the POI controls on the left panel to search for points of interest along the calculated route.

That's it — run `mvn clean javafx:run` and you should see the app window.

# Welcome to the FedX Repository

FedX is a practical framework for transparent access to Linked Data sources through a federation. 
It incorporates new sophisticated optimization techniques combined with effective variants of existing
techniques and is thus a highly scalable solution for practical federated query processing.


## Core Features

* *Virtual Integration* of heterogeneous Linked Data sources (e.g. as SPARQL endpoints)
* Transparent access to data sources through a federation
* Efficient query processing in federated environments
* On-demand federation setup at query time
* Fast and effective query execution due to new optimization techniques for federated setups
* Practical applicability & easy integration as a RDF4J SAIL
* Comprehensive CLI for federated query processing from the command line

## Documentation

See *build/doc/FedX_Documentation.html* for detailed information

## Development

### Build

The FedX repository uses Gradle as a build system:

```
./gradlew build
./gradlew eclipse
./gradlew tests
./gradlew testNativeStore testSparqlRepository testRemoteRepository
```
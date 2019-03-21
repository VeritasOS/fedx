Changelog

FedX 6.0.0 SNAPSHOT
 * basic support for values clause
 * support for dynamic federations
   - use FedxDataset to define the endpoints for a given query execution
 * provide hash join operator as an alternative for bound joins
   - note: not yet active
 * upgrade 3rd party components
   (RDF4J, Apache Commons, Jackson, Guava)
 * improvements to shutdown behavior
 * revising of logging infrastructure
   - use SLF4J as logging API
   - make logging backend optional (adapters via SLF4j)
   - migrate (optional) logging backend to log4j 2
   - redefine logging behavior for CLI
 * Technical Improvements
   - use Gradle 5 Build Environment
   - migrate unit tests to junit 5
   - test infrastructure for simulating errornous environments
   - intgegrate static code analysis using spotbugs

FedX 5.1.0 (2018-06-18)
 * Switch license to Apache License 2.0
 * Build using 3rd party dependencies from Maven central
 * Upgrade 3rd Party dependencies
 * Definition of pom.xml file
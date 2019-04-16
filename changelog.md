Changelog

FedX 6.0.0 SNAPSHOT

Note: this release is not fully backwards compatible to previous releases. 
See upgrade notes for details.

 * basic support for values clause
 * support for dynamic federations
   - use FedxDataset to define the endpoints for a given query execution
 * provide hash join operator as an alternative for bound joins
   - note: not yet active
 * improvements to shutdown behavior
 * refinement of query timeout handling
   - properly handle globally configured max execution time
   - support setting execution time per query
 * support use of FedX in RDF4J workbench (incl. documentation)
 * support to use resolvable endpoints
 * upgrade 3rd party components
   (RDF4J, Apache Commons, Jackson, Guava)
 * revising of logging infrastructure
   - use SLF4J as logging API
   - make logging backend optional (adapters via SLF4j)
   - migrate (optional) logging backend to log4j 2
   - redefine logging behavior for CLI
 * Improvements to optimizers (LIMIT, FILTER)
 * Improved overall robustness and fault tolerance
 * Documentation migrated to https://github.com/VeritasOS/fedx/wiki
 * Technical Improvements
   - use Gradle 5 Build Environment
   - migrate unit tests to junit 5
   - test infrastructure for simulating errornous environments
   - integrate static code analysis using spotbugs


FedX 5.1.0 (2018-06-18)
 * Switch license to Apache License 2.0
 * Build using 3rd party dependencies from Maven central
 * Upgrade 3rd Party dependencies
 * Definition of pom.xml file
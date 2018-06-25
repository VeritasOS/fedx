Changelog

FedX 2.0 (2012-07-04)
 * Upgrade to Sesame 2.6.5
 * Monitoring facilities (query log, query plan log, number of remote requests)
 * Support for remote repositories as endpoints
 * Optimization improvements: single source queries, scoping of variables (DISTINCT)
 * Improved: join ordering rules
 * Updated documentation
 * Improve: public interface methods
 * Fix: source selection issues
 * Fix: error handling
 
FedX 1.1 (2011-11-22)
 * Migration to Sesame 2.6.1
 * SPARQL 1.1 support
 * Option to specify commonly used PREFIX declarations in a file
 * PREFIX Handling in CLI
 * Documentation
 * Error handling: all subqueries belonging to a query abort in case of errors
 * Fix: Join Order optimization can now deal with NUnion
 * Fix: Join processing where exclusive group has no free variables
 
FedX 1.0 (2011-07-13)
 * Migration to Sesame 2.4.0
 * Improved: Error Handling 

FedX 1.1 beta
 * Improved: ASK requests in source selection are sent in parallel
 * Cache now has a clear() functionality
 * Added: cli.sh
 
FedX 1.0 beta (2011-05-27)
 * initial release


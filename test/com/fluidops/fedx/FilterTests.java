package com.fluidops.fedx;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class FilterTests extends SPARQLBaseTest {


	@Test
	public void testSimpleFilter() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query01.rq", "/tests/filter/query01.srx", false);			
	}
	
	@Test
	public void testOrFilter() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query02.rq", "/tests/filter/query02.srx", false);			
	}
	
	@Test
	public void testAndFilter() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query03.rq", "/tests/filter/query03.srx", false);			
	}
	
	@Test
	public void testAndFilter2() throws Exception {
		/* test insertion of resource filter into query */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query04.rq", "/tests/filter/query04.srx", false);			
	}
	
	@Test
	public void testAndFilter3() throws Exception {
		/* test range filter with integers */
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl"));
		execute("/tests/filter/query05.rq", "/tests/filter/query05.srx", false);			
	}

	@Test
	public void testFilterPushing() throws Exception {
		prepareTest(Arrays.asList("/tests/data/data1.ttl", "/tests/data/data2.ttl", "/tests/data/data4.ttl"));
		execute("/tests/filter/query06.rq", "/tests/filter/query06.srx", false);
	}
}

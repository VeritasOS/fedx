/*
 * Copyright (C) 2018 Veritas Technologies LLC.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fluidops.fedx.endpoint;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryResolver;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fluidops.fedx.Config;
import com.fluidops.fedx.FedXFactory;
import com.fluidops.fedx.endpoint.provider.NativeRepositoryInformation;
import com.fluidops.fedx.endpoint.provider.NativeStoreProvider;
import com.fluidops.fedx.endpoint.provider.RemoteRepositoryProvider;
import com.fluidops.fedx.endpoint.provider.RemoteRepositoryRepositoryInformation;
import com.fluidops.fedx.endpoint.provider.RepositoryEndpointProvider;
import com.fluidops.fedx.endpoint.provider.RepositoryInformation;
import com.fluidops.fedx.endpoint.provider.ResolvableRepositoryInformation;
import com.fluidops.fedx.endpoint.provider.ResolvableRepositoryProvider;
import com.fluidops.fedx.endpoint.provider.SPARQLProvider;
import com.fluidops.fedx.endpoint.provider.SPARQLRepositoryInformation;
import com.fluidops.fedx.exception.FedXException;
import com.fluidops.fedx.exception.FedXRuntimeException;
import com.fluidops.fedx.util.FedXUtil;
import com.fluidops.fedx.util.Vocabulary;



/**
 * Utility class providing various methods to create Endpoints to be used as federation members.
 * 
 * @author Andreas Schwarte
 *
 */
public class EndpointFactory {

	private static final Logger logger = LoggerFactory.getLogger(EndpointFactory.class);
	
	/**
	 * Construct a SPARQL endpoint using the the provided information.
	 * 
	 * @param name
	 * 			a descriptive name, e.g. http://dbpedia
	 * @param endpoint
	 * 			the URL of the SPARQL endpoint, e.g. http://dbpedia.org/sparql
	 * 
	 * @return
	 * 		an initialized {@link EndpointBase} containing the repository
	 * 
	 * @throws Exception
	 */
	public static Endpoint loadSPARQLEndpoint(String name, String endpoint) throws FedXException {
		
		SPARQLProvider repProvider = new SPARQLProvider();
		return repProvider.loadEndpoint(new SPARQLRepositoryInformation(name, endpoint));
	}
	
	
	/**
	 * Construct a SPARQL endpoint using the the provided information and the host of the url as name.
	 * 
	 * @param endpoint
	 * 			the URL of the SPARQL endpoint, e.g. http://dbpedia.org/sparql
	 * 
	 * @return
	 * 		an initialized {@link EndpointBase} containing the repository
	 * 
	 * @throws FedXException
	 */
	public static Endpoint loadSPARQLEndpoint(String endpoint) throws FedXException {
		try {
			String id = new URL(endpoint).getHost();
			if (id.equals("localhost"))
				id = id + "_" + new URL(endpoint).getPort();
			return loadSPARQLEndpoint("http://"+id, endpoint);
		} catch (MalformedURLException e) {
			throw new FedXException("Malformed URL: " + endpoint);
		}
	}
	
	
	public static Endpoint loadRemoteRepository(String repositoryServer, String repositoryName) throws FedXException {
		RemoteRepositoryProvider repProvider = new RemoteRepositoryProvider();
		return repProvider.loadEndpoint(new RemoteRepositoryRepositoryInformation(repositoryServer, repositoryName));
	
	}
	
	/**
	 * Load a {@link ResolvableEndpoint}
	 * 
	 * <p>
	 * The federation must be initialized with a {@link RepositoryResolver} ( see
	 * {@link FedXFactory#withRepositoryResolver(RepositoryResolver)}) and this
	 * resolver must offer a Repository with the id provided by
	 * {@link Endpoint#getId()}
	 * </p>
	 * 
	 * <p>
	 * Note that the name is set to "http://" + repositoryId
	 * </p>
	 * 
	 * @param repositoryId the repository identifier
	 * @return the configured {@link Endpoint}
	 * @see ResolvableRepositoryProvider
	 * @see ResolvableRepositoryInformation
	 */
	public static Endpoint loadResolvableRepository(String repositoryId) {
		ResolvableRepositoryProvider repProvider = new ResolvableRepositoryProvider();
		return repProvider.loadEndpoint(new ResolvableRepositoryInformation(repositoryId));
	}

	/**
	 * Load an {@link EndpointBase} for a given (configured) Repository.
	 * <p>
	 * Note that {@link EndpointType} is set to {@link EndpointType#Other}
	 * </p>
	 * 
	 * <p>
	 * If the repository is already initialized, it is assumed that the lifecycle is
	 * managed externally. Otherwise, FedX will make sure to take care for the
	 * lifecycle of the repository, i.e. initialize and shutdown.
	 * </p>
	 * 
	 * @param id         the identifier, e.g. "myRepository"
	 * @param repository the constructed repository
	 * @return the initialized endpoint
	 * @throws FedXException
	 */
	public static Endpoint loadEndpoint(String id, Repository repository)
			throws FedXException {
		RepositoryEndpointProvider repProvider = new RepositoryEndpointProvider(repository);
		String name = "http://" + id;
		String location = "http://unknown";
		try {
			location = repository.getDataDir().getAbsolutePath();
		} catch (Exception e) {
			logger.debug("Failed to use data dir as location, using unknown instead: " + e.getMessage());
			logger.trace("Details:", e);
		}
		return repProvider.loadEndpoint(new RepositoryInformation(id, name, location, EndpointType.Other));
	}

	/**
	 * Construct a NativeStore endpoint using the provided information.
	 * 
	 * <p>
	 * If the repository location denotes an absolute path, the native store
	 * directory must already exist. If a relative path is used, the repository is
	 * created on the fly (if necessary).
	 * </p>
	 * 
	 * @param name     a descriptive name, e.g. http://dbpedia
	 * @param location the location of the data store, either absolute or relative
	 *                 in a "repositories" subfolder of {@link Config#getBaseDir()}
	 * 
	 * @return an initialized endpoint containing the repository
	 * 
	 * @throws Exception
	 */
	public static Endpoint loadNativeEndpoint(String name, String location) throws FedXException {
		
		NativeStoreProvider repProvider = new NativeStoreProvider();
		return repProvider.loadEndpoint(new NativeRepositoryInformation(name, location));
	}
	
	
	/**
	 * Construct a {@link NativeStore} endpoint using the provided information and
	 * the file location as name.
	 * 
	 * <p>
	 * If the repository location denotes an absolute path, the native store
	 * directory must already exist. If a relative path is used, the repository is
	 * created on the fly (if necessary).
	 * </p>
	 * 
	 * @param location the location of the data store, either absolute or relative
	 *                 in a "repositories" subfolder of {@link Config#getBaseDir()}
	 * 
	 * @return an initialized endpoint containing the repository
	 * 
	 * @throws Exception
	 */
	public static Endpoint loadNativeEndpoint(String location) throws FedXException {
		return loadNativeEndpoint("http://" + new File(location).getName(), location);
	}
	
	
	
	/**
	 * Utility function to load federation members from a data configuration file. A
	 * data configuration file provides information about federation members in form
	 * of ntriples. Currently the types NativeStore and SPARQLEndpoint are
	 * supported. For details please refer to the documentation in
	 * {@link NativeRepositoryInformation} and {@link SPARQLRepositoryInformation}.
	 * 
	 * @param dataConfig
	 * 
	 * @return a list of initialized endpoints, i.e. the federation members
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public static List<Endpoint> loadFederationMembers(File dataConfig) throws FedXException {
		
		if (!dataConfig.exists())
			throw new FedXRuntimeException("File does not exist: " + dataConfig.getAbsolutePath());
		
		Model graph = new TreeModel();
		RDFParser parser = Rio.createParser(RDFFormat.N3);
		RDFHandler handler = new DefaultRDFHandler(graph);
		parser.setRDFHandler(handler);
		try (FileReader fr = new FileReader(dataConfig)) {
			parser.parse(fr, Vocabulary.FEDX.NAMESPACE);
		} catch (Exception e) {
			throw new FedXException("Unable to parse dataconfig " + dataConfig + ":" + e.getMessage());
		} 
		
		List<Endpoint> res = new ArrayList<>();
		for (Statement st : graph.filter(null, Vocabulary.FEDX.STORE, null))
		{
			Endpoint e = loadEndpoint(graph, st.getSubject(), st.getObject());
			res.add(e);
		}
		
		return res;
	}
	
	
	private static Endpoint loadEndpoint(Model graph, Resource repNode, Value repType) throws FedXException {
		
		
		// NativeStore => RDF4J native store implementation
		if (repType.equals(FedXUtil.literal("NativeStore")))
		{
			NativeStoreProvider repProvider = new NativeStoreProvider();
			return repProvider.loadEndpoint(new NativeRepositoryInformation(graph, repNode));
		} 
		
		// SPARQL Repository => SPARQLRepository 
		else if (repType.equals(FedXUtil.literal("SPARQLEndpoint")))
		{
			SPARQLProvider repProvider = new SPARQLProvider();
			return repProvider.loadEndpoint(new SPARQLRepositoryInformation(graph, repNode));
		} 
		
		// Remote Repository
		else if (repType.equals(FedXUtil.literal("RemoteRepository")))
		{
			RemoteRepositoryProvider repProvider = new RemoteRepositoryProvider();
			return repProvider.loadEndpoint(new RemoteRepositoryRepositoryInformation(graph, repNode));
		} 
		
		// Resolvable Repository
		else if (repType.equals(FedXUtil.literal("ResolvableRepository"))) {
			ResolvableRepositoryProvider repProvider = new ResolvableRepositoryProvider();
			return repProvider.loadEndpoint(new ResolvableRepositoryInformation(graph, repNode));
		}

		// other generic type
		else if (repType.equals(FedXUtil.literal("Other")))
		{
			
			// TODO add reflection techniques to allow for flexibility
			throw new UnsupportedOperationException("Operation not yet supported for generic type.");
			
		}
		
		else {
			throw new FedXRuntimeException("Repository type not supported: " + repType.stringValue());
		}
		
		
	}
	
	
	
	/**
	 * Construct a unique id for the provided SPARQL Endpoint, e.g
	 * 
	 * http://dbpedia.org/ => %type%_dbpedia.org
	 * 
	 * @param endpointID
	 * @param type
	 * 			the repository type, e.g. native, sparql, etc
	 * 
	 * @return the ID for the SPARQL endpoint
	 */
	public static String getId(String endpointID, String type) {
		String id = endpointID.replace("http://", "");
		id = id.replace("/", "_");
		return type + "_" + id;
	}
	
	
	
	protected static class DefaultRDFHandler implements RDFHandler {

		protected final Model graph;
				
		public DefaultRDFHandler(Model graph)
		{
			super();
			this.graph = graph;
		}

		@Override
		public void endRDF() throws RDFHandlerException {
			; // no-op
		}

		@Override
		public void handleComment(String comment) throws RDFHandlerException {
			; // no-op			
		}

		@Override
		public void handleNamespace(String prefix, String uri)
				throws RDFHandlerException {
			; // no-op			
		}

		@Override
		public void handleStatement(Statement st) throws RDFHandlerException {
			graph.add(st);			
		}

		@Override
		public void startRDF() throws RDFHandlerException {
			; // no-op			
		}
	}
}

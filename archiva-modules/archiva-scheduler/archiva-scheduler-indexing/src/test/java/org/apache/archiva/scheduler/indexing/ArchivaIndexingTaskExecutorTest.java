package org.apache.archiva.scheduler.indexing;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import junit.framework.TestCase;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.utils.PathUtil;
import org.apache.archiva.indexer.ArchivaIndexingContext;
import org.apache.archiva.indexer.UnsupportedBaseContextException;
import org.apache.archiva.repository.*;
import org.apache.archiva.repository.features.ArtifactCleanupFeature;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.FlatSearchRequest;
import org.apache.maven.index.FlatSearchResponse;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.expr.StringSearchExpression;
import org.apache.maven.index.updater.DefaultIndexUpdater;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index_shaded.lucene.search.BooleanClause;
import org.apache.maven.index_shaded.lucene.search.BooleanQuery;
import org.apache.maven.index_shaded.lucene.search.IndexSearcher;
import org.apache.maven.index_shaded.lucene.search.TopDocs;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ArchivaIndexingTaskExecutorTest
 */
@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath*:/spring-context.xml" } )
public class ArchivaIndexingTaskExecutorTest
    extends TestCase
{
    @Inject
    private ArchivaIndexingTaskExecutor indexingExecutor;

    private BasicManagedRepository repositoryConfig;

    @Inject
    private NexusIndexer indexer;

    @Inject
    RepositoryRegistry repositoryRegistry;

    @Inject
    private IndexUpdater indexUpdater;

    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        Path baseDir = Paths.get(System.getProperty("basedir"), "target/test-classes").toAbsolutePath();
        repositoryConfig = new BasicManagedRepository( "test-repo", "Test Repository", baseDir);
        Path repoLocation = baseDir.resolve("test-repo" );
        repositoryConfig.setLocation(repoLocation.toUri() );
        repositoryConfig.setLayout( "default" );
        repositoryConfig.setScanned( true );
        repositoryConfig.addActiveReleaseScheme( ReleaseScheme.RELEASE );
        repositoryConfig.removeActiveReleaseScheme( ReleaseScheme.SNAPSHOT );
        repositoryRegistry.putRepository(repositoryConfig);
    }

    @After
    @Override
    public void tearDown()
        throws Exception
    {

        for ( IndexingContext indexingContext : indexer.getIndexingContexts().values() )
        {
            indexer.removeIndexingContext( indexingContext, true );
        }
        repositoryRegistry.destroy();
        /*
        removeIndexingContext with true cleanup files.
        // delete created index in the repository
        File indexDir = new File( repositoryConfig.getLocation(), ".indexer" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );

        indexDir = new File( repositoryConfig.getLocation(), ".index" );
        FileUtils.deleteDirectory( indexDir );
        assertFalse( indexDir.exists() );
        */
        super.tearDown();
    }

    protected IndexingContext getIndexingContext() throws UnsupportedBaseContextException {
        Repository repo = repositoryRegistry.getRepository(repositoryConfig.getId());
        assert repo != null;
        ArchivaIndexingContext ctx = repo.getIndexingContext();
        assert ctx != null;
        return ctx.getBaseContext(IndexingContext.class);
    }

    @Test
    public void testAddArtifactToIndex()
        throws Exception
    {
        Path basePath = PathUtil.getPathFromUri( repositoryConfig.getLocation() );
        Path artifactFile = basePath.resolve(
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ManagedRepository repo = repositoryRegistry.getManagedRepository(repositoryConfig.getId());
        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      repo.getIndexingContext());

        indexingExecutor.executeTask( task );

        Map<String, IndexingContext> ctxs = indexer.getIndexingContexts();
        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "org.apache.archiva" ) ),
               BooleanClause.Occur.SHOULD );
        q.add(
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( "archiva-index-methods-jar-test" ) ),
            BooleanClause.Occur.SHOULD );

        FlatSearchRequest request = new FlatSearchRequest( q );
        FlatSearchResponse response = indexer.searchFlat( request );

        assertTrue( Files.exists(basePath.resolve( ".indexer" )) );
        assertFalse( Files.exists(basePath.resolve(".index" )) );
        assertEquals( 1, response.getTotalHits() );

        Set<ArtifactInfo> results = response.getResults();

        ArtifactInfo artifactInfo = results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.getGroupId() );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.getArtifactId() );
        assertEquals( "test-repo", artifactInfo.getRepository() );

    }

    @Test
    public void testUpdateArtifactInIndex()
        throws Exception
    {
        Path basePath = PathUtil.getPathFromUri( repositoryConfig.getLocation( ) );
        Path artifactFile = basePath.resolve(
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ManagedRepository repo = repositoryRegistry.getManagedRepository(repositoryConfig.getId());
        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      repo.getIndexingContext() );

        indexingExecutor.executeTask( task );
        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "org.apache.archiva" ) ),
               BooleanClause.Occur.SHOULD );
        q.add(
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( "archiva-index-methods-jar-test" ) ),
            BooleanClause.Occur.SHOULD );

        IndexingContext ctx = getIndexingContext();

        IndexSearcher searcher = ctx.acquireIndexSearcher();
        TopDocs topDocs = searcher.search( q, null, 10 );

        //searcher.close();
        ctx.releaseIndexSearcher( searcher );

        assertTrue( Files.exists(basePath.resolve(".indexer" )) );
        assertFalse( Files.exists(basePath.resolve(".index" )) );

        // should only return 1 hit!
        assertEquals( 1, topDocs.totalHits );
    }

    @Test
    public void testRemoveArtifactFromIndex()
        throws Exception
    {
        Path basePath = PathUtil.getPathFromUri( repositoryConfig.getLocation( ) );
        Path artifactFile = basePath.resolve(
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );

        ManagedRepository repo = repositoryRegistry.getManagedRepository(repositoryConfig.getId());
        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      repo.getIndexingContext() );

        // add artifact to index
        indexingExecutor.executeTask( task );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.apache.archiva" ) ),
               BooleanClause.Occur.SHOULD );
        //q.add(
        //    indexer.constructQuery( MAVEN.ARTIFACT_ID, new SourcedSearchExpression( "archiva-index-methods-jar-test" ) ),
        //    Occur.SHOULD );

        FlatSearchRequest flatSearchRequest =
            new FlatSearchRequest( q, indexer.getIndexingContexts().get( repositoryConfig.getId() ) );

        FlatSearchResponse response = indexer.searchFlat( flatSearchRequest );

        assertTrue( Files.exists(basePath.resolve(".indexer" )) );
        assertFalse( Files.exists(basePath.resolve( ".index" )) );

        // should return 1 hit
        assertEquals( 1, response.getTotalHitsCount() );

        // remove added artifact from index
        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.DELETE,
                        repo.getIndexingContext());
        indexingExecutor.executeTask( task );

        task = new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.FINISH,
                                         repo.getIndexingContext() );
        indexingExecutor.executeTask( task );

        q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new SourcedSearchExpression( "org.apache.archiva" ) ),
               BooleanClause.Occur.SHOULD );
        q.add( indexer.constructQuery( MAVEN.ARTIFACT_ID,
                                       new SourcedSearchExpression( "archiva-index-methods-jar-test" ) ),
               BooleanClause.Occur.SHOULD );

        assertTrue( Files.exists(basePath.resolve( ".indexer" )) );
        assertFalse( Files.exists(basePath.resolve(".index" )) );

        flatSearchRequest = new FlatSearchRequest( q, getIndexingContext() );

        response = indexer.searchFlat( flatSearchRequest );
        // artifact should have been removed from the index!
        assertEquals( 0, response.getTotalHitsCount() );//.totalHits );

        // TODO: test it was removed from the packaged index also
    }

    @Test
    public void testPackagedIndex()
        throws Exception
    {

        Path basePath = PathUtil.getPathFromUri( repositoryConfig.getLocation());
        Path indexerDirectory =basePath.resolve( ".indexer" );

        Files.list(indexerDirectory).filter( path -> path.getFileName().toString().startsWith("nexus-maven-repository-index") )
            .forEach( path ->
            {
                try
                {
                    Files.delete( path );
                }
                catch ( IOException e )
                {
                    e.printStackTrace( );
                }
            } );


        Path artifactFile = basePath.resolve(
                                      "org/apache/archiva/archiva-index-methods-jar-test/1.0/archiva-index-methods-jar-test-1.0.jar" );
        ManagedRepository repo = repositoryRegistry.getManagedRepository(repositoryConfig.getId());
        ArtifactIndexingTask task =
            new ArtifactIndexingTask( repositoryConfig, artifactFile, ArtifactIndexingTask.Action.ADD,
                                      repo.getIndexingContext() );
        task.setExecuteOnEntireRepo( false );

        indexingExecutor.executeTask( task );

        task = new ArtifactIndexingTask( repositoryConfig, null, ArtifactIndexingTask.Action.FINISH,
                                         repo.getIndexingContext() );

        task.setExecuteOnEntireRepo( false );

        indexingExecutor.executeTask( task );

        assertTrue( Files.exists(indexerDirectory) );

        // test packed index file creation
        //no more zip
        //Assertions.assertThat(new File( indexerDirectory, "nexus-maven-repository-index.zip" )).exists();
        Assertions.assertThat( Files.exists(indexerDirectory.resolve("nexus-maven-repository-index.properties" ) ));
        Assertions.assertThat( Files.exists(indexerDirectory.resolve("nexus-maven-repository-index.gz" ) ));

        // unpack .zip index
        Path destDir = basePath.resolve( ".indexer/tmp" );
        //unzipIndex( indexerDirectory.getPath(), destDir.getPath() );

        DefaultIndexUpdater.FileFetcher fetcher = new DefaultIndexUpdater.FileFetcher( indexerDirectory.toFile() );
        IndexUpdateRequest updateRequest = new IndexUpdateRequest( getIndexingContext(), fetcher );
        //updateRequest.setLocalIndexCacheDir( indexerDirectory );
        indexUpdater.fetchAndUpdateIndex( updateRequest );

        BooleanQuery q = new BooleanQuery();
        q.add( indexer.constructQuery( MAVEN.GROUP_ID, new StringSearchExpression( "org.apache.archiva" ) ),
               BooleanClause.Occur.SHOULD );
        q.add(
            indexer.constructQuery( MAVEN.ARTIFACT_ID, new StringSearchExpression( "archiva-index-methods-jar-test" ) ),
            BooleanClause.Occur.SHOULD );

        FlatSearchRequest request = new FlatSearchRequest( q, getIndexingContext() );
        FlatSearchResponse response = indexer.searchFlat( request );

        Set<ArtifactInfo> results = response.getResults();

        ArtifactInfo artifactInfo = results.iterator().next();
        assertEquals( "org.apache.archiva", artifactInfo.getGroupId() );
        assertEquals( "archiva-index-methods-jar-test", artifactInfo.getArtifactId() );
        assertEquals( "test-repo", artifactInfo.getRepository() );

        assertEquals( 1, response.getTotalHits() );
    }

}

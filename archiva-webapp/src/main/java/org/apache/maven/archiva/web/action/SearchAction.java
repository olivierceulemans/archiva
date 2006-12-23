package org.apache.maven.archiva.web.action;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.TermQuery;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfigurationStoreException;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.RepositoryArtifactIndexFactory;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.indexer.RepositoryIndexSearchException;
import org.apache.maven.archiva.indexer.lucene.LuceneQuery;
import org.apache.maven.archiva.indexer.lucene.LuceneRepositoryArtifactIndex;
import org.apache.maven.archiva.indexer.record.StandardIndexRecordFields;
import org.apache.maven.archiva.web.util.VersionMerger;
import org.codehaus.plexus.xwork.action.PlexusActionSupport;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;

/**
 * Search all indexed fields by the given criteria.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="searchAction"
 */
public class SearchAction
    extends PlexusActionSupport
{
    /**
     * Query string.
     */
    private String q;

    /**
     * The MD5 to search by.
     */
    private String md5;

    /**
     * Search results.
     */
    private Collection searchResults;

    /**
     * @plexus.requirement
     */
    private RepositoryArtifactIndexFactory factory;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory repositoryFactory;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    private static final String RESULTS = "results";

    private static final String ARTIFACT = "artifact";

    private String infoMessage;

    public String quickSearch()
        throws MalformedURLException, RepositoryIndexException, RepositoryIndexSearchException,
        ConfigurationStoreException, ParseException
    {
        // TODO: give action message if indexing is in progress

        assert q != null && q.length() != 0;

        RepositoryArtifactIndex index = getIndex();

        if ( !index.exists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        MultiFieldQueryParser parser = new MultiFieldQueryParser( new String[]{StandardIndexRecordFields.GROUPID,
            StandardIndexRecordFields.ARTIFACTID, StandardIndexRecordFields.BASE_VERSION,
            StandardIndexRecordFields.CLASSIFIER, StandardIndexRecordFields.CLASSES, StandardIndexRecordFields.FILES,
            StandardIndexRecordFields.TYPE, StandardIndexRecordFields.PROJECT_NAME,
            StandardIndexRecordFields.PROJECT_DESCRIPTION}, LuceneRepositoryArtifactIndex.getAnalyzer() );
        searchResults = index.search( new LuceneQuery( parser.parse( q ) ) );

        if ( searchResults.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }

        searchResults = VersionMerger.merge( searchResults );

        return SUCCESS;
    }

    public String findArtifact()
        throws Exception
    {
        // TODO: give action message if indexing is in progress

        assert md5 != null && md5.length() != 0;

        RepositoryArtifactIndex index = getIndex();

        if ( !index.exists() )
        {
            addActionError( "The repository is not yet indexed. Please wait, and then try again." );
            return ERROR;
        }

        searchResults = index.search(
            new LuceneQuery( new TermQuery( new Term( StandardIndexRecordFields.MD5, md5.toLowerCase() ) ) ) );

        if ( searchResults.isEmpty() )
        {
            addActionError( "No results found" );
            return INPUT;
        }
        if ( searchResults.size() == 1 )
        {
            return ARTIFACT;
        }
        else
        {
            return RESULTS;
        }
    }

    private RepositoryArtifactIndex getIndex()
        throws ConfigurationStoreException, RepositoryIndexException
    {
        Configuration configuration = configurationStore.getConfigurationFromStore();
        File indexPath = new File( configuration.getIndexPath() );

        return factory.createStandardIndex( indexPath );
    }

    public String doInput()
    {
        return INPUT;
    }

    public String getQ()
    {
        return q;
    }

    public void setQ( String q )
    {
        this.q = q;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5( String md5 )
    {
        this.md5 = md5;
    }

    public Collection getSearchResults()
    {
        return searchResults;
    }

    public String getInfoMessage()
    {
        return infoMessage;
    }

    public void setInfoMessage( String infoMessage )
    {
        this.infoMessage = infoMessage;
    }
}

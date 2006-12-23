package org.apache.maven.archiva.reporting.filter;

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

import org.apache.maven.archiva.discoverer.filter.MetadataFilter;
import org.apache.maven.archiva.reporting.database.ReportingDatabase;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;

/**
 * Implementation of a reporting filter. Artifacts already in the database are ignored.
 */
public class ReportingMetadataFilter
    implements MetadataFilter
{
    private final ReportingDatabase reporter;

    public ReportingMetadataFilter( ReportingDatabase reporter )
    {
        this.reporter = reporter;
    }

    public boolean include( RepositoryMetadata metadata, long timestamp )
    {
        return !reporter.isMetadataUpToDate( metadata, timestamp );
    }
}

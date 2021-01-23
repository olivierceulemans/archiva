package org.apache.archiva.rest.api.model.v2.repository;/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;

/**
 * @author Martin Stockhammer <martin_s@apache.org>
 */
@Schema(name="RemoteIndexInfo",description = "Information about remote indexes")
public class RemoteIndexInfo
{
    private boolean downloadRemoteIndex = false;
    private String indexUri;

    @Schema(name="download_remote_index",description = "True, if the index will be downloaded from the remote repository")
    public boolean isDownloadRemoteIndex( )
    {
        return downloadRemoteIndex;
    }

    public void setDownloadRemoteIndex( boolean downloadRemoteIndex )
    {
        this.downloadRemoteIndex = downloadRemoteIndex;
    }

    @Schema(name="index_uri", description = "The URI that specifies the path to the remote index")
    public String getIndexUri( )
    {
        return indexUri;
    }

    public void setIndexUri( String indexUri )
    {
        this.indexUri = indexUri;
    }
}
